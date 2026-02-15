# Collaborative Editing System - Microservices Project

**Student:** Manahil Iftikhar  
**Project:** Collaborative Editing System (Microservices-Based Architecture)  
**Professor:** Liang Peng  
**Submission Date:** February 16, 2026

---

## 📋 Project Overview

A complete microservice-based collaborative document editing system similar to Google Docs, built using Spring Boot and Java 17. This system implements enterprise-grade microservices architecture with comprehensive version control and real-time collaboration features.

---

## ✅ Features Implemented

### 1. User Management Service (Port 8081)
**Operations:**
- User Registration with validation
- User Authentication (JWT tokens)
- User Profile Management
- Get User by ID
- Update User Profile

**Tests:** 13 JUnit tests ✅

---

### 2. Document Service (Port 8082)
**Operations:**
- Create New Documents
- Edit Documents Collaboratively
- Track Changes in Real-time
- Get Document by ID
- List Documents by Owner
- Get Public Documents

**Tests:** 14 JUnit tests ✅

---

### 3. Version Control Service (Port 8083)
**Operations:**
- Maintain Version History
- Revert to Previous Versions
- Track User Contributions
- Get Version History
- Get Specific Version

**Tests:** 13 JUnit tests ✅

---

### 4. API Gateway (Port 8080)
- Centralized request routing
- Routes to all microservices
- CORS configuration

---

## 🛠️ Technology Stack

- **Language:** Java 17
- **Framework:** Spring Boot 3.2.0
- **Architecture:** Microservices with API Gateway
- **Database:** H2 (in-memory for development)
- **Security:** Spring Security, JWT, BCrypt
- **ORM:** Hibernate/JPA
- **Testing:** JUnit 5, Spring Boot Test
- **Build Tool:** Maven 3.9+
- **API Gateway:** Spring Cloud Gateway

---

## 📋 Prerequisites

To run this project, you need:
- **Java 17** or higher
- **Maven 3.9+**
- **4 GB RAM** minimum
- **Windows/Mac/Linux**

---

## 🚀 Installation & Running Instructions

### Step 1: Build All Services

Open Command Prompt and run these commands:
```bash
# Navigate to project folder
cd collaborative-editing-system

# Build User Service
cd user-service
mvn clean install
cd ..

# Build Document Service
cd document-service
mvn clean install
cd ..

# Build Version Service
cd version-service
mvn clean install
cd ..

# Build API Gateway
cd api-gateway
mvn clean install
cd ..
```

**Expected:** All should show `BUILD SUCCESS`

---

### Step 2: Run All Services

Open **4 separate Command Prompt windows** and run:

**Window 1 - User Service:**
```bash
cd user-service
mvn spring-boot:run
```
✅ Wait for: `Started UserServiceApplication in X seconds`

**Window 2 - Document Service:**
```bash
cd document-service
mvn spring-boot:run
```
✅ Wait for: `Started DocumentServiceApplication in X seconds`

**Window 3 - Version Service:**
```bash
cd version-service
mvn spring-boot:run
```
✅ Wait for: `Started VersionServiceApplication in X seconds`

**Window 4 - API Gateway:**
```bash
cd api-gateway
mvn spring-boot:run
```
✅ Wait for: `Started ApiGatewayApplication in X seconds`

**⚠️ Important:** Keep all 4 windows open while testing!

---

## 🧪 Running Tests

Open a **5th Command Prompt** and run:
```bash
# Test User Service (13 tests)
cd user-service
mvn test

# Test Document Service (14 tests)
cd ../document-service
mvn test

# Test Version Service (13 tests)
cd ../version-service
mvn test
```

**Expected Results:**
- User Service: 13/13 tests passed ✅
- Document Service: 14/14 tests passed ✅
- Version Service: 13/13 tests passed ✅
- **Total: 40/40 tests passing**

---

## 📡 API Endpoints

All APIs are accessed through the **API Gateway** on **port 8080**.

### User Service APIs
```
POST   /api/users/register       - Register new user
POST   /api/users/login          - Authenticate user (returns JWT)
GET    /api/users/profile/{username} - Get user profile
PUT    /api/users/profile/{username} - Update user profile
GET    /api/users/{userId}       - Get user by ID
```

### Document Service APIs
```
POST   /api/documents            - Create new document
PUT    /api/documents/{id}       - Edit existing document
GET    /api/documents/{id}       - Get document by ID
GET    /api/documents/owner/{ownerId} - Get user's documents
GET    /api/documents/public     - Get all public documents
GET    /api/documents/{id}/changes - Get document change history
```

