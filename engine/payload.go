package vpncore

import (
	"fmt"
	"regexp"
	"strconv"
	"strings"
	"time"
)

type payloadVariables struct {
	Host      string
	Port      int
	Method    string
	Protocol  string
	UserAgent string
	RealRaw   string
}

type payloadChunk struct {
	data        []byte
	delayBefore time.Duration
}

var splitPattern = regexp.MustCompile(`(?i)\[split(?:=(\d+))?\]`)

func expandPayload(payload string, vars payloadVariables) ([]payloadChunk, error) {
	indexes := splitPattern.FindAllStringSubmatchIndex(payload, -1)
	chunks := make([]payloadChunk, 0, len(indexes)+1)
	start := 0
	delay := time.Duration(0)

	for _, index := range indexes {
		chunks = appendExpandedChunk(chunks, payload[start:index[0]], delay, vars)
		delay = 0
		if index[2] >= 0 {
			millis, err := strconv.Atoi(payload[index[2]:index[3]])
			if err != nil || millis < 0 || millis > 60000 {
				return nil, fmt.Errorf("invalid split delay")
			}
			delay = time.Duration(millis) * time.Millisecond
		}
		start = index[1]
	}
	chunks = appendExpandedChunk(chunks, payload[start:], delay, vars)
	return chunks, nil
}

func appendExpandedChunk(chunks []payloadChunk, value string, delay time.Duration, vars payloadVariables) []payloadChunk {
	replacer := strings.NewReplacer(
		"[method]", vars.Method,
		"[host_port]", fmt.Sprintf("%s:%d", vars.Host, vars.Port),
		"[host]", vars.Host,
		"[port]", strconv.Itoa(vars.Port),
		"[protocol]", vars.Protocol,
		"[ua]", vars.UserAgent,
		"[real_raw]", vars.RealRaw,
		"[crlf]", "\r\n",
		"[lfcr]", "\n\r",
		"[cr]", "\r",
		"[lf]", "\n",
	)
	data := []byte(replacer.Replace(value))
	if len(data) == 0 && len(chunks) > 0 {
		return chunks
	}
	return append(chunks, payloadChunk{data: data, delayBefore: delay})
}
