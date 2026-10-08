#!/usr/bin/env bash
# ==============================================================================
# Zovira E-Commerce Platform — macOS Launcher
# ==============================================================================
# Usage:
#   ./run-mac.sh           Start both Spring Boot API and Vite Web App (default)
#   ./run-mac.sh api       Start only the Spring Boot backend
#   ./run-mac.sh web       Start only the Vite frontend
#   ./run-mac.sh setup     Check toolchains, create .env, install dependencies
#   ./run-mac.sh infra     Start Docker Compose services (Postgres, Redis, MinIO, Mailpit)
#   ./run-mac.sh infra-down Stop Docker Compose services
#   ./run-mac.sh test      Run backend and frontend test suites
#   ./run-mac.sh lint      Lint and type-check frontend
#   ./run-mac.sh build     Build API jar and web production bundle
#   ./run-mac.sh clean     Clean build artifacts and temporary files
#   ./run-mac.sh --help    Show this help message
# ==============================================================================

set -e

# Resolve repository root directory
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

# ------------------------------------------------------------------ Colors ----
if [ -t 1 ]; then
  BOLD='\033[1m'
  DIM='\033[2m'
  GREEN='\033[0;32m'
  YELLOW='\033[0;33m'
  RED='\033[0;31m'
  CYAN='\033[0;36m'
  BLUE='\033[0;34m'
  RESET='\033[0m'
else
  BOLD=''; DIM=''; GREEN=''; YELLOW=''; RED=''; CYAN=''; BLUE=''; RESET=''
fi

step()  { printf "\n${BOLD}${BLUE}==>${RESET} ${BOLD}%s${RESET}\n" "$1"; }
ok()    { printf "  ${GREEN}✓${RESET} %s\n" "$1"; }
warn()  { printf "  ${YELLOW}!${RESET} %s\n" "$1"; }
info()  { printf "  ${DIM}%s${RESET}\n" "$1"; }
die()   { printf "\n${RED}✗ Error:${RESET} %s\n\n" "$1" >&2; exit 1; }

# Ports
PORT_GATEWAY="${PORT_API:-8080}"
PORT_API="$PORT_GATEWAY"
PORT_DISCOVERY="${PORT_DISCOVERY:-8761}"
PORT_USER="${PORT_USER:-8081}"
PORT_CATALOG="${PORT_CATALOG:-8082}"
PORT_CART="${PORT_CART:-8083}"
PORT_ORDER="${PORT_ORDER:-8084}"
PORT_WEB="${PORT_WEB:-5173}"
PORT_DB="${MONGO_PORT:-27017}"

DISCOVERY_PID=""
GATEWAY_PID=""
USER_PID=""
CATALOG_PID=""
CART_PID=""
ORDER_PID=""
BACKEND_PID=""
FRONTEND_PID=""

# ------------------------------------------------------------------ Banner ----
banner() {
  printf "\n"
  printf "  ${BOLD}${CYAN}┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓${RESET}\n"
  printf "  ${BOLD}${CYAN}┃${RESET}   ${BOLD}Zovira Marketplace${RESET} — Development Launcher for macOS         ${BOLD}${CYAN}┃${RESET}\n"
  printf "  ${BOLD}${CYAN}┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛${RESET}\n"
}

# ----------------------------------------------------------- Toolchain checks -
check_macos() {
  if [ "$(uname -s)" != "Darwin" ]; then
    warn "This script is tailored for macOS, but running on $(uname -s)."
  fi
}

check_node() {
  if ! command -v node >/dev/null 2>&1; then
    warn "Node.js is not installed."
    if command -v brew >/dev/null 2>&1; then
      printf '    Install Node.js with Homebrew now? [y/N] '
      read -r reply
      case "$reply" in
        [yY]*) brew install node ;;
        *) die "Node.js is required. Run: brew install node" ;;
      esac
    else
      die "Node.js (>= 22) is required. Please install it from https://nodejs.org"
    fi
  fi

  command -v npm >/dev/null 2>&1 || die "npm is missing. Reinstall Node.js."

  local major
  major="$(node -p 'process.versions.node.split(".")[0]' 2>/dev/null || echo 0)"
  if [ "$major" -lt 20 ]; then
    warn "Node $(node -v) is installed. Node 22+ is recommended."
  else
    ok "Node.js $(node -v) and npm $(npm -v) detected"
  fi
}

