# Development Environment Setup

## Prerequisites Installation

### 1. Install Java (JDK 17)

The API requires Java 17. Install it with:

```bash
# Ubuntu/Debian/WSL2
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk

# Verify installation
java -version
```

After installing, set JAVA_HOME (add to ~/.zshrc):

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

Then reload your shell:
```bash
source ~/.zshrc
```

### 2. Install Node.js (for Frontend)

```bash
# Using nvm (recommended)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
source ~/.zshrc
nvm install 20
nvm use 20

# Or using apt
sudo apt-get install -y nodejs npm
```

### 3. Install Python 3 (for DataAcquisition)

```bash
sudo apt-get install -y python3 python3-pip python3-venv
```

### 4. Install PostgreSQL (Optional - for local development)

```bash
sudo apt-get install -y postgresql postgresql-contrib
```

Or use Docker:
```bash
sudo apt-get install -y docker.io docker-compose
```

### 5. Install Make (Optional - for Makefile support)

```bash
sudo apt-get install -y build-essential
```

## First-Time Project Setup

After installing the prerequisites:

```bash
# 1. Setup environment variables
cp .env.example TradingSandboxAPI/.env
# Edit TradingSandboxAPI/.env with your actual credentials

# 2. Build all services
./builder build

# 3. Run tests
./builder test

# 4. Start all services
./builder run-all
```

## Troubleshooting

### Maven wrapper issues
If you see "cannot open maven-wrapper.properties", the wrapper files are missing.
They should now be in place. Just install Java and try again.

### JAVA_HOME not set
Add to ~/.zshrc:
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```
Then reload: `source ~/.zshrc`

### Permission denied on ./bb
Run:
```bash
chmod +x ./bb
```
