# Marketplace Backend API

A Spring Boot REST API for a classifieds marketplace inspired by OLX.bg. Users
can register, browse public ads and publish, edit or deactivate their own
listings. There is no payment integration or card processing.

## Features

- Email/password and Google authentication with application JWTs
- Public listing catalog, category filters and full-text-style keyword search
- Search across listing title, description and location
- Seller-owned listing creation, editing and soft deletion
- Listing condition, location, contact phone and seller details
- Private conversations and unread messages linked to individual listings
- Owner-only write protection plus admin moderation endpoints
- Shopping cart and order modules for arranging an order without online payment
- PostgreSQL/H2 schema management with Liquibase
- Swagger UI, health checks, Docker, Railway, AWS and Render deployment configuration

## Main API

| Module | Path | Access |
| --- | --- | --- |
| Authentication | `/api/auth` | Public |
| Listings | `/api/listings` | Public GET, authenticated writes |
| My listings | `/api/listings/mine` | Authenticated |
| Messages | `/api/conversations` | Conversation participants |
| Legacy listing alias | `/api/products` | Same behavior as `/api/listings` |
| Categories | `/api/categories` | Public GET |
| Cart | `/api/cart` | Authenticated |
| Orders | `/api/orders` | Authenticated |
| Admin | `/api/admin/**` | Admin only |
| Health | `/actuator/health` | Public |

No `/api/payments` endpoint exists. Orders are not paid inside this service.

## Run locally

Requirements: Java 17 or newer. Local development uses an in-memory H2
database.

```bash
./gradlew bootRun
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

The demo users both use password `password123`:

- `customer@example.com` (`CUSTOMER`)
- `admin@example.com` (`ADMIN`)

## Configuration

Copy the values from `.env.example` into the IDE run configuration or export
them before starting the application.

| Variable | Required | Default | Description |
| --- | --- | --- | --- |
| `DB_HOST` / `PGHOST` | Production | `localhost` | PostgreSQL host; Railway uses `PGHOST` |
| `DB_PORT` / `PGPORT` | No | `5432` | PostgreSQL port; Railway uses `PGPORT` |
| `DB_NAME` / `PGDATABASE` | Production | `javacommerce` | Database name; Railway uses `PGDATABASE` |
| `DB_USERNAME` / `PGUSER` | Production | `postgres` | Database user; Railway uses `PGUSER` |
| `DB_PASSWORD` / `PGPASSWORD` | Production | — | Database password; Railway uses `PGPASSWORD` |
| `JWT_SECRET` | Production | — | JWT key with at least 32 bytes |
| `JWT_EXPIRATION` | No | `86400000` | Token lifetime in milliseconds |
| `GOOGLE_CLIENT_ID` | Google login | — | Google OAuth Web client ID |
| `CORS_ALLOWED_ORIGINS` | No | local frontend origins | Comma-separated origins |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | No | — | Optional production patterns |
| `PORT` | No | `8080` | HTTP port |

Production runs with the `prod` profile and requires PostgreSQL plus a stable
JWT secret. No Stripe keys or payment secrets are needed.

## Authentication

Register and then use the returned token:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "firstName": "Ivan",
    "lastName": "Ivanov",
    "email": "ivan@example.com",
    "password": "password123"
  }'
```

For protected calls send:

```text
Authorization: Bearer <token>
```

Google Identity Services credentials are accepted at `POST /api/auth/google`.
The frontend and backend must use the same `GOOGLE_CLIENT_ID`.

## Publish a listing

```bash
curl -X POST http://localhost:8080/api/listings \
  -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "iPhone 15 Pro 256 GB",
    "description": "Excellent condition, with box and charger",
    "price": 1499.00,
    "stock": 1,
    "imageUrl": "https://example.com/iphone.jpg",
    "categoryId": 1,
    "location": "Sofia",
    "condition": "LIKE_NEW",
    "contactPhone": "+359 888 123 456"
  }'
```

Allowed condition values are `NEW`, `LIKE_NEW`, `GOOD`, `FAIR` and
`FOR_PARTS`.

Seller operations:

- `GET /api/listings/mine` — all of the current user's ads
- `PUT /api/listings/{id}` — edit an owned ad
- `DELETE /api/listings/{id}` — deactivate an owned ad

A regular user receives `403 Forbidden` when attempting to change another
seller's listing.

## Messages between users

An authenticated user can start a private conversation from any active
listing owned by another user:

```http
POST /api/conversations
Authorization: Bearer <token>
Content-Type: application/json

{
  "listingId": 12,
  "message": "Здравейте, обявата още ли е актуална?"
}
```

The inbox is available at `GET /api/conversations`. Only the buyer and seller
can access a conversation. Reading `GET /api/conversations/{id}/messages`
marks received messages as read, and replies use
`POST /api/conversations/{id}/messages`.

## Tests and deployment

```bash
./gradlew test
docker build -t marketplace-backend .
```

The included `render.yaml` Blueprint creates an always-on Docker service and a
paid PostgreSQL database, wires the database credentials automatically and
runs with the `prod` profile. Follow [RENDER_DEPLOYMENT.md](RENDER_DEPLOYMENT.md)
for the dashboard steps and the values that Render requests during creation.

For Railway, the application accepts Railway's PostgreSQL variables directly,
uses its injected `PORT`, and exposes `/actuator/health` for deployment checks.
Follow [RAILWAY_DEPLOYMENT.md](RAILWAY_DEPLOYMENT.md) for the exact service,
database, variables, CORS and public-domain setup.

For AWS, the project includes an Elastic Beanstalk configuration for a Docker
environment backed by Amazon RDS PostgreSQL. Follow
[AWS_DEPLOYMENT.md](AWS_DEPLOYMENT.md) for the complete setup, environment
variables, HTTPS and deployment steps.

## Project structure

```text
src/main/java/com/shop/ecommerce
├── auth        # Registration, Google login and JWT security
├── product     # Marketplace listings and seller ownership
├── messaging   # Private listing conversations and unread messages
├── category    # Public listing categories
├── cart        # Optional cart for arranging orders
├── order       # Orders without integrated payment
├── admin       # Moderation and administration
└── common      # CORS, OpenAPI and error handling
```
