#!/bin/bash

# TradingSandbox Builder (brazil-build style)
# Unified build, run, and test system for all services

set -e  # Exit on error

# Load configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$SCRIPT_DIR/bb.config"

if [ -f "$CONFIG_FILE" ]; then
    source "$CONFIG_FILE"
else
    echo "Warning: bb.config not found, using defaults"
    # Default values
    API_PORT=8080
    FRONTEND_PORT=5173
    DATA_PORT=8001
    API_DIR="TradingSandboxAPI"
    FRONTEND_DIR="TradingSandbox-FrontEnd"
    DATA_DIR="DataAcquisition"
    SPRING_PROFILE="dev"
    VITE_HOST="localhost"
    UVICORN_RELOAD="--reload"
    PYTHON_VENV_DIR="venv"
    USE_COLORS=1
fi

# Colors for output
if [ "$USE_COLORS" = "1" ]; then
    RED='\033[0;31m'
    GREEN='\033[0;32m'
    YELLOW='\033[1;33m'
    BLUE='\033[0;34m'
    NC='\033[0m' # No Color
else
    RED=''
    GREEN=''
    YELLOW=''
    BLUE=''
    NC=''
fi

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# Load environment variables from .env file
load_env() {
    if [ -f "$ENV_FILE" ]; then
        log_info "Loading environment from $ENV_FILE"
        set -a  # automatically export all variables
        source "$ENV_FILE"
        set +a
    else
        log_warn ".env file not found at $ENV_FILE"
    fi
}

# Build commands
build_api() {
    log_info "Building API (Maven)..."
    cd "$API_DIR"
    ./mvnw clean package ${MAVEN_OPTS:--DskipTests}
    cd ..
    log_success "API build complete"
}

build_frontend() {
    log_info "Building Frontend (npm)..."
    cd "$FRONTEND_DIR"
    npm install ${NPM_INSTALL_OPTS}
    npm run build
    cd ..
    log_success "Frontend build complete"
}

build_data() {
    log_info "Setting up DataAcquisition (Python)..."
    cd "$DATA_DIR"

    # Check if venv is properly set up
    if [ ! -f "$PYTHON_VENV_DIR/bin/activate" ]; then
        log_info "Creating virtual environment..."
        # Remove incomplete venv if it exists
        [ -d "$PYTHON_VENV_DIR" ] && rm -rf "$PYTHON_VENV_DIR"

        # Try to create venv
        if ! python3 -m venv "$PYTHON_VENV_DIR"; then
            log_error "Failed to create virtual environment."
            log_error "You may need to install python3-venv:"
            log_error "  sudo apt install python3.12-venv"
            cd ..
            return 1
        fi
    fi

    source "$PYTHON_VENV_DIR/bin/activate"
    pip install -r requirements.txt
    deactivate
    cd ..
    log_success "DataAcquisition setup complete"
}

build_all() {
    log_info "Building all services..."
    build_api
    build_frontend
    build_data
    log_success "All services built successfully!"
}

# Run commands
run_api() {
    log_info "Running API on port $API_PORT..."
    load_env
    cd "$API_DIR"
    ./mvnw spring-boot:run -Dspring-boot.run.profiles="$SPRING_PROFILE"
}

run_frontend() {
    log_info "Running Frontend on port $FRONTEND_PORT..."
    cd "$FRONTEND_DIR"
    npm run dev -- --host "$VITE_HOST" --port "$FRONTEND_PORT"
}

run_data() {
    log_info "Running DataAcquisition on port $DATA_PORT..."
    cd "$DATA_DIR"
    source "$PYTHON_VENV_DIR/bin/activate"
    uvicorn src.api:app $UVICORN_RELOAD --host 0.0.0.0 --port "$DATA_PORT"
}

