package main

import (
	"fmt"
	"os"
	"os/signal"
	"syscall"

	vpncore "libstunnel/engine"
)

type platform struct{}

func (platform) Log(message string) {
	fmt.Println(message)
}

func (platform) Protect(_ int64) bool {
	return true
}

func main() {
	if len(os.Args) != 2 {
		fmt.Fprintln(os.Stderr, "usage: vpncore <engine-config.json>")
		os.Exit(2)
	}
	raw, err := os.ReadFile(os.Args[1])
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
	manager := vpncore.NewManager(platform{})
	if err := manager.Start(string(raw)); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
	defer manager.Stop()

	stop := make(chan os.Signal, 1)
	signal.Notify(stop, os.Interrupt, syscall.SIGTERM)
	<-stop
}
