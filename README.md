# VaultLink Backend — Kotlin Spring Boot

## Quick Start

```bash
cd vaultlink-backend
./gradlew bootRun
```

Server starts at **http://localhost:8080**
H2 Console: **http://localhost:8080/h2-console** (JDBC URL: `jdbc:h2:mem:vaultlinkdb`)

## Default Users (seeded on startup)

| Email | Password | Role |
|---|---|---|
| admin@kleeto.com | kleeto123 | KLEETO |
| neha@homefirst.com | homefirst123 | HOMEFIRST |
| admin@vaultlink.com | admin123 | ADMIN |

## Tech Stack
- Kotlin 1.9 + Spring Boot 3.2
- Spring Security + JWT (jjwt 0.12)
- Spring Data JPA + H2 (dev) / PostgreSQL (prod)
- BCrypt password hashing

## API Endpoints

### Auth
| Method | URL | Description |
|---|---|---|
| POST | /api/auth/login | Login → returns JWT + refreshToken |
| POST | /api/auth/refresh | Refresh access token |
| POST | /api/auth/logout | Invalidate refresh token |
| GET | /api/auth/me | Get current user profile |

### Kleeto — Pickup Workflow
| Method | URL | Auth |
|---|---|---|
| GET | /api/pickup-requests | Any |
| GET | /api/pickup-requests?state=pending | Any |
| PATCH | /api/pickup-requests/{id}/confirm | KLEETO |
| PATCH | /api/pickup-requests/{id}/intransit | KLEETO |
| PATCH | /api/pickup-requests/{id}/delivered | KLEETO |

### HomeFirst — Vault / Documents
| Method | URL | Auth |
|---|---|---|
| GET | /api/sf-requests | Any |
| GET | /api/sf-requests/{id} | Any |
| PATCH | /api/sf-requests/{prId}/lais/{laiNumber}/acknowledge | HOMEFIRST |
| PATCH | /api/sf-requests/{prId}/lais/{laiNumber}/revert | HOMEFIRST |
| PATCH | /api/sf-requests/{prId}/lais/{laiNumber}/docs/{idx}/vault | HOMEFIRST |

### Dashboard
| Method | URL |
|---|---|
| GET | /api/dashboard/kleeto |
| GET | /api/dashboard/homefirst |

### Acknowledgements
| Method | URL |
|---|---|
| GET | /api/acknowledgements |
| GET | /api/acknowledgements/pending |
| POST | /api/acknowledgements/upload (multipart: file, laiNumber) |

### Reconciliation
| Method | URL |
|---|---|
| GET | /api/reconciliation/summary |
| GET | /api/reconciliation |
| POST | /api/reconciliation/run |

### Reports
| Method | URL |
|---|---|
| GET | /api/reports |
| POST | /api/reports/generate |

### Branches
| Method | URL |
|---|---|
| GET | /api/branches |
| GET | /api/branches/search?q=... |

## Switch to PostgreSQL (Production)
Edit `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/vaultlinkdb
    username: vaultlink
    password: yourpassword
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```
