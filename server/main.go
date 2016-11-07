package main

import (
	"log"
	"net"
	"os"
)

const (
	PORT        = `7604`
	UDP_BUF_CAP = 1024
	PROTOCOL    = `udp` // might be udp4 for IP4 or udp6
)

func exitOnError(e error) {
	if e != nil {
		log.Println(e)
		os.Exit(1)
	}
}

func main() {

	socket, e := net.ResolveUDPAddr(PROTOCOL, ":"+PORT)
	exitOnError(e)

	conn, e := net.ListenUDP(PROTOCOL, socket)
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