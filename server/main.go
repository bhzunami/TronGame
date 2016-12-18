package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net"
	"os"
	"strconv"
	"sync"
	"time"
	"unsafe"

	"github.com/satori/go.uuid"
)

const (
	TCP_PORT       = 7606
	UDP_PORT       = 7607
	TCP_BUF_CAP    = 1024
	UDP_BUF_CAP    = 1024
	TIME_PERIOD    = 17 * time.Millisecond // ~60fps
	PLAYER_TIMEOUT = 10 * time.Second
)

var (
	TcpByteBufferPool = &sync.Pool{
		New: func() interface{} {
			return make([]byte, TCP_BUF_CAP, TCP_BUF_CAP)
		},
	}
	UdpByteBufferPool = &sync.Pool{
		New: func() interface{} {
			return make([]byte, UDP_BUF_CAP, UDP_BUF_CAP)
		},
	}
)

type PlayerState struct {
	Connection *net.UDPConn
	Position   [3]float32
	Direction  [3]float32
	LastPing   time.Time
}

var Players = struct {
	Lock  *sync.RWMutex
	Store map[uuid.UUID]*PlayerState
}{
	Lock:  new(sync.RWMutex),
	Store: make(map[uuid.UUID]*PlayerState),
}

type JSON []byte

func (j JSON) MarshalJSON() ([]byte, error) {
	return j, nil
}
func (j *JSON) UnmarshalJSON(data []byte) error {
	*j = append((*j)[:0], data...)
	return nil
}

func exitOnError(e error) {
	if e != nil {
		log.Println(e)
		os.Exit(1)
	}
}

type GeneralResponse struct {
	Error    *ErrorResponse `json:"error,omitempty"`
	Response interface{}    `json:"response,omitempty"`
}

type ErrorResponse struct {
	Type    string      `json:"type"`
	Message string      `json:"message"`
	Payload interface{} `json:"payload"`
}

func gcPlayers() {

	for {
		time.Sleep(PLAYER_TIMEOUT)
		now := time.Now()
		Players.Lock.Lock()
		for id, state := range Players.Store {
			if state.LastPing.Before(now.Add(-PLAYER_TIMEOUT)) {
				log.Println("Player GC:", state)
				delete(Players.Store, id)
			}
		}
		Players.Lock.Unlock()
	}
}

func updatePlayerPositions() {

	bf := make([]byte, 24, 24)

	for {
		time.Sleep(TIME_PERIOD)

		Players.Lock.RLock()
		for id, state := range Players.Store {

			posBytes := *((*[12]byte)(unsafe.Pointer(&state.Position[0])))
			dirBytes := *((*[12]byte)(unsafe.Pointer(&state.Direction[0])))

			for i := 0; i < 12; i++ {
				bf[i] = posBytes[i]
				bf[i+12] = dirBytes[i]
			}

			state.Connection.Write(bf[:])
			fmt.Println("sending player position", id.String(), state.Position)

			if state.Direction[0] > 0 {
				state.Direction[0] -= 0.01
			}
			if state.Direction[1] > 0 {
				state.Direction[1] -= 0.01
			}
			if state.Direction[0] < 0 {
				state.Direction[0] += 0.01
			}
			if state.Direction[1] < 0 {
				state.Direction[1] += 0.01
			}

		}
		Players.Lock.RUnlock()
	}
}

const (
	KeyForward  byte = 0x01
	KeyBackward byte = 0x02
	KeyLeft     byte = 0x04
	KeyRight    byte = 0x08
	KeySpace    byte = 0x10
)

type PlayerUpdate struct {
	UUID string `json:"uuid"`
	Keys byte   `json:"keys"`
}

func readPlayerKeys() {

	conn, e := net.ListenUDP(`udp`, &net.UDPAddr{Port: UDP_PORT})
	if e != nil {
		exitOnError(fmt.Errorf("Error listening on UDP port %s: %s", UDP_PORT, e.Error()))
	}
	log.Println("Listening on UDP port", UDP_PORT)

	bf := make([]byte, 1024, 1024)
	for {
		n, remote, e := conn.ReadFromUDP(bf)
		if e != nil {
			log.Println("Error reading from UDP:", e)
			continue
		}
		_ = remote
		bs := bf[:n]
		pu := PlayerUpdate{}
		if e := json.Unmarshal(bs, &pu); e != nil {
			log.Println("Error decoding JSON from UDP:", e)
			continue
		}
		km := pu.Keys
		id, e := uuid.FromString(pu.UUID)
		if e != nil {
			log.Println("Error decoding UUID from UDP:", e)
			continue
		}
		if km == 0 {
			continue
		}
		Players.Lock.Lock()
		ps, ok := Players.Store[id]
		if !ok {
			log.Println("No player with UUID:", id)
			continue
		}
		ps.LastPing = time.Now()
		if km&KeyForward == KeyForward {
			ps.Position[0] += 1
		}
		if km&KeyBackward == KeyBackward {
			ps.Position[0] -= 1
		}
		if km&KeyRight == KeyRight {
			ps.Position[1] += 1
			if ps.Direction[0] < 1 {
				ps.Direction[0] += 0.1
			}
			if ps.Direction[1] < 1 {
				ps.Direction[1] += 0.1
			}
		} else {

		}
		if km&KeyLeft == KeyLeft {
			ps.Position[1] -= 1
			if ps.Direction[0] > -1 {
				ps.Direction[0] -= 0.1
			}
			if ps.Direction[1] > -1 {
				ps.Direction[1] -= 0.1
			}
		} else {

		}
		Players.Lock.Unlock()
	}
}

