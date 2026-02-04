# TradingSandbox

## Prerequisites

- Java 17 (JDK)
- Node.js 20+
- Python 3.8+
- PostgreSQL (or Docker)

**First time?** See [SETUP.md](SETUP.md) for detailed installation instructions.

## Quick Start

This project uses a unified build system (similar to Amazon's brazil-build) to manage all services.
You can use either the `builder` script or `make` commands - they both use the same configuration.

### Using Builder (Bash Script)

```bash
# Build all services
./builder build

# Run all services
./builder run-all

# Run individual services
./builder run-api          # Spring Boot API
./builder run-frontend     # React Frontend
./builder run-data         # Python Data Acquisition

# Run tests
./builder test             # All tests
./builder test-api         # API tests only
./builder test-frontend    # Frontend linting
./builder test-data        # Python tests

# Clean build artifacts
./builder clean

# Utilities
./builder config           # Show current configuration
./builder status           # Show running services
./builder stop             # Stop all services
./builder help             # Show help
```

### Using Make

**Note:** Requires GNU Make to be installed. Install with:
```bash
# Ubuntu/Debian
sudo apt-get install build-essential

# macOS
xcode-select --install
```

Usage:
```bash
# Build all services
make build

# Run all services
make run-all

# Run individual services
make run-api
make run-frontend
make run-data

# Run tests
make test
make test-api
make test-frontend
make test-data

# Clean build artifacts
make clean

# Utilities
make config           # Show current configuration
make status           # Show running services
make stop             # Stop all services
make install          # Install all dependencies
make help             # Show all targets
```

### Configuration

There are TWO configuration files:

#### 1. [bb.config](bb.config) - Build System Configuration
Controls the build tool behavior:
- Service ports for dev servers
- Build options (Maven, npm, Python)
- Spring profiles
- Virtual environment settings

Example:
```bash
# Change dev server ports
API_PORT=8080
FRONTEND_PORT=5173
DATA_PORT=8001
```

#### 2. TradingSandboxAPI/.env - Application Runtime Configuration
Contains sensitive application configuration (auto-loaded by `builder` and `make`):
- Database credentials
- API keys
- JWT secrets
- Service URLs

**First-time setup:**
```bash
# Copy example and edit with your values
cp .env.example TradingSandboxAPI/.env
# Then edit TradingSandboxAPI/.env with your actual credentials
```

The `.env` file is automatically loaded when you run services via `./builder run-api` or `make run-api`.

## Project Structure

- **TradingSandboxAPI/** - Java Spring Boot REST API
- **TradingSandbox-FrontEnd/** - React/TypeScript Frontend
- **DataAcquisition/** - Python FastAPI Service
