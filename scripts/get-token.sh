#!/usr/bin/env bash
set -euo pipefail

# Load local credentials
set -a
source .env.keycloak
set +a

TOKEN_URL="${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token"

RESPONSE=$(curl --fail --silent --show-error \
  --request POST "$TOKEN_URL" \
  --data-urlencode "grant_type=client_credentials" \
  --data-urlencode "client_id=${KEYCLOAK_CLIENT_ID}" \
  --data-urlencode "client_secret=${KEYCLOAK_CLIENT_SECRET}")

ACCESS_TOKEN=$(echo "$RESPONSE" | jq -er '.access_token')

printf '%s\n' "$ACCESS_TOKEN"