check_java() {
  if ! command -v java >/dev/null 2>&1; then
    warn "Java JDK is not found in PATH."
    if command -v brew >/dev/null 2>&1; then
      printf '    Install OpenJDK with Homebrew now? [y/N] '
      read -r reply
      case "$reply" in
        [yY]*) brew install openjdk ;;
        *) die "Java JDK 17+ (JDK 21 recommended) is required to run the Spring Boot API." ;;
      esac
    else
      die "Java JDK 17+ is required. Install OpenJDK from https://adoptium.net"
    fi
  fi

  local java_ver
  java_ver="$(java -version 2>&1 | head -n 1)"
  ok "Java detected: $java_ver"

  if [ ! -f "$ROOT_DIR/backend/mvnw" ]; then
    die "backend/mvnw wrapper not found!"
  fi
  chmod +x "$ROOT_DIR/backend/mvnw"
}

check_mongodb() {
  if nc -z -w 2 localhost "$PORT_DB" 2>/dev/null; then
    ok "MongoDB is reachable on port $PORT_DB"
    return 0
  fi

  warn "MongoDB is not responding on port $PORT_DB."

  # Check if Homebrew has mongodb service
  if command -v brew >/dev/null 2>&1 && brew services list 2>/dev/null | grep -E "mongodb" >/dev/null; then
    info "Attempting to start Homebrew MongoDB service..."
    brew services start mongodb/brew/mongodb-community 2>/dev/null || brew services start mongodb-community 2>/dev/null || true
    sleep 2
    if nc -z -w 2 localhost "$PORT_DB" 2>/dev/null; then
      ok "MongoDB started successfully via Homebrew"
      return 0
    fi
  fi

  # Attempt native mongod
  if [ -x "/opt/homebrew/bin/mongod" ]; then
    info "Attempting to start native mongod in background..."
    /opt/homebrew/bin/mongod --fork --logpath /tmp/mongod.log --dbpath /opt/homebrew/var/mongodb 2>/dev/null || true
    sleep 2
    if nc -z -w 2 localhost "$PORT_DB" 2>/dev/null; then
      ok "MongoDB started successfully"
      return 0
    fi
  fi

  warn "Could not auto-start MongoDB. Ensure MongoDB is running locally on port 27017."
}

# ----------------------------------------------------------------- Environment -
ensure_env() {
  local env_created=0

  if [ ! -f "$ROOT_DIR/.env" ]; then
    if [ -f "$ROOT_DIR/.env.example" ]; then
      cp "$ROOT_DIR/.env.example" "$ROOT_DIR/.env"
      # Quote unquoted MAIL_FROM containing angle brackets to prevent bash redirection issues
      if grep -q '^MAIL_FROM=Zovira <' "$ROOT_DIR/.env"; then
        /usr/bin/sed -i '' 's|^MAIL_FROM=Zovira <\(.*\)>|MAIL_FROM="Zovira <\1>"|' "$ROOT_DIR/.env" 2>/dev/null || true
      fi
      env_created=1
    else
      warn ".env.example not found; creating empty .env"
      touch "$ROOT_DIR/.env"
    fi
  fi

  # Ensure JWT_SECRET is populated with a cryptographically secure 48-byte random key
  local current_secret
  current_secret="$(grep '^JWT_SECRET=' "$ROOT_DIR/.env" 2>/dev/null | cut -d '=' -f2- || true)"
  if [ -z "$current_secret" ]; then
    local new_secret
    new_secret="$(openssl rand -base64 48 2>/dev/null | tr -d '\r\n')"
    if grep -q '^JWT_SECRET=' "$ROOT_DIR/.env"; then
      /usr/bin/sed -i '' "s|^JWT_SECRET=.*|JWT_SECRET=${new_secret}|" "$ROOT_DIR/.env"
    else
      printf "\nJWT_SECRET=%s\n" "$new_secret" >> "$ROOT_DIR/.env"
    fi
    ok "Generated fresh cryptographically secure JWT_SECRET in .env"
  fi

  if [ "$env_created" -eq 1 ]; then
    ok "Created .env from .env.example"
  else
    ok ".env configuration is present"
  fi

  # Safely load environment variables into the current shell session
  while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in
      \#*|"") continue ;;
      *=*)
        local key="${line%%=*}"
        key="$(echo "$key" | tr -d ' ')"
        local val="${line#*=}"
        case "$val" in
          \"*\"|\'*\')
            val="${val#?}"
            val="${val%?}"
            ;;
        esac
        if [[ "$key" =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ ]]; then
          export "$key"="$val"
        fi
        ;;
    esac
  done < "$ROOT_DIR/.env"
}

