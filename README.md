# Xcart — E-Commerce Platform

A full-stack e-commerce application built with Spring Boot and React, featuring JWT authentication, Kafka event-driven notifications, optimistic locking for inventory, and a complete admin dashboard.

## What This Project Demonstrates

- **Domain-Driven Design** — 11 bounded context modules with clean separation (controller → service → entity → repository → DTO)
- **Event-Driven Architecture** — Order status changes flow through Kafka to a decoupled notification service
- **Concurrency Handling** — Optimistic locking (`@Version`) on inventory with retry-on-conflict for safe stock updates
- **Security** — JWT with access/refresh tokens, BCrypt, role-based authorization (`ROLE_ADMIN` / `ROLE_CUSTOMER`)
- **Database Migrations** — Flyway with baseline-on-migrate for existing schemas
- **CI/CD** — GitHub Actions builds + tests, deploys to Render on green main pushes

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Client (React)                        │
│                  TypeScript + Vite + Tailwind                 │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTPS (REST + JWT)
┌──────────────────────────▼──────────────────────────────────┐
│                    Spring Boot Backend                        │
│                                                              │
│  ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌───────────────────┐ │
│  │  Auth   │ │ Products │ │   Cart   │ │      Orders       │ │
│  │  + JWT  │ │ + Search │ │          │ │ + State Machine   │ │
│  └─────────┘ └──────────┘ └──────────┘ └─────────┬─────────┘ │
│                                                  │           │
│  ┌─────────┐ ┌──────────┐ ┌──────────┐          │           │
│  │ Payment │ │ Inventory│ │  Reviews │ │  Kafka (async)     │
│  │ Gateway │ │ @Version │ │          │          ▼           │
│  └─────────┘ └──────────┘ └──────────┘  ┌──────────────┐    │
│  ┌─────────┐ ┌──────────┐               │ Notification │    │
│  │  Admin  │ │   User   │               │   Service    │    │
│  │Dashboard│ │ Profile  │               └──────────────┘    │
│  └─────────┘ └──────────┘                                   │
└───────────────────────────────────────────┬─────────────────┘
                                            │
                    ┌───────────────────────┼───────────────────┐
                    │                       │                   │
              ┌─────▼─────┐         ┌──────▼──────┐    ┌──────▼──────┐
              │ PostgreSQL │         │ Kafka (Aiven)│    │  Cloudinary │
              │  (Render)  │         │  Cloud, SSL  │    │  (images)   │
              └───────────┘         └─────────────┘    └─────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 4.1.0, Java 17 |
| Frontend | React, TypeScript, Vite, Tailwind CSS |
| Database | PostgreSQL (Render) |
| Messaging | Apache Kafka (Aiven Cloud, SSL) |
| Image Storage | Cloudinary |
| Auth | JWT (jjwt), BCrypt, Spring Security |
| Migrations | Flyway |
| ORM | Spring Data JPA / Hibernate |
| API Docs | OpenAPI 3 (springdoc-openapi) |
| CI/CD | GitHub Actions → Render |
| Containerization | Docker (multi-stage) |

## Domain Modules

| Module | Responsibility | Key Pattern |
|--------|---------------|-------------|
| **auth** | Registration, login, JWT issuance | Access + refresh token flow |
| **user** | Profile management, password change | Role-based access |
| **product** | Product CRUD, search/filter | JPA Specifications for dynamic queries |
| **category** | Category management | Soft-delete via FK protection |
| **inventory** | Stock tracking | Optimistic locking (`@Version`) + retry |
| **cart** | Shopping cart with price snapshot | Snapshot price at add-time |
| **order** | Order placement, state machine | Allowed-transitions map, cancel = restock |
| **payment** | Payment processing | Strategy pattern (`PaymentGateway` interface) |
| **review** | Product ratings + comments | One review per user per product |
| **notification** | Order status notifications | Kafka consumer, decoupled from order flow |
| **admin** | Dashboard stats, order/user management | Admin-only endpoints |

## API Endpoints

### Public (no auth required)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/register` | Register new customer |
| POST | `/auth/login` | Login, returns JWT tokens |
| GET | `/products` | List all products |
| GET | `/products/{id}` | Get product by ID |
| GET | `/products/search?name=&categoryId=&minPrice=&maxPrice=` | Dynamic product search |
| GET | `/categories` | List categories |
| GET | `/inventory/{productId}` | Check stock |
| GET | `/products/{productId}/reviews` | List reviews for a product |
| GET | `/products/{productId}/reviews/average` | Average rating |

### Customer (requires JWT)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/auth/me` | Current user profile |
| GET/PUT | `/users/profile` | View/update profile |
| PUT | `/users/change-password` | Change password |
| GET/POST | `/cart` | View/add to cart |
| PUT/DELETE | `/cart/*` | Update/remove cart items |
| POST | `/orders` | Place order from cart |
| GET | `/orders` | List my orders |
| DELETE | `/orders/{id}` | Cancel order |
| POST | `/payments` | Process payment for order |
| POST | `/products/{productId}/reviews` | Write a review |
| PUT/DELETE | `/reviews/*` | Edit/delete own review |
| GET/PUT | `/notifications/*` | View/mark notifications read |