run_all() {
    log_info "Starting all services..."
    load_env

    # Track PIDs for cleanup
    PIDS=()

    # Function to cleanup on failure
    cleanup_on_failure() {
        log_error "Service failed to start. Stopping all services..."
        for pid in "${PIDS[@]}"; do
            kill "$pid" 2>/dev/null || true
        done
        exit 1
    }

    # Start API in background
    log_info "Starting API..."
    cd "$API_DIR"
    ./mvnw spring-boot:run &
    API_PID=$!
    PIDS+=($API_PID)
    cd ..

    # Check if API started
    sleep 2
    if ! kill -0 "$API_PID" 2>/dev/null; then
        log_error "API failed to start"
        cleanup_on_failure
    fi
    log_success "API started (PID: $API_PID)"

    # Start Data service in background
    log_info "Starting DataAcquisition..."
    if [ ! -f "$DATA_DIR/$PYTHON_VENV_DIR/bin/activate" ]; then
        log_error "DataAcquisition venv not found. Run './builder build-data' first."
        cleanup_on_failure
    fi

    cd "$DATA_DIR"
    source "$PYTHON_VENV_DIR/bin/activate"
    uvicorn src.api:app $UVICORN_RELOAD --host 0.0.0.0 --port "$DATA_PORT" &
    DATA_PID=$!
    PIDS+=($DATA_PID)
    deactivate
    cd ..

    # Check if Data service started
    sleep 2
    if ! kill -0 "$DATA_PID" 2>/dev/null; then
        log_error "DataAcquisition failed to start"
        cleanup_on_failure
    fi
    log_success "DataAcquisition started (PID: $DATA_PID)"

    # Start Frontend
    log_info "Starting Frontend..."
    cd "$FRONTEND_DIR"
    npm run dev &
    FRONTEND_PID=$!
    PIDS+=($FRONTEND_PID)
    cd ..

    # Check if Frontend started
    sleep 2
    if ! kill -0 "$FRONTEND_PID" 2>/dev/null; then
        log_error "Frontend failed to start"
        cleanup_on_failure
    fi
    log_success "Frontend started (PID: $FRONTEND_PID)"

    log_success "All services started successfully!"
    log_warn "Press Ctrl+C to stop all services"

    # Wait for interrupt
    trap "log_info 'Stopping all services...'; kill ${PIDS[@]} 2>/dev/null; exit 0" INT TERM
    wait
}

# Test commands
test_api() {
    log_info "Running API tests..."
    load_env
    cd "$API_DIR"
    ./mvnw test
    cd ..
    log_success "API tests complete"
}

test_frontend() {
    log_info "Running Frontend linter..."
    cd "$FRONTEND_DIR"
    npm run lint
    cd ..
    log_success "Frontend lint complete"
}

test_data() {
    log_info "Running DataAcquisition tests..."
    cd "$DATA_DIR"
    if [ ! -f "$PYTHON_VENV_DIR/bin/activate" ]; then
        log_warn "Python virtual environment not properly set up."
        log_warn "Run './builder build-data' first (requires: sudo apt install python3.12-venv)"
        log_warn "Skipping DataAcquisition tests."
        cd ..
        return 0
    fi
    source "$PYTHON_VENV_DIR/bin/activate"
    pytest
    deactivate
    cd ..
    log_success "DataAcquisition tests complete"
}

test_all() {
    log_info "Running all tests..."
    test_api
    test_frontend
    test_data
    log_success "All tests passed!"
}

# Clean commands
clean_api() {
    log_info "Cleaning API..."
    cd "$API_DIR"
    ./mvnw clean
    cd ..
    log_success "API cleaned"
}

clean_frontend() {
    log_info "Cleaning Frontend..."
    cd "$FRONTEND_DIR"
    rm -rf dist node_modules
    cd ..
    log_success "Frontend cleaned"
}

clean_data() {
    log_info "Cleaning DataAcquisition..."
    cd "$DATA_DIR"
    rm -rf "$PYTHON_VENV_DIR" __pycache__ .pytest_cache
    find . -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
    cd ..
    log_success "DataAcquisition cleaned"
}

clean_all() {
    log_info "Cleaning all services..."
    clean_api
    clean_frontend
    clean_data
    log_success "All services cleaned!"
}