# ------------------------------------------------------------- Dependencies ---
ensure_dependencies() {
  if [ -d "$ROOT_DIR/frontend/node_modules" ]; then
    ok "Frontend dependencies (node_modules) are present"
  else
    info "Installing frontend dependencies with npm install..."
    ( cd "$ROOT_DIR/frontend" && npm install --no-audit --no-fund )
    ok "Frontend dependencies installed"
  fi
}

# ----------------------------------------------------------- Port Management --
check_port() {
  local port="$1"
  local desc="$2"
  local holder
  holder="$(lsof -nP -iTCP:"$port" -sTCP:LISTEN -t 2>/dev/null | head -1 || true)"
  [ -n "$holder" ] || return 0

  local name
  name="$(ps -p "$holder" -o comm= 2>/dev/null || echo "process")"
  warn "Port $port ($desc) is already in use by $name (pid $holder)."
  printf '    Stop process %s to continue? [y/N] ' "$holder"
  read -r reply
  case "$reply" in
    [yY]*)
      kill -CONT "$holder" 2>/dev/null || true
      kill "$holder" 2>/dev/null || true
      for _ in 1 2 3 4 5; do
        lsof -nP -iTCP:"$port" -sTCP:LISTEN -t >/dev/null 2>&1 || break
        sleep 1
      done
      if lsof -nP -iTCP:"$port" -sTCP:LISTEN -t >/dev/null 2>&1; then
        kill -9 "$holder" 2>/dev/null || true
        sleep 1
      fi
      if lsof -nP -iTCP:"$port" -sTCP:LISTEN -t >/dev/null 2>&1; then
        die "Could not free port $port. Please stop process $holder manually."
      else
        ok "Port $port ($desc) freed"
      fi
      ;;
    *)
      die "Port $port is busy. Stop the conflicting process or change port in .env."
      ;;
  esac
}

# ------------------------------------------------------------- Browser Probe --
open_when_ready() {
  [ "${OPEN_BROWSER:-1}" = "1" ] || return 0
  (
    for _ in $(seq 1 60); do
      if curl -fsS -o /dev/null -m 1 "http://localhost:${PORT_WEB}" 2>/dev/null && \
         curl -fsS -o /dev/null -m 1 "http://localhost:${PORT_GATEWAY}/actuator/health" 2>/dev/null; then
        sleep 1
        open "http://localhost:${PORT_WEB}" 2>/dev/null || true
        exit 0
      fi
      sleep 1
    done
  ) &
}

