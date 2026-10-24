# Fifteen Hundun (15-Hundun)

A full-stack personal management platform featuring a Spring Boot backend and a Vue.js frontend.

## Project Overview

Fifteen Hundun is a feature-complete full-stack application offering user management, memo/note-taking, and a feedback system. The system supports multiple login methods (username, email, phone number) and includes a comprehensive admin backend.

## Technology Stack

### Backend

- **Framework**: Spring Boot  
- **Database**: MySQL + MyBatis  
- **Cache**: Redis  
- **Authentication**: JWT  
- **File Storage**: Alibaba Cloud OSS  
- **WebSocket**: Supports real-time communication  
- **Email Service**: SMTP email sending  

### Frontend

- **Framework**: Vue 3  
- **Build Tool**: Vite  
- **State Management**: Pinia  
- **Routing**: Vue Router  

## Feature Modules

### User Module

- User registration/login (supports username, email, phone number)  
- Email verification code login  
- Avatar upload  
- User information management  
- Admin invitation code generation  

### Memo Module

- Create and manage notes  
- Category support (main type, sub-type)  

### Feedback System

- Users submit feedback  
- Admins process feedback  
- Email notifications sent to users regarding feedback status  

### Admin Dashboard

- User management (view, delete, disable)  
- Feedback processing  
- Operation log viewing  
- Admin invitation  

## Project Structure

```
15-hundun/
├── back-end/hundun-lab/          # Spring Boot backend
│   └── src/main/java/com/hundunlab/site/
│       ├── common/                   # Common components
│       │   ├── anno/                 # Custom annotations
│       │   ├── config/               # Configuration classes
│       │   ├── constant/             # Constant definitions
│       │   ├── context/              # Context
│       │   ├── exception/            # Exception handling
│       │   ├── response/             # Response wrapper
│       │   └── util/                 # Utility classes
│       ├── framework/                # Framework layer
│       │   ├── aspect/               # AOP aspects
│       │   ├── interception/         # Interceptors
│       │   └── task/                 # Scheduled tasks
│       └── module/                   # Business modules
│           ├── memo/                 # Memo module
│           └── user/                 # User module
├── front-end/hundun-front/           # Vue.js frontend
│   └── src/
│       ├── api/                      # API interfaces
│       ├── assets/                   # Static assets
│       ├── router/                   # Routing configuration
│       ├── stores/                   # State management
│       ├── utils/                    # Utility functions
│       └── views/                    # Page views
└── file/
    └── hundun.sql                    # Database script
```

## Quick Start

### Environment Requirements

- JDK 17+  
- Maven 3.8+  
- Node.js 16+  
- MySQL 8.0+  
- Redis 6.0+  

### Backend Configuration

1. Create a database and import `file/hundun.sql`

2. Modify the configuration file: `back-end/hundun-lab/src/main/resources/application.yml`

3. Configure Alibaba Cloud OSS (optional)

4. Run the project:

```bash
cd back-end/hundun-lab
mvn spring-boot:run
```

### Frontend Configuration

1. Install dependencies:

```bash
cd front-end/hundun-front
npm install
# or
pnpm install
```

2. Start the development server:

```bash
npm run dev
```

3. Build the production version:

```bash
npm run build
```

## API Endpoints

### User Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | /user/register | User registration |
| POST | /user/login/username-password | Username/password login |
| POST | /user/login/email-code | Email verification code login |
| POST | /user/login/email-password | Email/password login |
| POST | /user/login/phone-password | Phone/password login |
| GET | /user/info | Get user information |
| PUT | /user/avatar | Update avatar |

### Feedback Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | /feedback | Get all feedback |
| POST | /feedback | Add feedback |
| PUT | /feedback/status | Update feedback status |
| DELETE | /feedback/handled | Delete handled feedback |

### Admin Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | /users/count | Get user count |
| PUT | /user/status | Modify user status |
| DELETE | /user/{id} | Delete user |
| GET | /logs | View operation logs |

## Configuration Notes

### Redis Configuration

Redis is used for caching and session management. Primary cached data includes:
- User information
- Feedback list
- JWT tokens

### Email Configuration

Set SMTP parameters in the configuration file:
- Email address
- Authorization code
- SMTP server host

## License

This project is intended solely for learning and communication purposes.