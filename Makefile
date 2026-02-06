.PHONY: help build build-api build-frontend build-data run-api run-frontend run-data run-all test test-api test-frontend test-data clean clean-api clean-frontend clean-data install

# Load configuration
include bb.config
export

# Default target
.DEFAULT_GOAL := help

# Colors
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[1;33m
RED := \033[0;31m
NC := \033[0m

##@ Build Commands

build: ## Build all services
	@echo "$(BLUE)[INFO]$(NC) Building all services..."
	@$(MAKE) build-api
	@$(MAKE) build-frontend
	@$(MAKE) build-data
	@echo "$(GREEN)[SUCCESS]$(NC) All services built successfully!"

build-api: ## Build API service
	@echo "$(BLUE)[INFO]$(NC) Building API (Maven)..."
	@cd $(API_DIR) && ./mvnw clean package $(MAVEN_OPTS)
	@echo "$(GREEN)[SUCCESS]$(NC) API build complete"

build-frontend: ## Build Frontend service
	@echo "$(BLUE)[INFO]$(NC) Building Frontend (npm)..."
	@cd $(FRONTEND_DIR) && npm install $(NPM_INSTALL_OPTS) && npm run build
	@echo "$(GREEN)[SUCCESS]$(NC) Frontend build complete"

build-data: ## Setup DataAcquisition service
	@echo "$(BLUE)[INFO]$(NC) Setting up DataAcquisition (Python)..."
	@cd $(DATA_DIR) && \
		if [ ! -f "$(PYTHON_VENV_DIR)/bin/activate" ]; then \
			echo "$(BLUE)[INFO]$(NC) Creating virtual environment..."; \
			[ -d "$(PYTHON_VENV_DIR)" ] && rm -rf $(PYTHON_VENV_DIR); \
			if ! python3 -m venv $(PYTHON_VENV_DIR); then \
				echo "$(RED)[ERROR]$(NC) Failed to create virtual environment."; \
				echo "$(RED)[ERROR]$(NC) You may need to install python3-venv:"; \
				echo "$(RED)[ERROR]$(NC)   sudo apt install python3.12-venv"; \
				exit 1; \
			fi; \
		fi && \
		. $(PYTHON_VENV_DIR)/bin/activate && \
		pip install -r requirements.txt
	@echo "$(GREEN)[SUCCESS]$(NC) DataAcquisition setup complete"

##@ Run Commands

run-api: ## Run API service
	@echo "$(BLUE)[INFO]$(NC) Running API on port $(API_PORT)..."
	@if [ -f "$(ENV_FILE)" ]; then \
		echo "$(BLUE)[INFO]$(NC) Loading environment from $(ENV_FILE)"; \
		set -a && . $(ENV_FILE) && set +a && \
		cd $(API_DIR) && ./mvnw spring-boot:run -Dspring-boot.run.profiles=$(SPRING_PROFILE); \
	else \
		cd $(API_DIR) && ./mvnw spring-boot:run -Dspring-boot.run.profiles=$(SPRING_PROFILE); \
	fi

run-frontend: ## Run Frontend dev server
	@echo "$(BLUE)[INFO]$(NC) Running Frontend on port $(FRONTEND_PORT)..."
	@cd $(FRONTEND_DIR) && npm run dev -- --host $(VITE_HOST) --port $(FRONTEND_PORT)

run-data: ## Run DataAcquisition service
	@echo "$(BLUE)[INFO]$(NC) Running DataAcquisition on port $(DATA_PORT)..."
	@cd $(DATA_DIR) && \
		. $(PYTHON_VENV_DIR)/bin/activate && \
		uvicorn src.api:app $(UVICORN_RELOAD) --host 0.0.0.0 --port $(DATA_PORT)

run-all: ## Run all services concurrently (use Ctrl+C to stop)
	@echo "$(BLUE)[INFO]$(NC) Starting all services..."
	@echo "$(YELLOW)[WARN]$(NC) Press Ctrl+C to stop all services"
	@trap 'killall java node python 2>/dev/null; exit 0' INT; \
		$(MAKE) run-api & \
		$(MAKE) run-frontend & \
		$(MAKE) run-data & \
		wait

##@ Test Commands

test: ## Run all tests
	@echo "$(BLUE)[INFO]$(NC) Running all tests..."
	@$(MAKE) test-api
	@$(MAKE) test-frontend
	@$(MAKE) test-data
	@echo "$(GREEN)[SUCCESS]$(NC) All tests passed!"

test-api: ## Run API tests
	@echo "$(BLUE)[INFO]$(NC) Running API tests..."
	@if [ -f "$(ENV_FILE)" ]; then \
		echo "$(BLUE)[INFO]$(NC) Loading environment from $(ENV_FILE)"; \
		set -a && . $(ENV_FILE) && set +a && \
		cd $(API_DIR) && ./mvnw test; \
	else \
		cd $(API_DIR) && ./mvnw test; \
	fi
	@echo "$(GREEN)[SUCCESS]$(NC) API tests complete"

test-frontend: ## Run Frontend tests and linter
	@echo "$(BLUE)[INFO]$(NC) Running Frontend tests..."
	@cd $(FRONTEND_DIR) && npx vitest run
	@echo "$(BLUE)[INFO]$(NC) Running Frontend linter..."
	@cd $(FRONTEND_DIR) && npm run lint
	@echo "$(GREEN)[SUCCESS]$(NC) Frontend tests and lint complete"

test-data: ## Run DataAcquisition tests
	@echo "$(BLUE)[INFO]$(NC) Running DataAcquisition tests..."
	@if [ ! -f "$(DATA_DIR)/$(PYTHON_VENV_DIR)/bin/activate" ]; then \
		echo "$(YELLOW)[WARN]$(NC) Python virtual environment not properly set up."; \
		echo "$(YELLOW)[WARN]$(NC) Run 'make build-data' first (requires: sudo apt install python3.12-venv)"; \
		echo "$(YELLOW)[WARN]$(NC) Skipping DataAcquisition tests."; \
	else \
		cd $(DATA_DIR) && \
		. $(PYTHON_VENV_DIR)/bin/activate && \
		pytest && \
		echo "$(GREEN)[SUCCESS]$(NC) DataAcquisition tests complete"; \
	fi

##@ Clean Commands

clean: ## Clean all build artifacts
	@echo "$(BLUE)[INFO]$(NC) Cleaning all services..."
	@$(MAKE) clean-api
	@$(MAKE) clean-frontend
	@$(MAKE) clean-data
	@echo "$(GREEN)[SUCCESS]$(NC) All services cleaned!"

clean-api: ## Clean API build artifacts
	@echo "$(BLUE)[INFO]$(NC) Cleaning API..."
	@cd $(API_DIR) && ./mvnw clean
	@echo "$(GREEN)[SUCCESS]$(NC) API cleaned"

clean-frontend: ## Clean Frontend build artifacts
	@echo "$(BLUE)[INFO]$(NC) Cleaning Frontend..."
	@cd $(FRONTEND_DIR) && rm -rf dist node_modules
	@echo "$(GREEN)[SUCCESS]$(NC) Frontend cleaned"

clean-data: ## Clean DataAcquisition artifacts
	@echo "$(BLUE)[INFO]$(NC) Cleaning DataAcquisition..."
	@cd $(DATA_DIR) && rm -rf $(PYTHON_VENV_DIR) __pycache__ .pytest_cache
	@find $(DATA_DIR) -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
	@echo "$(GREEN)[SUCCESS]$(NC) DataAcquisition cleaned"

##@ Installation Commands

install: ## Install dependencies for all services
	@echo "$(BLUE)[INFO]$(NC) Installing dependencies for all services..."
	@cd $(API_DIR) && ./mvnw dependency:resolve
	@cd $(FRONTEND_DIR) && npm install
	@cd $(DATA_DIR) && \
		if [ ! -d "$(PYTHON_VENV_DIR)" ]; then \
			python3 -m venv $(PYTHON_VENV_DIR); \
		fi && \
		. $(PYTHON_VENV_DIR)/bin/activate && \
		pip install -r requirements.txt
	@echo "$(GREEN)[SUCCESS]$(NC) All dependencies installed!"

##@ Utility Commands

status: ## Show running services
	@echo "$(BLUE)[INFO]$(NC) Checking TradingSandbox service status..."
	@echo "\n$(BLUE)API (Spring Boot):$(NC)"
	@pgrep -f "spring-boot:run" > /dev/null && pgrep -fa "spring-boot:run" | head -1 || echo "  Not running"
	@echo "\n$(BLUE)Frontend (Vite):$(NC)"
	@pgrep -f "vite.*TradingSandbox-FrontEnd" > /dev/null && pgrep -fa "vite.*TradingSandbox-FrontEnd" | head -1 || echo "  Not running"
	@echo "\n$(BLUE)DataAcquisition (uvicorn):$(NC)"
	@pgrep -f "uvicorn.*DataAcquisition" > /dev/null && pgrep -fa "uvicorn.*DataAcquisition" | head -1 || echo "  Not running"

stop: ## Stop all running services
	@echo "$(YELLOW)[WARN]$(NC) Stopping TradingSandbox services..."
	@echo "$(BLUE)[INFO]$(NC) Stopping API (Spring Boot)..."
	@pkill -f "spring-boot:run" 2>/dev/null || true
	@echo "$(BLUE)[INFO]$(NC) Stopping Frontend (Vite)..."
	@pkill -f "vite.*TradingSandbox-FrontEnd" 2>/dev/null || true
	@echo "$(BLUE)[INFO]$(NC) Stopping DataAcquisition (uvicorn)..."
	@pkill -f "uvicorn.*DataAcquisition" 2>/dev/null || true
	@sleep 1
	@echo "$(GREEN)[SUCCESS]$(NC) TradingSandbox services stopped"

logs-api: ## Show API logs
	@cd $(API_DIR) && tail -f logs/spring.log 2>/dev/null || echo "No log file found"

config: ## Show current configuration
	@echo "$(BLUE)Current Configuration:$(NC)"
	@echo "API Port: $(API_PORT)"
	@echo "Frontend Port: $(FRONTEND_PORT)"
	@echo "Data Port: $(DATA_PORT)"
	@echo "Database: $(DB_HOST):$(DB_PORT)/$(DB_NAME)"
	@echo "Spring Profile: $(SPRING_PROFILE)"

##@ Help

help: ## Display this help message
	@awk 'BEGIN {FS = ":.*##"; printf "\n$(BLUE)TradingSandbox Build System$(NC)\n\nUsage:\n  make $(YELLOW)<target>$(NC)\n"} /^[a-zA-Z_-]+:.*?##/ { printf "  $(YELLOW)%-18s$(NC) %s\n", $$1, $$2 } /^##@/ { printf "\n$(BLUE)%s$(NC)\n", substr($$0, 5) } ' $(MAKEFILE_LIST)
	@echo ""