# ----------------------------------------------------------- Sign-in Summary --
sign_in_hint() {
  printf "\n"
  printf "  ${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}\n"
  printf "    ${BOLD}${GREEN}Zovira is ready and serving requests!${RESET}\n"
  printf "  ${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}\n"
  printf "    ${BOLD}Frontend Web App${RESET}   : ${CYAN}http://localhost:%s${RESET}\n" "$PORT_WEB"
  printf "    ${BOLD}Backend REST API${RESET}   : ${CYAN}http://localhost:%s${RESET}\n" "$PORT_API"
  printf "    ${BOLD}Swagger UI Docs${RESET}    : ${CYAN}http://localhost:%s/swagger-ui.html${RESET}\n" "$PORT_API"
  printf "    ${BOLD}Actuator Health${RESET}    : ${CYAN}http://localhost:%s/actuator/health${RESET}\n" "$PORT_API"
  printf "\n"
  printf "    ${BOLD}Demo Accounts:${RESET}\n"
  printf "      ${BOLD}Admin${RESET}    : ${DIM}admin@zovira.test${RESET}          Password: ${BOLD}Admin@12345${RESET}\n"
  printf "      ${BOLD}Customer${RESET} : ${DIM}aarav@zovira.test${RESET}          Password: ${BOLD}Customer@123${RESET}\n"
  printf "      ${BOLD}Seller${RESET}   : ${DIM}seller.artisanloom@zovira.test${RESET} Password: ${BOLD}Seller@123${RESET}\n"
  printf "  ${BOLD}${CYAN}────────────────────────────────────────────────────────────────────${RESET}\n"
  printf "    Press ${BOLD}Ctrl+C${RESET} in this terminal to gracefully stop all services.\n"
  printf "  ${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}\n\n"
}

# ---------------------------------------------------------- Cleanup Handler ---
cleanup() {
  local exit_code=$?
  trap - INT TERM EXIT
  printf "\n"
  info "Stopping Zovira microservices..."

  for pid in "$DISCOVERY_PID" "$GATEWAY_PID" "$USER_PID" "$CATALOG_PID" "$CART_PID" "$ORDER_PID" "$BACKEND_PID" "$FRONTEND_PID"; do
    if [ -n "$pid" ]; then
      pkill -P "$pid" 2>/dev/null || true
      kill "$pid" 2>/dev/null || true
    fi
  done

  # Release ports if any background process lingered
  for port in "$PORT_GATEWAY" "$PORT_DISCOVERY" "$PORT_USER" "$PORT_CATALOG" "$PORT_CART" "$PORT_ORDER" "$PORT_WEB"; do
    local p
    p="$(lsof -nP -iTCP:"$port" -sTCP:LISTEN -t 2>/dev/null || true)"
    [ -n "$p" ] && kill -9 "$p" 2>/dev/null || true
  done

  ok "All services stopped cleanly."
  exit "$exit_code"
}

# --------------------------------------------------------- Setup Routine ------
do_setup() {
  step "Verifying system toolchain"
  check_macos
  check_node
  check_java

  step "Checking database connectivity"
  check_mongodb

  step "Checking configuration"
  ensure_env

  step "Checking frontend dependencies"
  ensure_dependencies
}

# ------------------------------------------------------------- Main Router ----
cmd="${1:-dev}"

