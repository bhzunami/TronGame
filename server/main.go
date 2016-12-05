package main

import (
	"encoding/json"
	"fmt"
	"github.com/satori/go.uuid"
	"log"
	"net"
	"os"
	"strconv"
	"sync"
	"time"
	"unsafe"
)

const (
	TCP_PORT    = `7606`
	TCP_BUF_CAP = 1024
	TIME_PERIOD = 8 * time.Millisecond
)

var (
	TcpByteBufferPool = &sync.Pool{
		New: func() interface{} {
			return make([]byte, TCP_BUF_CAP, TCP_BUF_CAP)
		},
	}
)

type PlayerState struct {
	Connection *net.UDPConn
	Position   [3]float32
	Direction  [3]float32
}

var Players = struct {
	Lock  *sync.RWMutex
	Store map[uuid.UUID]PlayerState
}{
	Lock:  new(sync.RWMutex),
	Store: make(map[uuid.UUID]PlayerState),
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

type TcpRequest struct {
	Action  string `json:"action"`
	Payload JSON   `json:"payload"`
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

func dispatch(action string, payload JSON) (interface{}, *ErrorResponse) {
	switch action {
	case "join":
		joinReq := JoinRequest{}
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

func serveUDP() {

	for {
		time.Sleep(TIME_PERIOD)

		Players.Lock.RLock()
		for id, state := range Players.Store {
			posBytes := *((*[12]byte)(unsafe.Pointer(&state.Position[0])))
			dirBytes := *((*[12]byte)(unsafe.Pointer(&state.Direction[0])))
			_ = id
			state.Connection.Write(posBytes[:])
			state.Connection.Write(dirBytes[:])
		}
		Players.Lock.RUnlock()
	}
}

func serveTCP() {

	socket, e := net.Listen(`tcp`, ":"+TCP_PORT)
	if e != nil {
		exitOnError(fmt.Errorf("Error listening on TCP port %s: %s", TCP_PORT, e.Error()))
	}
	defer socket.Close()
	log.Println("Serving TCP port", TCP_PORT)

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
			json.NewEncoder(conn).Encode(GeneralResponse{Error: err})
			conn.Close()
			continue
		}

		go func() {
			res, err := dispatch(action, payload)
			if err != nil {
				json.NewEncoder(conn).Encode(GeneralResponse{Error: err})
			} else {
				json.NewEncoder(conn).Encode(GeneralResponse{Response: res})
			}
			conn.Close()
		}()

	}
}

type JoinRequest struct {
	Name string `json:"name"`
	Host string `json:"host"` // for UDP
	Port int    `json:"port"` // for UDP
}
type JoinResponse struct {
	Id string `json:"id"`
}

func HandleJoin(req JoinRequest) (JoinResponse, *ErrorResponse) {

	res := JoinResponse{}
	dialString := req.Host + ":" + strconv.Itoa(req.Port)

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
	Players.Store[id] = PlayerState{
		Connection: conn,
	}
	Players.Lock.Unlock()

	res.Id = id.String()
	return res, nil
}

func main() {
	go serveUDP()
	serveTCP()
}
