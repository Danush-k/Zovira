# Zovira — E-Commerce Microservices Platform

Zovira is a multi-category, multi-seller e-commerce marketplace built with **Spring Boot Microservices (Spring Cloud)** and a modern **Vue 3 Frontend (JavaScript)**.

---

## Architecture Overview

The system is decomposed into decoupled microservices orchestrated via Spring Cloud Service Discovery and an API Gateway:

```
                            ┌───────────────────────────────────┐
                            │    Vue 3 Web App (JavaScript)     │
                            │      http://localhost:5173        │
                            └─────────────────┬─────────────────┘
                                              │ (REST / JSON)
                                              ▼
                            ┌───────────────────────────────────┐
                            │      Spring Cloud API Gateway     │
                            │        http://localhost:8080      │
                            └───────┬───────────┬───────────────┘
                                    │           │
           ┌────────────────────────┼───────────┼────────────────────────┐
           │                        │           │                        │
           ▼                        ▼           ▼                        ▼
┌──────────────────────┐ ┌────────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│     User Service     │ │  Catalog Service   │ │   Cart Service   │ │  Order Service   │
│      Port 8081       │ │     Port 8082      │ │    Port 8083     │ │    Port 8084     │
│  - Auth & JWT        │ │  - Products & SKU  │ │  - Shopping Cart │ │  - Checkout      │
│  - User Profiles     │ │  - Categories      │ │  - Coupons       │ │  - Orders        │
│  - Addresses         │ │  - Inventory       │ │  - Wishlists     │ │  - Payments      │
│  - Seller Profiles   │ │  - Search & Media  │ │                  │ │  - Shipments     │
│  - Audit Logs        │ │  - Reviews/Ratings │ │                  │ │  - Returns       │
└──────────┬───────────┘ └──────────┬─────────┘ └────────┬─────────┘ └────────┬─────────┘
           │                        │                    │                    │
           └────────────────────────┴─────────┬──────────┴────────────────────┘
                                              │ Service Registration
                                              ▼
                                ┌───────────────────────────┐
                                │ Netflix Eureka Discovery  │
                                │   http://localhost:8761   │
                                └───────────────────────────┘
```

---

## Services & Ports

| Service | Port | Description |
| :--- | :--- | :--- |
| **Discovery Service** | `8761` | Netflix Eureka Service Discovery Server |
| **API Gateway** | `8080` | Spring Cloud Gateway (Single Entry Point for Frontend) |
| **User Service** | `8081` | Authentication, Users, Addresses, Seller Profiles, Audit Logs |
| **Catalog Service** | `8082` | Products, Categories, Brands, Inventory, Reviews, Uploads |
| **Cart Service** | `8083` | Shopping Carts, Coupons, Wishlists |
| **Order Service** | `8084` | Checkout Flow, Orders, Payments, Shipments, Returns |
| **Frontend Web** | `5173` | Vue 3 Single Page Application (100% Pure JavaScript, Vite, TailwindCSS) |
| **PostgreSQL** | `5432` | Relational Database |
| **Redis** | `6379` | Distributed Cache & Rate Limiting |
| **MinIO** | `9000` / `9001` | Object Storage for Media |
| **Mailpit** | `1025` / `8025` | Local SMTP Mail Catcher |

---

## Tech Stack

- **Backend**:
  - Java 21 / Spring Boot 3.5.x
  - Spring Cloud 2024.0.0 (Netflix Eureka Server & Spring Cloud Gateway)
  - Spring Data JPA & PostgreSQL
  - Spring Security with JWT (Stateless Bearer Tokens)
  - Multi-Module Maven Project (`zovira-parent`)
- **Frontend**:
  - Vue 3 (Composition API, `<script setup>`) — **Pure JavaScript (No TypeScript)**
  - Pinia (State Management)
  - Vue Router (SPA Routing)
  - Vite (Build Tool & Dev Server)
  - Tailwind CSS

---

## Running the Application

### 1. Prerequisites
- Java JDK 21+
- Node.js 20+ and npm
- Docker (for database and supporting infra)

### 2. Start Infrastructure
```bash
./run-mac.sh infra
```
This boots PostgreSQL, Redis, MinIO, and Mailpit in background Docker containers.

### 3. Launch Microservices & Web Frontend
```bash
./run-mac.sh
# or
./run-mac.sh dev
```
This automatically:
1. Verifies database connectivity and environment variables
2. Starts **Eureka Discovery Server** on `http://localhost:8761`
3. Starts **User Service** on `http://localhost:8081`
4. Starts **Catalog Service** on `http://localhost:8082`
5. Starts **Cart Service** on `http://localhost:8083`
6. Starts **Order Service** on `http://localhost:8084`
7. Starts **API Gateway** on `http://localhost:8080`
8. Starts **Vite Web Frontend** on `http://localhost:5173`

### 4. Build & Test
- **Build All**:
  ```bash
  ./run-mac.sh build
  ```
- **Test All**:
  ```bash
  ./run-mac.sh test
  ```
- **Lint Frontend**:
  ```bash
  ./run-mac.sh lint
  ```
