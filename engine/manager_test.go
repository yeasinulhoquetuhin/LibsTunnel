package vpncore

import "testing"

func TestManagerStartsMinimalXray(t *testing.T) {
	manager := NewManager(nil)
	err := manager.Start(`{
		"injection":{"enabled":false},
		"xray":{
			"log":{"loglevel":"none"},
			"outbounds":[{"tag":"direct","protocol":"freedom","settings":{}}]
		}
	}`)
	if err != nil {
		t.Fatal(err)
	}
	manager.Stop()
}

func TestSSHConfigValidation(t *testing.T) {
	_, err := parseConfig(`{
		"backend":"ssh",
		"ssh":{"host":"ssh.example","port":22,"username":"user","password":"secret"},
		"injection":{"enabled":false},
		"tun":{"mtu":1500}
	}`)
	if err != nil {
		t.Fatal(err)
	}
}
