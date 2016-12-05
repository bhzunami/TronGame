package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net"
	"os"
	"sync"

	"github.com/satori/go.uuid"
)

var players = make(map[uuid.UUID]string)
var playerLock = new(sync.RWMutex)

const (
	UDP_PORT    = `7604`
	UDP_BUF_CAP = 1024
	TCP_PORT    = `7606`
	TCP_BUF_CAP = 1024
)

func exitOnError(e error) {
	if e != nil {
		log.Println(e)
		os.Exit(1)
	}
}

func listenUDP() {
	socket, e := net.ResolveUDPAddr(`udp`, ":"+UDP_PORT)
	exitOnError(e)

	conn, e := net.ListenUDP(`udp`, socket)
	exitOnError(e)

	defer conn.Close()

	buffer := make([]byte, UDP_BUF_CAP, UDP_BUF_CAP)

	for {
		n, remAddr, e := conn.ReadFromUDP(buffer)
		if e != nil {
			log.Println(remAddr, e)
			continue
		}
		payload := buffer[:n]
		log.Println("received", string(payload), "from", remAddr)
	}
}

func listenTCP() {
	l, e := net.Listen(`tcp`, ":"+TCP_PORT)
	if e != nil {
		fmt.Println("Error listening:", e.Error())
		os.Exit(1)
	}
	// Close the listener when the application closes.
	defer l.Close()
	fmt.Println("Listening on " + ":" + TCP_PORT)
	buffer := make([]byte, TCP_BUF_CAP, TCP_BUF_CAP)
	for {
		conn, e := l.Accept()
		if e != nil {
			log.Println("Error accepting: ", e)
			continue
		}
		n, e := conn.Read(buffer)
		if e != nil {
			log.Println("Error reading: ", e)
			continue
		}
		payload := buffer[:n]

		id := uuid.NewV4()
		player := struct {
			Name string `json:"name"`
			Id   string `json:"id"`
		}{}

		json.Unmarshal(payload, &player)

		log.Println("received", string(payload), "from", conn.RemoteAddr(), "length: ", len(payload), "id: ", id.String())

		playerLock.Lock()
		players[id] = player.Name
		playerLock.Unlock()

		player.Id = id.String()

		json.NewEncoder(conn).Encode(player)

	}
}

func main() {
	go listenTCP()
	go listenUDP()
	select {}

}