### Admin (requires `ROLE_ADMIN`)
| Method | Path | Description |
|--------|------|-------------|
| POST/PUT/DELETE | `/products/*` | Manage products |
| POST/PUT/DELETE | `/categories/*` | Manage categories |
| PUT | `/inventory/update` | Update stock |
| POST | `/products/images` | Upload product image |
| GET | `/admin/dashboard` | Dashboard stats |
| GET | `/admin/orders` | All orders |
| PUT | `/admin/orders/{id}/status` | Update order status |
| GET | `/admin/users` | All users |
| PUT/DELETE | `/admin/users/*` | Enable/disable/delete users |

## Database Schema

13 tables: `users`, `roles`, `user_roles`, `products`, `categories`, `inventory`, `carts`, `cart_items`, `orders`, `order_items`, `payments`, `reviews`, `notifications`

Managed by Flyway. The baseline migration (`V1__baseline_schema.sql`) captures the initial schema state.

## Running Locally

### Prerequisites
- Java 17
- PostgreSQL
- Kafka (or use [Aiven](https://aiven.io) free tier)
- [Cloudinary](https://cloudinary.com) account (free tier)
- Docker (optional, for containerized build)

### 1. Clone the repo
```bash
git clone https://github.com/TilekarPranav/xcart-ecommerce.git
cd xcart-ecommerce
```

### 2. Set up PostgreSQL
```bash
createdb ecommerce
```

### 3. Set environment variables
```bash
export DB_URL="jdbc:postgresql://localhost:5432/ecommerce"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_password"
export JWT_SECRET="your-base64-encoded-secret"
export CLOUDINARY_CLOUD_NAME="your_cloud_name"
export CLOUDINARY_API_KEY="your_api_key"
export CLOUDINARY_API_SECRET="your_api_secret"
export KAFKA_BOOTSTRAP_SERVERS="your_kafka_bootstrap_servers"
export KAFKA_TRUSTSTORE_PASSWORD="your_truststore_password"
export KAFKA_KEYSTORE_PASSWORD="your_keystore_password"
```

### 4. Run with Maven
```bash
./mvnw spring-boot:run
```

Or build and run the JAR:
```bash
./mvnw clean package -DskipTests
java -jar target/E-Commerce-0.0.1-SNAPSHOT.jar
```

### 5. Run with Docker
```bash
docker build -t xcart-ecommerce .
docker run -p 8080:8080 --env-file .env xcart-ecommerce
```

### 6. Run with Docker Compose (app + PostgreSQL)
```bash
docker-compose up -d
```

### 7. Access the API
- **API base URL:** `http://localhost:8080`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **Frontend:** [https://x-cart.onrender.com](https://x-cart.onrender.com)

## Testing

```bash
# Run all tests
./mvnw test

# Run with integration tests
./mvnw verify
```

Tests use H2 in-memory database with Mockito for service-level unit tests.

## Security Features

- JWT-based stateless authentication (15-min access token, 7-day refresh token)
- BCrypt password hashing
- Role-based authorization (`ROLE_ADMIN`, `ROLE_CUSTOMER`)
- CORS restricted to specific frontend origins
- Input validation with Bean Validation (`@Valid`)
- Global exception handler with proper HTTP status codes

## Key Design Decisions

| Decision | Why |
|----------|-----|
| **Modular monolith** (not microservices) | Clean domain boundaries that *could* be extracted into microservices, without the operational overhead |
| **Price snapshot in cart** | Prevents price-change bugs between cart-add and checkout |
| **Optimistic locking on inventory** | Safe concurrent stock updates without pessimistic DB locks |
| **Kafka for notifications** | Decouples order status changes from notification delivery |
| **Soft-delete for products** | Prevents FK violations on historical orders/reviews |
| **PaymentGateway interface** | Strategy pattern — swap MockPaymentGateway for Stripe/Razorpay without touching PaymentService |
| **Flyway baseline-on-migrate** | Adopted Flyway on an existing database without manual migration |

## Links

- **Live Backend:** [https://xcart-ecommerce.onrender.com](https://xcart-ecommerce.onrender.com)
- **Live Frontend:** [https://x-cart.onrender.com](https://x-cart.onrender.com)
- **API Docs:** [https://xcart-ecommerce.onrender.com/swagger-ui.html](https://xcart-ecommerce.onrender.com/swagger-ui.html)
- **GitHub:** [TilekarPranav/xcart-ecommerce](https://github.com/TilekarPranav/xcart-ecommerce)

## Author

**Pranav Tilekar** — Full Stack Java Developer

## License

This project is a learning/portfolio project. Feel free to explore and learn from it.