case "$cmd" in
  dev|microservices)
    banner
    do_setup

    step "Checking network ports"
    check_port "$PORT_DISCOVERY" "Eureka Discovery Server"
    check_port "$PORT_GATEWAY" "Spring Cloud API Gateway"
    check_port "$PORT_USER" "User Service"
    check_port "$PORT_CATALOG" "Catalog Service"
    check_port "$PORT_CART" "Cart Service"
    check_port "$PORT_ORDER" "Order Service"
    check_port "$PORT_WEB" "Vite Web Server"

    trap cleanup INT TERM EXIT

    if [ ! -f "$ROOT_DIR/backend/discovery-service/target/zovira-discovery-1.0.0-SNAPSHOT.jar" ]; then
      step "Building microservices packages"
      ( cd "$ROOT_DIR/backend" && mvn -B -DskipTests package )
    fi

    step "Starting Eureka Discovery Server on http://localhost:${PORT_DISCOVERY}"
    (
      cd "$ROOT_DIR/backend"
      exec java -jar discovery-service/target/zovira-discovery-1.0.0-SNAPSHOT.jar
    ) &
    DISCOVERY_PID=$!
    sleep 2

    step "Starting User Service on http://localhost:${PORT_USER}"
    (
      cd "$ROOT_DIR/backend"
      exec java -jar user-service/target/zovira-user-service-1.0.0-SNAPSHOT.jar
    ) &
    USER_PID=$!

    step "Starting Catalog Service on http://localhost:${PORT_CATALOG}"
    (
      cd "$ROOT_DIR/backend"
      exec java -jar catalog-service/target/zovira-catalog-service-1.0.0-SNAPSHOT.jar
    ) &
    CATALOG_PID=$!

    step "Starting Cart Service on http://localhost:${PORT_CART}"
    (
      cd "$ROOT_DIR/backend"
      exec java -jar cart-service/target/zovira-cart-service-1.0.0-SNAPSHOT.jar
    ) &
    CART_PID=$!

    step "Starting Order Service on http://localhost:${PORT_ORDER}"
    (
      cd "$ROOT_DIR/backend"
      exec java -jar order-service/target/zovira-order-service-1.0.0-SNAPSHOT.jar
    ) &
    ORDER_PID=$!

    step "Starting Spring Cloud API Gateway on http://localhost:${PORT_GATEWAY}"
    (
      cd "$ROOT_DIR/backend"
      exec java -jar api-gateway/target/zovira-gateway-1.0.0-SNAPSHOT.jar
    ) &
    GATEWAY_PID=$!

    step "Starting Vite Frontend (JavaScript) on http://localhost:${PORT_WEB}"
    (
      cd "$ROOT_DIR/frontend"
      exec npm run dev -- --port "$PORT_WEB"
    ) &
    FRONTEND_PID=$!

    sign_in_hint
    open_when_ready

    # Wait for background processes to run until user interrupts
    wait "$DISCOVERY_PID" "$GATEWAY_PID" "$USER_PID" "$CATALOG_PID" "$CART_PID" "$ORDER_PID" "$FRONTEND_PID" 2>/dev/null || true
    ;;

  api|backend)
    banner
    step "Verifying Java & Database"
    check_java
    check_mongodb
    ensure_env
    check_port "$PORT_DISCOVERY" "Eureka Discovery Server"
    check_port "$PORT_GATEWAY" "API Gateway"
    check_port "$PORT_USER" "User Service"
    check_port "$PORT_CATALOG" "Catalog Service"
    check_port "$PORT_CART" "Cart Service"
    check_port "$PORT_ORDER" "Order Service"
    trap cleanup INT TERM EXIT

    if [ ! -f "$ROOT_DIR/backend/discovery-service/target/zovira-discovery-1.0.0-SNAPSHOT.jar" ]; then
      step "Building microservices packages"
      ( cd "$ROOT_DIR/backend" && mvn -B -DskipTests package )
    fi

    step "Starting Eureka Discovery Server on http://localhost:${PORT_DISCOVERY}"
    ( cd "$ROOT_DIR/backend" && exec java -jar discovery-service/target/zovira-discovery-1.0.0-SNAPSHOT.jar ) &
    DISCOVERY_PID=$!
    sleep 2

    step "Starting Microservices (User, Catalog, Cart, Order)"
    ( cd "$ROOT_DIR/backend" && exec java -jar user-service/target/zovira-user-service-1.0.0-SNAPSHOT.jar ) &
    USER_PID=$!
    ( cd "$ROOT_DIR/backend" && exec java -jar catalog-service/target/zovira-catalog-service-1.0.0-SNAPSHOT.jar ) &
    CATALOG_PID=$!
    ( cd "$ROOT_DIR/backend" && exec java -jar cart-service/target/zovira-cart-service-1.0.0-SNAPSHOT.jar ) &
    CART_PID=$!
    ( cd "$ROOT_DIR/backend" && exec java -jar order-service/target/zovira-order-service-1.0.0-SNAPSHOT.jar ) &
    ORDER_PID=$!

    step "Starting API Gateway on http://localhost:${PORT_GATEWAY}"
    ( cd "$ROOT_DIR/backend" && exec java -jar api-gateway/target/zovira-gateway-1.0.0-SNAPSHOT.jar ) &
    GATEWAY_PID=$!

    wait "$DISCOVERY_PID" "$GATEWAY_PID" "$USER_PID" "$CATALOG_PID" "$CART_PID" "$ORDER_PID" 2>/dev/null || true
    ;;

  web|frontend)
    banner
    step "Verifying Node.js & dependencies"
    check_node
    ensure_env
    ensure_dependencies
    check_port "$PORT_WEB" "Vite Web Server"
    step "Starting Vite Frontend on http://localhost:${PORT_WEB}"
    cd "$ROOT_DIR/frontend"
    exec npm run dev -- --port "$PORT_WEB"
    ;;

  setup)
    banner
    do_setup
    printf "\n  ${GREEN}${BOLD}✓ Setup complete!${RESET} Run ${BOLD}./run-mac.sh${RESET} to start Zovira.\n\n"
    ;;

  infra)
    banner
    step "Starting local infrastructure via Docker Compose"
    docker compose up -d
    ok "Docker containers started (Postgres, Redis, MinIO, Mailpit)"
    ;;

  infra-down)
    banner
    step "Stopping local infrastructure containers"
    docker compose down
    ok "Docker containers stopped"
    ;;

  test)
    banner
    do_setup
    step "Running backend API test suite"
    ( cd "$ROOT_DIR/backend" && ./mvnw -B verify )
    step "Running frontend web test suite"
    ( cd "$ROOT_DIR/frontend" && npm test )
    ok "All tests passed!"
    ;;

  lint)
    banner
    step "Linting frontend (JavaScript)"
    ( cd "$ROOT_DIR/frontend" && npm run lint )
    ok "Lint passed!"
    ;;

  build)
    banner
    do_setup
    step "Building Spring Boot Microservices packages"
    ( cd "$ROOT_DIR/backend" && mvn -B -DskipTests package )
    step "Building Vite frontend production bundle"
    ( cd "$ROOT_DIR/frontend" && npm run build )
    ok "Build complete! (backend microservices and frontend/dist)"
    ;;

  clean)
    banner
    warn "This will remove backend/target, frontend/dist and frontend/node_modules."
    printf '    Are you sure you want to clean? [y/N] '
    read -r reply
    case "$reply" in
      [yY]*)
        rm -rf "$ROOT_DIR/backend/target" "$ROOT_DIR/frontend/dist" "$ROOT_DIR/frontend/node_modules"
        ok "Clean complete. Run ./run-mac.sh setup to reinstall dependencies."
        ;;
      *)
        info "Clean cancelled."
        ;;
    esac
    ;;

  -h|--help|help)
    banner
    printf "  ${BOLD}USAGE:${RESET}\n"
    printf "    ./run-mac.sh [command]\n\n"
    printf "  ${BOLD}COMMANDS:${RESET}\n"
    printf "    ${CYAN}dev${RESET}         (default) Run both Spring Boot backend and Vite frontend\n"
    printf "    ${CYAN}api${RESET}         Run only the Spring Boot REST API\n"
    printf "    ${CYAN}web${RESET}         Run only the Vite frontend dev server\n"
    printf "    ${CYAN}setup${RESET}       Verify toolchain, generate .env, and install npm modules\n"
    printf "    ${CYAN}infra${RESET}       Start Docker Compose infrastructure (Postgres, Redis, etc.)\n"
    printf "    ${CYAN}infra-down${RESET}  Stop Docker Compose infrastructure containers\n"
    printf "    ${CYAN}test${RESET}        Run API and web automated test suites\n"
    printf "    ${CYAN}lint${RESET}        Run ESLint and Vue TypeScript type-checks\n"
    printf "    ${CYAN}build${RESET}       Compile backend JAR and build frontend distribution\n"
    printf "    ${CYAN}clean${RESET}       Clean build artifacts (target/, dist/, node_modules/)\n"
    printf "    ${CYAN}help${RESET}        Show this guide\n\n"
    ;;

  *)
    die "Unknown command '$cmd'. Run ./run-mac.sh --help for available commands."
    ;;
esac
