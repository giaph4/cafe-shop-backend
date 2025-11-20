#!/usr/bin/env bash
# Script kiểm tra nhanh các endpoint chính của Shift Platform.
# Yêu cầu: đã export TOKEN=<JWT> và BASE_URL=https://api.example.com

set -euo pipefail

: "${TOKEN:?Need to set TOKEN environment variable}"
BASE_URL="${BASE_URL:-https://api.example.com}"

function title() {
  echo "\n==== $1 ===="
}

title "Login"
curl -sS -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"manager01","password":"Secret@123"}' | jq

title "Shift report by session"
curl -sS "$BASE_URL/api/v1/shifts/reports/sessions/300?refresh=false" \
  -H "Authorization: Bearer $TOKEN" | jq

title "List conversations"
curl -sS "$BASE_URL/api/chat/conversations?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" | jq

title "Send text message"
curl -sS -X POST "$BASE_URL/api/chat/conversations/41/messages/text" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "content=Xin%20ch%C3%A0o%20team" | jq

title "Mark message seen"
curl -sS -X POST "$BASE_URL/api/chat/conversations/41/messages/8005/seen" \
  -H "Authorization: Bearer $TOKEN" \
  -w '\nStatus: %{http_code}\n'