# Help command
show_help() {
    cat << EOF
TradingSandbox Builder

Usage: ./builder <command>

BUILD COMMANDS:
  build              Build all services
  build-api          Build API only
  build-frontend     Build Frontend only
  build-data         Setup DataAcquisition only

RUN COMMANDS:
  run-api            Run API service
  run-frontend       Run Frontend dev server
  run-data           Run DataAcquisition service
  run-all            Run all services concurrently

TEST COMMANDS:
  test               Run all tests
  test-api           Run API tests
  test-frontend      Run Frontend lint
  test-data          Run DataAcquisition tests

CLEAN COMMANDS:
  clean              Clean all build artifacts
  clean-api          Clean API only
  clean-frontend     Clean Frontend only
  clean-data         Clean DataAcquisition only

OTHER COMMANDS:
  help               Show this help message
  config             Show current configuration
  status             Show running services
  stop               Stop all running services

CONFIGURATION:
  Edit bb.config to customize ports, URLs, and build settings

EOF
}

# Main command dispatcher
case "$1" in
    # Build commands
    build)
        build_all
        ;;
    build-api)
        build_api
        ;;
    build-frontend)
        build_frontend
        ;;
    build-data)
        build_data
        ;;

    # Run commands
    run-api)
        run_api
        ;;
    run-frontend)
        run_frontend
        ;;
    run-data)
        run_data
        ;;
    run-all)
        run_all
        ;;

    # Test commands
    test)
        test_all
        ;;
    test-api)
        test_api
        ;;
    test-frontend)
        test_frontend
        ;;
    test-data)
        test_data
        ;;

    # Clean commands
    clean)
        clean_all
        ;;
    clean-api)
        clean_api
        ;;
    clean-frontend)
        clean_frontend
        ;;
    clean-data)
        clean_data
        ;;

    # Utility commands
    config)
        echo -e "${BLUE}Current Configuration:${NC}"
        echo "API Port: $API_PORT"
        echo "Frontend Port: $FRONTEND_PORT"
        echo "Data Port: $DATA_PORT"
        echo "Database: $DB_HOST:$DB_PORT/$DB_NAME"
        echo "Spring Profile: $SPRING_PROFILE"
        echo ""
        echo "Edit bb.config to change these settings"
        ;;

    status)
        log_info "Checking TradingSandbox service status..."

        echo -e "\n${BLUE}API (Spring Boot):${NC}"
        if pgrep -f "spring-boot:run" > /dev/null; then
            pgrep -fa "spring-boot:run" | head -1
        else
            echo "  Not running"
        fi

        echo -e "\n${BLUE}Frontend (Vite):${NC}"
        if pgrep -f "vite.*TradingSandbox-FrontEnd" > /dev/null; then
            pgrep -fa "vite.*TradingSandbox-FrontEnd" | head -1
        else
            echo "  Not running"
        fi

        echo -e "\n${BLUE}DataAcquisition (uvicorn):${NC}"
        if pgrep -f "uvicorn.*DataAcquisition" > /dev/null; then
            pgrep -fa "uvicorn.*DataAcquisition" | head -1
        else
            echo "  Not running"
        fi
        ;;

    stop)
        log_warn "Stopping TradingSandbox services..."

        # Only kill our specific services by matching exact patterns
        log_info "Stopping API (Spring Boot)..."
        pkill -f "spring-boot:run" 2>/dev/null || true

        log_info "Stopping Frontend (Vite)..."
        pkill -f "vite.*TradingSandbox-FrontEnd" 2>/dev/null || true

        log_info "Stopping DataAcquisition (uvicorn)..."
        pkill -f "uvicorn.*DataAcquisition" 2>/dev/null || true

        # Wait a moment for graceful shutdown
        sleep 1

        log_success "TradingSandbox services stopped"
        ;;

    # Help
    help|--help|-h|"")
        show_help
        ;;

    *)
        log_error "Unknown command: $1"
        echo ""
        show_help
        exit 1
        ;;
esac
