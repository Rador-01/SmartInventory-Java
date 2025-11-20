#!/bin/bash

# SmartInventory Backend Server Startup Script
# This script starts the Spring Boot backend server on port 5001

echo "=========================================="
echo "Starting SmartInventory Backend Server"
echo "=========================================="
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed!"
    echo "Please install Maven first:"
    echo "  - Ubuntu/Debian: sudo apt-get install maven"
    echo "  - MacOS: brew install maven"
    echo "  - Windows: Download from https://maven.apache.org/"
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed!"
    echo "Please install Java JDK 17 or higher"
    exit 1
fi

echo "✓ Maven found: $(mvn -version | head -n 1)"
echo "✓ Java found: $(java -version 2>&1 | head -n 1)"
echo ""

# Navigate to project directory
cd "$(dirname "$0")"

echo "Starting Spring Boot application..."
echo "Server will be available at: http://localhost:5001"
echo "API Base URL: http://localhost:5001/api"
echo ""
echo "Press Ctrl+C to stop the server"
echo "=========================================="
echo ""

# Start the Spring Boot application
mvn spring-boot:run
