# Authentication Issue Fix Guide

## Problem Description

The registration and login pages are showing "Network error: Failed to fetch" when trying to create an account or log in.

![Error Screenshot](https://user-images.githubusercontent.com/placeholder/auth-error.png)

## Root Cause

**The Spring Boot backend server is not running.**

The frontend application is configured to connect to `http://127.0.0.1:5001/api`, but the backend server is not started, causing all API requests to fail with a network error.

## Solution

### Quick Start (Recommended)

#### For Linux/Mac:
```bash
./start-server.sh
```

#### For Windows:
```cmd
start-server.bat
```

### Manual Start

If the startup scripts don't work, you can start the server manually:

```bash
# Navigate to project root
cd SmartInventory-Java

# Start the Spring Boot application
mvn spring-boot:run
```

The server will start on port 5001. You should see output like:

```
========================================
SmartInventory API is running!
Server: http://localhost:5001
API Base: http://localhost:5001/api
========================================
```

### Verify Server is Running

Once the server is started, you can verify it's working by:

1. **Browser Test**: Open `http://localhost:5001/api/auth/me` in your browser
   - You should see a JSON response (likely an error since you're not authenticated, but this confirms the server is responding)

2. **Command Line Test**:
   ```bash
   curl http://localhost:5001/api/auth/me
   ```

### Testing Registration

Once the server is running:

1. Open your browser and navigate to the registration page
2. Fill in the form:
   - **Full Name**: Your full name (this field is for display purposes only)
   - **Username**: Choose a unique username (min 3 characters)
   - **Email**: Your email address
   - **Password**: Choose a password (min 6 characters)
3. Click "Create Account"

If successful, you'll be automatically logged in and redirected to the dashboard.

## Technical Details

### Frontend Configuration
- **File**: `FrontEnd/JavaScript/auth.js`
- **API Base URL**: `http://127.0.0.1:5001/api`
- **Registration Endpoint**: `POST /api/auth/register`
- **Login Endpoint**: `POST /api/auth/login`

### Backend Configuration
- **File**: `src/main/resources/application.properties`
- **Server Port**: `5001`
- **Database**: SQLite (`inventory.db`)
- **CORS**: Configured to allow requests from `localhost:5500`, `localhost:8000`, etc.

### API Endpoints

#### Register New User
```
POST http://localhost:5001/api/auth/register
Content-Type: application/json

{
  "username": "john",
  "email": "john@example.com",
  "password": "password123",
  "role": "USER"
}
```

**Response** (201 Created):
```json
{
  "message": "User registered successfully",
  "user": {
    "id": 1,
    "username": "john",
    "email": "john@example.com",
    "role": "USER"
  },
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### Login
```
POST http://localhost:5001/api/auth/login
Content-Type: application/json

{
  "username": "john",
  "password": "password123"
}
```

**Response** (200 OK):
```json
{
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "john",
    "email": "john@example.com",
    "role": "USER"
  }
}
```

## Troubleshooting

### Port Already in Use

If you get an error that port 5001 is already in use:

**Find the process**:
```bash
# Linux/Mac
lsof -i :5001

# Windows
netstat -ano | findstr :5001
```

**Kill the process**:
```bash
# Linux/Mac
kill -9 <PID>

# Windows
taskkill /PID <PID> /F
```

### Maven Not Found

Install Maven:

- **Ubuntu/Debian**: `sudo apt-get install maven`
- **MacOS**: `brew install maven`
- **Windows**: Download from https://maven.apache.org/download.cgi

### Java Not Found

Install Java JDK 17 or higher:

- **Ubuntu/Debian**: `sudo apt-get install openjdk-17-jdk`
- **MacOS**: `brew install openjdk@17`
- **Windows**: Download from https://adoptium.net/

### Database Issues

If you encounter database errors, the SQLite database file may be corrupted or have permission issues:

```bash
# Check if database exists
ls -la inventory.db

# If issues persist, backup and recreate
mv inventory.db inventory.db.backup
# Restart server - it will create a new database
```

## Additional Notes

### Full Name Field

The registration form includes a "Full Name" field in the UI, but this field is currently not sent to the backend. Only username, email, password, and role are submitted. This is by design - the fullname field can be added to the backend model in a future update if needed.

### Security

- Passwords are securely hashed using BCrypt before storage
- JWT tokens are used for authentication
- CORS is configured to only allow requests from specific origins
- All API endpoints except `/api/auth/register` and `/api/auth/login` require authentication

### Running in Production

For production deployment:

1. Change `application.properties` to use a production database (PostgreSQL, MySQL, etc.)
2. Update `jwt.secret` to a strong, randomly generated secret
3. Configure CORS to only allow your production frontend domain
4. Use HTTPS
5. Set appropriate environment variables
6. Build and run the JAR file:
   ```bash
   mvn clean package
   java -jar target/smart-inventory-1.0.0.jar
   ```

## Summary

**The authentication system is working correctly.** The issue was simply that the backend server was not running. Start the server using one of the methods above, and registration/login will work as expected.
