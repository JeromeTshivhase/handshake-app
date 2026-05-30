# Handshake — C2C Escrow Transaction Platform

A production-ready peer-to-peer (C2C) marketplace backend built with **Java 17 + Spring Boot 3.3**, featuring escrow-based payment protection, real-time messaging, OTP verification, fraud detection, and 2FA authentication.

---

## What It Does

Handshake solves a core trust problem in peer-to-peer transactions: neither buyer nor seller wants to go first. The platform acts as a neutral escrow layer — funds are held securely until both parties confirm the deal is complete, protecting against fraud and non-delivery.

**Core flows:**
- A buyer creates a *handshake* (offer) on a listing
- The seller accepts or rejects it
- Funds move into escrow on acceptance
- Both parties confirm delivery; funds are released to the seller
- If no confirmation within the escrow window, funds are auto-released

---

## Key Features

| Feature | Details |
|---|---|
| **Escrow engine** | Transaction state machine: `INITIATED → ESCROW_HELD → COMPLETED / REFUNDED / AUTO_RELEASED` |
| **OTP verification** | Time-limited one-time passwords for transaction confirmation and safe meetup coordination |
| **2FA authentication** | TOTP-based two-factor auth with setup, verification, and disable flows |
| **Real-time messaging** | WebSocket (STOMP) conversations between buyers and sellers with typing indicators, soft delete, and user blocking |
| **Fraud detection** | Pluggable fraud checks (blacklist, velocity limits, suspicious pattern detection) on every handshake creation and acceptance |
| **Safe meetup scheduling** | In-person exchange coordination with OTP-gated confirmation and automated cleanup jobs |
| **Listing management** | Full CRUD for marketplace listings with image upload (cloud storage), categories, and favourites |
| **Notifications** | Real-time WebSocket notifications + persistent notification history |
| **Moderation** | User reporting system with report status tracking |
| **Data security** | Field-level encryption via `@Convert` (JPA), blind index backfill for encrypted searchable fields, JWT authentication |
| **Database migrations** | 36 Flyway migrations tracking full schema evolution from initial schema to production hardening |

---

## Tech Stack

- **Language:** Java 17
- **Framework:** Spring Boot 3.3 (Web, Security, Data JPA, Validation, WebSocket)
- **Database:** PostgreSQL with Flyway migrations
- **Auth:** JWT + Spring Security + TOTP (2FA)
- **Messaging:** STOMP over WebSocket
- **Build:** Maven
- **API Docs:** SpringDoc / Swagger (OpenAPI 3)
- **Cloud:** AWS (file storage)

---

## Architecture

The project follows a domain-driven package structure:

```
com.trustmarket.handshake
├── auth/          # JWT auth, 2FA, password reset
├── transaction/   # Handshake lifecycle, escrow, payments
├── listing/       # Marketplace listings, categories, images
├── messaging/     # Conversations, messages, typing indicators, block list
├── notification/  # Real-time + persistent notifications
├── safe_meetup/   # In-person exchange coordination
├── user/          # User profiles, verification, settings, payout methods
├── moderation/    # Reports and content moderation
├── review/        # Buyer/seller reviews
├── favorite/      # Listing favourites
└── shared/        # Security, encryption, fraud detection, file storage, API wrappers
```

---

## Getting Started

### Prerequisites

- Java 17+
- PostgreSQL 14+
- Maven 3.8+

### Setup

```bash
# Clone the repo
git clone https://github.com/your-username/handshake-app.git
cd handshake-app

# Configure your database and secrets
cp src/main/resources/application.example.yml src/main/resources/application.yml
# Edit application.yml with your PostgreSQL credentials and JWT secret

# Run (Flyway will create all tables automatically)
mvn spring-boot:run
```

### API Docs

Once running, visit `http://localhost:8080/swagger-ui.html` for the full interactive API documentation.

---

## Notable Design Decisions

**Escrow auto-release:** Transactions have an `escrow_release_at` timestamp. A scheduled job checks for expired escrows and auto-releases funds, preventing funds from being locked indefinitely if a user goes inactive.

**Fraud detection hooks:** Every handshake creation and acceptance passes through `FraudDetectionService`, which is designed as a pluggable layer — the current implementation provides blacklist checking and velocity stubs ready to be backed by a persistent store or ML model.

**Field-level encryption:** Sensitive user fields use a JPA `AttributeConverter` for transparent encryption/decryption. A `BlindIndexBackfillRunner` handles backfilling searchable encrypted fields on startup.

**WebSocket security:** STOMP connections are authenticated via a `ChannelInterceptor` that validates JWTs on connection and subscription, not just on HTTP upgrade.

---

## Project Status

This is a personal project built to demonstrate production-level backend architecture patterns. Core transaction flows, authentication, and messaging are fully implemented. Fraud detection velocity checks and ML-based pattern detection are stubbed and ready for extension.

---

## Author

**Ndiene Tshivhase** — Java Backend Engineer  
[LinkedIn](https:linkedin.com/in/tshivhasendiene) · [GitHub](https://github.com/JeromeTshivhase/handshake-app) · [Portfolio](https://ndiene-tshivhase.onrender.com/)