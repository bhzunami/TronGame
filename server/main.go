package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/satori/go.uuid"
	"log"
	"math"
	"math/rand"
	"net"
	"os"
	"strconv"
	"sync"
	"time"
	"unsafe"
)

const PowerUpVelocityMultiplier = 1.5

var (
	Blocks      = [100][2]float32{}
	PowerUps    = [20][2]float32{}
	BlockSize   = [3]float32{5, 8}
	PowerUpSize = [3]float32{10, 10}
	DamageSize  = [3]float32{2, 2}
)

func init() {

	rand.Seed(time.Now().UnixNano())

	for i, _ := range Blocks {
		Blocks[i] = [2]float32{
			(2000.0 * rand.Float32()) - 1000.0,
			(2000.0 * rand.Float32()) - 1000.0,
		}
	}

	for i, _ := range PowerUps {
		PowerUps[i] = [2]float32{
			(2000.0 * rand.Float32()) - 1000.0,
			(2000.0 * rand.Float32()) - 1000.0,
		}
	}

}

const (
	DegreeInRadians = (2 * math.Pi) / 360 // 0.0174532925199432954743716805978692718781530857086181640625
)

const (
	TCP_PORT       = 7606
	UDP_PORT       = 7607
	TCP_BUF_CAP    = 1024
	UDP_BUF_CAP    = 1024
	TIME_PERIOD    = 17 * time.Millisecond // ~60fps
	PLAYER_TIMEOUT = 100 * time.Second
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

type DamageBuffer struct {
	Coordinates [600][2]float32
	Iterator    int
}

type PlayerState struct {
	Connection *net.UDPConn
	Position   [3]float32
	Rotation   [3]float32
	LastPing   time.Time
	Velocity   float32
	Damage     *DamageBuffer
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

func writePlayerSegment(bf *bytes.Buffer, id uuid.UUID, state *PlayerState) {

	posBytes := *((*[12]byte)(unsafe.Pointer(&state.Position[0])))
	rotBytes := *((*[12]byte)(unsafe.Pointer(&state.Rotation[0])))

	bf.Write(id.Bytes())
	bf.Write(posBytes[:])
	bf.Write(rotBytes[:])

}

func updatePlayerPositions() {

	bf := bytes.NewBuffer(nil)

	for {

		time.Sleep(TIME_PERIOD)

		Players.Lock.RLock()
		amountOfPlayers := byte(len(Players.Store))

		for id, state := range Players.Store {

			bf.WriteByte(amountOfPlayers)
			writePlayerSegment(bf, id, state)

			for otherId, otherState := range Players.Store {
				if otherId == id {
					continue
				}
				writePlayerSegment(bf, otherId, otherState)
			}

			state.Connection.Write(bf.Bytes())
			bf.Truncate(0)

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

		Players.Lock.Lock()
		ps, ok := Players.Store[id]
		if !ok {
			// log.Println("No player with UUID:", id)
			Players.Lock.Unlock()
			continue
		}
		ps.LastPing = time.Now()

		{ // always go forward
			angle := float64(ps.Rotation[2])
			ps.Position[0] += ps.Velocity * float32(math.Cos(angle))
			ps.Position[1] += ps.Velocity * float32(math.Sin(angle))
		}

		if km&KeyBackward == KeyBackward {
			angle := float64(ps.Rotation[2])
			ps.Position[0] -= (ps.Velocity / 2) * float32(math.Cos(angle))
			ps.Position[1] -= (ps.Velocity / 2) * float32(math.Sin(angle))
		}

		if km&KeyLeft == KeyLeft {
			ps.Rotation[0] -= 1 * DegreeInRadians
			ps.Rotation[2] += 1 * DegreeInRadians
		}

		if km&KeyRight == KeyRight {
			ps.Rotation[0] += 1 * DegreeInRadians
			ps.Rotation[2] -= 1 * DegreeInRadians
		}

		if ps.Rotation[0] > 360*DegreeInRadians { // rotated 360°
			ps.Rotation[0] -= 360 * DegreeInRadians
		}

		if ps.Rotation[0] > 360*DegreeInRadians { // rotated 360°
			ps.Rotation[0] -= 360 * DegreeInRadians
		}

		if ps.Rotation[2] > 360*DegreeInRadians { // rotated 360°
			ps.Rotation[2] -= 360 * DegreeInRadians
		}

		// X rotation limited to 45° (pi/2)
		if ps.Rotation[0] > math.Pi/4 {
			ps.Rotation[0] = math.Pi / 4
		}

		if ps.Rotation[0] < -math.Pi/4 {
			ps.Rotation[0] = -math.Pi / 4
		}

		// stabilize X rotation when not in curve
		if km&KeyLeft != KeyLeft && km&KeyRight != KeyRight {
			if ps.Rotation[0] > 0 {
				ps.Rotation[0] -= 3 * DegreeInRadians
			}
			if ps.Rotation[0] < 0 {
				ps.Rotation[0] += 3 * DegreeInRadians
			}
		}

		collision, center := detectCollision(ps.Position[0], ps.Position[1])
		_ = center

		switch collision {

		case BlockCollision:
			delete(Players.Store, id)

		case WorldCollision:
			ps.Rotation[2] += (180 * DegreeInRadians)

		case PowerUpCollision:
			ps.Velocity *= PowerUpVelocityMultiplier
			go func(id uuid.UUID) {
				time.Sleep(time.Second * 2)
				Players.Lock.Lock()
				defer Players.Lock.Unlock()
				if ps, ok := Players.Store[id]; ok {
					ps.Velocity /= PowerUpVelocityMultiplier
				}
			}(id)

		default:
			x, y := ps.Position[0], ps.Position[1]
			for oid, state := range Players.Store {
				if oid == id {
					continue // own points make no damage
				}
				for _, cor := range state.Damage.Coordinates {
					low, high := [2]float32{cor[0] - DamageSize[0], cor[1] - DamageSize[0]}, [2]float32{cor[0] + DamageSize[0], cor[1] + DamageSize[0]}
					if x >= low[0] && y >= low[1] && x <= high[0] && y <= high[1] {
						log.Println(oid.String(), "killed", id.String())
						delete(Players.Store, id)
					}
				}
			}

		}

		{ // set damage point
			n := ps.Damage.Iterator
			ps.Damage.Coordinates[n] = [2]float32{ps.Position[0], ps.Position[1]}
			n++
			if n >= len(ps.Damage.Coordinates) {
				n = 0
			}
			ps.Damage.Iterator = n
		}

		Players.Lock.Unlock()
	}
}

type Collision string

const (
	NoCollision      Collision = "none"
	BlockCollision   Collision = "block"
	PowerUpCollision Collision = "powerUp"
	// PlayerCollision  Collision = "player"
	WorldCollision Collision = "world"
)

// PRECONDITION: player store is locked
func detectCollision(x, y float32) (Collision, [2]float32) {

	if x >= 1000 || x <= -1000 || y >= 1000 || y <= -1000 {
		return WorldCollision, [2]float32{x, y}
	}

	for _, block := range Blocks {
		low, high := [2]float32{block[0] - BlockSize[0], block[1] - BlockSize[0]}, [2]float32{block[0] + BlockSize[0], block[1] + BlockSize[0]}
		if x >= low[0] && y >= low[1] && x <= high[0] && y <= high[1] {
			return BlockCollision, block
		}
	}

	for _, powerUp := range PowerUps {
		low, high := [2]float32{powerUp[0] - PowerUpSize[0], powerUp[1] - PowerUpSize[0]}, [2]float32{powerUp[0] + PowerUpSize[0], powerUp[1] + PowerUpSize[0]}
		if x >= low[0] && y >= low[1] && x <= high[0] && y <= high[1] {
			return PowerUpCollision, powerUp
		}
	}

	return NoCollision, [2]float32{0, 0}
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
	Id       string          `json:"id"`
	Blocks   [100][2]float32 `json:"blocks"`
	PowerUps [20][2]float32  `json:"powerUps"`
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
		Position:   [3]float32{(2000.0 * rand.Float32()) - 1000.0, (2000.0 * rand.Float32()) - 1000.0, 5},
		Rotation:   [3]float32{0, 0, 0},
		Velocity:   3,
		Damage:     &DamageBuffer{},
	}
	Players.Lock.Unlock()

	res.Id = id.String()
	res.Blocks = Blocks
	res.PowerUps = PowerUps
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