### Version Service APIs
```
POST   /api/versions             - Create new version
POST   /api/versions/revert      - Revert to previous version
GET    /api/versions/history/{documentId} - Get version history
GET    /api/versions/{documentId}/{versionNumber} - Get specific version
GET    /api/versions/contributions/{documentId} - Get user contributions
```

---

## 🎯 Quick Testing Guide

After all services are running, open a **5th Command Prompt**:

### Test 1: Register a User
```bash
curl -X POST http://localhost:8080/api/users/register -H "Content-Type: application/json" -d "{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"password123\",\"fullName\":\"Test User\"}"
```

### Test 2: Login
```bash
curl -X POST http://localhost:8080/api/users/login -H "Content-Type: application/json" -d "{\"username\":\"testuser\",\"password\":\"password123\"}"
```

### Test 3: Create Document
```bash
curl -X POST http://localhost:8080/api/documents -H "Content-Type: application/json" -d "{\"title\":\"Test Doc\",\"content\":\"Hello World\",\"ownerId\":1,\"isPublic\":false}"
```

### Test 4: Create Version
```bash
curl -X POST http://localhost:8080/api/versions -H "Content-Type: application/json" -d "{\"documentId\":1,\"content\":\"Hello World\",\"userId\":1,\"changeDescription\":\"Initial version\"}"
```

---

## 🎨 Web Interface (Optional)

A web-based UI is included for easier testing:

1. Open `collaborative-editor-ui.html` in a web browser
2. Register a new user or login
3. Create and edit documents
4. View version history
5. Test all features visually

---

## 🏗️ System Architecture
```
Client/Browser
      ↓
API Gateway (Port 8080)
      ↓
      ├─→ User Service (Port 8081) → H2 Database (userdb)
      ├─→ Document Service (Port 8082) → H2 Database (documentdb)
      └─→ Version Service (Port 8083) → H2 Database (versiondb)
```

---

## 📁 Project Structure
```
collaborative-editing-system/
│
├── api-gateway/              # API Gateway service
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       └── resources/
│   └── pom.xml
│
├── user-service/             # User management microservice
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/            # 13 JUnit tests
│   └── pom.xml
│
├── document-service/         # Document editing microservice
│   ├── src/
│   │   ├── main/
│   │   └── test/            # 14 JUnit tests
│   └── pom.xml
│
├── version-service/          # Version control microservice
│   ├── src/
│   │   ├── main/
│   │   └── test/            # 13 JUnit tests
│   └── pom.xml
│
├── collaborative-editor-ui.html  # Web interface
└── README.md                     # This file
```

---

## 📊 Project Statistics

- **Total Services:** 4 (1 Gateway + 3 Microservices)
- **REST APIs:** 16 endpoints
- **Operations per Service:** 5-6 operations
- **Total Tests:** 40 JUnit tests
- **Code Files:** 47
- **Lines of Code:** 3000+

---

## ✅ Academic Requirements Met

✅ **Three microservices** with 3+ operations each  
✅ **API Gateway** implementation  
✅ **RESTful APIs** (16 endpoints)  
✅ **Java Spring Boot** framework used  
✅ **JUnit automated tests** (40 tests included)  
✅ **Complete documentation**  
✅ **Professional code structure**  

---

## 🔐 Security Features

- **JWT Token Authentication:** Secure stateless authentication
- **BCrypt Password Hashing:** Industry-standard encryption
- **CORS Configuration:** Cross-origin request handling
- **Input Validation:** Data validation on all endpoints
- **SQL Injection Prevention:** JPA/Hibernate protection

---

## 📚 Design Patterns Used

1. **Microservices Architecture** - Service decomposition
2. **API Gateway Pattern** - Centralized routing
3. **Repository Pattern** - Data access abstraction
4. **DTO Pattern** - Data transfer objects
5. **Service Layer Pattern** - Business logic separation
6. **Dependency Injection** - Spring IoC container
7. **RESTful Architecture** - Stateless API design

---

## 🐛 Known Limitations

- **H2 In-Memory Database:** Data is lost when services restart
- **Single Instance:** No load balancing between multiple instances
- **Basic Authentication:** Can be enhanced with OAuth2

---

## 🚀 Future Enhancements

- PostgreSQL/MySQL database integration
- Docker containerization
- Real-time WebSocket support for live collaboration
- Kubernetes deployment
- Enhanced monitoring and logging
- Redis caching layer

---

## 📧 Contact Information

**Manahil Iftikhar**  
Email: manahiliftikhar593@gmail.com  
GitHub: https://github.com/Manahil-Iftikhar

---

**Thank you for reviewing this project!**