func serveTCP() {

	socket, e := net.ListenTCP(`tcp`, &net.TCPAddr{Port: TCP_PORT})
	if e != nil {
		exitOnError(fmt.Errorf("Error listening on TCP port %s: %s", TCP_PORT, e.Error()))
	}
	defer socket.Close()
	log.Println("Listening on TCP port", TCP_PORT)

	for {

		conn, e := socket.Accept()
		if e != nil {
			log.Println("Error accepting: ", e)
			if conn != nil {
				conn.Close()
			}
			continue
		}

		action, payload, err := decodeRequest(conn)
		if err != nil {
			if e := json.NewEncoder(conn).Encode(GeneralResponse{Error: err}); e != nil {
				log.Println("Failed encoding JSON:", e)
			}
			conn.Close()
			continue
		}

		go func() {
			res, err := dispatch(action, payload, conn)
			if err != nil {
				if e := json.NewEncoder(conn).Encode(GeneralResponse{Error: err}); e != nil {
					log.Println("Failed encoding JSON:", e)
				}
			} else {
				if e := json.NewEncoder(conn).Encode(GeneralResponse{Response: res}); e != nil {
					log.Println("Failed encoding JSON:", e)
				}
			}
			conn.Close()
		}()

	}
}

type JoinRequest struct {
	Conn net.Conn `json:"-"`
	Name string   `json:"name"`
	Port int      `json:"port"` // for UDP
}
type JoinResponse struct {
	Id string `json:"id"`
}

type TcpRequest struct {
	Action  string `json:"action"`
	Payload JSON   `json:"payload"`
}

func HandleJoin(req JoinRequest) (JoinResponse, *ErrorResponse) {

	host := ""

	if ra, ok := req.Conn.RemoteAddr().(*net.TCPAddr); ok {
		host = ra.IP.String()
	}

	res := JoinResponse{}

	if host == "" {
		return res, &ErrorResponse{
			Type:    "network",
			Message: "could not resolve remote address",
			Payload: "",
		}
	}

	dialString := host + ":" + strconv.Itoa(req.Port)

	addr, e := net.ResolveUDPAddr(`udp`, dialString)
	if e != nil {
		return res, &ErrorResponse{
			Type:    "network",
			Message: "could not resolve address",
			Payload: dialString,
		}
	}

	conn, e := net.DialUDP(`udp`, nil, addr)
	if e != nil {
		return res, &ErrorResponse{
			Type:    "network",
			Message: "could not connect to UDP client",
			Payload: dialString,
		}
	}
	id := uuid.NewV4()

	Players.Lock.Lock()
	Players.Store[id] = &PlayerState{
		Connection: conn,
		Position:   [3]float32{0, 0, 0},
		Direction:  [3]float32{1, 0, 0},
	}
	Players.Lock.Unlock()

	res.Id = id.String()
	return res, nil
}

func decodeRequest(conn net.Conn) (string, JSON, *ErrorResponse) {

	buffer := TcpByteBufferPool.Get().([]byte)
	defer TcpByteBufferPool.Put(buffer)

	n, e := conn.Read(buffer)
	if e != nil {
		return "", nil, &ErrorResponse{
			Type:    "network",
			Message: "failed reading from socket",
			Payload: nil,
		}
	}

	payload := buffer[:n]
	request := TcpRequest{}
	if e := json.Unmarshal(payload, &request); e != nil {
		return "", nil, &ErrorResponse{
			Type:    "request",
			Message: "failed decoding JSON",
			Payload: string(payload),
		}

	}

	return request.Action, request.Payload, nil
}

func dispatch(action string, payload JSON, conn net.Conn) (interface{}, *ErrorResponse) {
	switch action {
	case "join":
		joinReq := JoinRequest{Conn: conn}
		if e := json.Unmarshal(payload, &joinReq); e != nil {
			return nil, &ErrorResponse{
				Type:    "request",
				Message: "failed decoding JSON join request",
				Payload: string(payload),
			}
		}
		return HandleJoin(joinReq)
	}
	return nil, &ErrorResponse{
		Type:    "request",
		Message: "no such action",
		Payload: action,
	}
}

func main() {
	go gcPlayers()
	go updatePlayerPositions()
	go readPlayerKeys()
	serveTCP()
}
