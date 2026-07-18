# Live Streaming Backend

Spring Boot API for the Android live-streaming application.

## Requirements

- Java 21 or newer
- PostgreSQL 17 or newer with `psql`, or Docker with Compose

## Phase status

Phases 1 and 3A are implemented:

- Complete PostgreSQL/Flyway schema and JPA mappings for identity, wallets,
  payments, gifts, subscriptions, streaming, calls, social, notifications,
  moderation, and separate administration.
- Phone OTP and verified email/password registration and login.
- JWT access tokens, rotating refresh tokens, logout, and device sessions.
- User profile read/update/delete APIs.
- A unified wallet created with zero coin, diamond, and subscription-point
  balances for each new user.
- Public live-stream discovery, host lifecycle, audience join/leave, likes,
  five broadcaster slots, co-host requests, and host invitations.
- Short-lived Agora RTC AccessToken2 credentials. Publisher permission is
  derived from the server-side participant role, never from client input.

Phase 3A intentionally does not charge the wallet. Wallet purchases, gifts,
private calls/subscription debits, chat, notifications, recording, and
moderation workflows remain later phases; their database tables are already
mapped where applicable.

## Run locally

With a native PostgreSQL installation, run the one-time, rerunnable setup:

```powershell
.\db_setup.ps1
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

The setup prompts once for the PostgreSQL administrator password, creates the
local `livestream` role and database when needed, and applies every pending
Flyway migration. It preserves existing data when run again. `PGHOST`,
`PGPORT`, `PGUSER`, and `PGPASSWORD` can override the connection defaults.

Alternatively, start PostgreSQL with Docker; the application applies the same
Flyway migrations at startup:

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

The health endpoint is `GET http://localhost:8080/actuator/health`.

Database settings can be overridden with `DB_URL`, `DB_USERNAME`, and
`DB_PASSWORD`. Local defaults match `compose.yaml`; production must provide
secrets through the deployment environment.

The `dev` profile returns a `debugOtp` in `POST /api/v1/auth/otp/request`.
This is intentionally rejected outside the `dev` and `test` profiles. A real
SMS/email sender must replace development delivery before deployment.

Set `AUTH_JWT_SECRET` and `AUTH_OTP_SECRET` to independent secrets containing
at least 32 bytes outside local development.

## Agora setup

Create an Agora project with App Certificate authentication enabled, then set
the following environment variables before starting the backend:

```powershell
$env:AGORA_APP_ID="your-app-id"
$env:AGORA_APP_CERTIFICATE="your-primary-certificate"
$env:AGORA_TOKEN_TTL="PT15M"
```

The certificate is a backend secret and must never be included in the Android
application. The backend starts without Agora credentials so database and
authentication development can continue, but starting/joining a stream or
issuing an RTC token returns `503 AGORA_NOT_CONFIGURED` until they are set.

Use Agora RTC SDK 4.x on Android. Join with the returned `appId`, `channelName`,
`token`, and `userAccount`; do not invent a channel or user account client-side.
Set the client role from the returned `role` (`BROADCASTER` or `AUDIENCE`). Ask
for camera/microphone runtime permissions only for hosts and accepted co-hosts.
Renew through `POST /api/v1/streams/{streamId}/rtc-token` before `expiresAt` or
when the Agora SDK reports that the token will expire.

## Phase 1 API

```text
POST   /api/v1/auth/otp/request
POST   /api/v1/auth/register/phone
POST   /api/v1/auth/login/phone
POST   /api/v1/auth/register/email
POST   /api/v1/auth/login/email
POST   /api/v1/auth/password/reset
POST   /api/v1/auth/token/refresh
POST   /api/v1/auth/logout
POST   /api/v1/auth/logout-all
GET    /api/v1/auth/sessions
DELETE /api/v1/auth/sessions/{deviceId}
GET    /api/v1/users/me
PATCH  /api/v1/users/me/profile
GET    /api/v1/users/{username}
DELETE /api/v1/users/me
```

Phone numbers use E.164 format. Email registration first requests an `EMAIL`
OTP with purpose `REGISTER`; phone registration or login requests an `SMS`
OTP with the corresponding purpose.

## Phase 3A streaming API

Every endpoint below requires `Authorization: Bearer <accessToken>`.

```text
GET    /api/v1/stream-categories
GET    /api/v1/streams?categoryId={uuid}&page=0&size=20
GET    /api/v1/streams/{streamId}
POST   /api/v1/streams
POST   /api/v1/streams/{streamId}/end
POST   /api/v1/streams/{streamId}/join
POST   /api/v1/streams/{streamId}/leave
POST   /api/v1/streams/{streamId}/rtc-token
PUT    /api/v1/streams/{streamId}/like
DELETE /api/v1/streams/{streamId}/like
POST   /api/v1/streams/{streamId}/join-requests
GET    /api/v1/streams/{streamId}/join-requests
PATCH  /api/v1/streams/{streamId}/join-requests/{requestId}
POST   /api/v1/streams/{streamId}/invitations
GET    /api/v1/stream-invitations?status=PENDING
PATCH  /api/v1/stream-invitations/{invitationId}
DELETE /api/v1/streams/{streamId}/cohosts/{userId}
```

Start a stream with `{ "title", "description", "categoryId",
"thumbnailUrl" }`. Accept/reject a join request with `status` equal to
`ACCEPTED` or `REJECTED`; accept/decline an invitation with `ACCEPTED` or
`DECLINED`. A start or join response contains both the stream view and `rtc`:

```json
{
  "stream": { "id": "uuid", "status": "LIVE" },
  "rtc": {
    "appId": "Agora App ID",
    "channelName": "server-generated channel",
    "userAccount": "authenticated PostgreSQL user UUID",
    "token": "short-lived AccessToken2",
    "role": "BROADCASTER",
    "expiresAt": "2026-07-19T12:15:00Z"
  }
}
```

An accepted viewer must request a fresh RTC token to switch from audience to
broadcaster. Tokens already issued to removed co-hosts can remain valid for at
most the configured TTL, so the client must leave publishing immediately when
removed. The default TTL is 15 minutes.

## Tests

```powershell
.\mvnw.cmd test
```

Normalization/security unit tests always run. PostgreSQL integration tests use
Testcontainers and run automatically when Docker is available; they otherwise
skip without failing the build.

## Package layout

The application starts as a modular monolith. Each business capability owns
its controllers, application services, persistence code, and API models inside
one feature package.

```text
com.livestream.platform
|-- auth
|-- user
|-- streaming
|-- wallet
|-- gift
|-- subscription
|-- call
|-- notification
|-- social
|-- moderation
|-- admin
|-- config
`-- shared
```

Flyway owns the database schema and Hibernate only validates it. Migrations
`V1` through `V5` establish the complete planned PostgreSQL model, and `V6`
seeds the Phase 3A stream categories. Future
schema changes must use new versioned migrations; never edit a migration that
has already been applied to a shared environment.
