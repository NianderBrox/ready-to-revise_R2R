#!/usr/bin/env bash
#
# Starts the Postgres container and applies migrations.
# Usage: ./scripts/dev-db-up.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if ! command -v docker >/dev/null 2>&1; then
    cat <<'EOF'
Docker is not installed on this machine.

Install it first (Ubuntu):
  sudo apt-get update
  sudo apt-get install -y ca-certificates curl
  sudo install -m 0755 -d /etc/apt/keyrings
  sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  sudo chmod a+r /etc/apt/keyrings/docker.asc
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
    sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
  sudo apt-get update
  sudo apt-get install -y docker-ce docker-ce-cli containerd.io \
      docker-buildx-plugin docker-compose-plugin
  sudo usermod -aG docker "$USER"   # log out/in afterwards
EOF
    exit 1
fi

echo "== starting postgres (host port 5433) =="
docker compose up -d postgres

echo "== waiting for healthy =="
for _ in $(seq 1 30); do
    HEALTH="$(docker inspect --format '{{.State.Health.Status}}' r2r-postgres 2>/dev/null || echo starting)"
    [ "$HEALTH" = "healthy" ] && break
    sleep 2
done

if [ "${HEALTH:-none}" != "healthy" ]; then
    echo "FAIL: r2r-postgres did not become healthy"
    docker logs r2r-postgres --tail 20 || true
    exit 1
fi

echo "== applying migrations =="
(
    cd backend
    command -v npx >/dev/null 2>&1 || { echo "node/npm not found"; exit 1; }
    [ -d node_modules ] || npm ci
    npx prisma generate
    npx prisma migrate deploy
)

echo ""
echo "== READY =="
echo "postgres : localhost:5433 (r2r_user/r2r_password/r2r_db)"
echo "backend  : cd backend && cp .env.example .env  # fill secrets, then npm run start:dev"
