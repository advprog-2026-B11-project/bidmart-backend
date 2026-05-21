# BidMart Backend — API Contract

**Version:** 2.0
**Last Updated:** 2026-05-18
**Base URL (local):** `http://localhost:8080`
**Tech Stack:** Spring Boot 4.x · Java 21 · PostgreSQL · JWT · WebSocket (STOMP)

---

## Table of Contents

1. [Conventions](#conventions)
2. [Authentication Flow](#authentication-flow)
3. [Auth Module](#1-auth-module)
4. [User Profile Module](#2-user-profile-module)
5. [Session Module](#3-session-module)
6. [Admin Module](#4-admin-module)
7. [Wallet Module](#5-wallet-module)
8. [Listing Module](#6-listing-module)
9. [Category Module](#7-category-module)
10. [Bidding Module](#8-bidding-module)
11. [Notification Module](#9-notification-module)
12. [Order Module](#10-order-module)
13. [WebSocket Real-Time Notifications](#11-websocket-real-time-notifications)
14. [Error Response Format](#error-response-format)
15. [Domain Models / Enums](#domain-models--enums)
16. [Recommended FE Flow](#recommended-fe-flow)

---

## Conventions

### Headers
| Header | Required When | Value |
|---|---|---|
| `Authorization` | Endpoint butuh auth | `Bearer <accessToken>` |
| `Content-Type` | Request berbody | `application/json` |
| `User-Agent` | Login | Otomatis dari browser (disimpan sebagai `deviceInfo`) |

### HTTP Status Codes (umum)
| Code | Meaning |
|---|---|
| 200 | OK — success |
| 201 | Created — resource baru berhasil dibuat |
| 204 | No Content — success, no body (untuk DELETE) |
| 400 | Bad Request — validasi gagal |
| 401 | Unauthorized — tidak ada / invalid token |
| 403 | Forbidden — akses ditolak (bukan owner / role kurang) |
| 404 | Not Found — resource tidak ada |
| 409 | Conflict — race condition / resource sudah ada |
| 422 | Unprocessable Entity — saldo tidak cukup (bidding/wallet) |
| 500 | Internal Server Error |

### ID Format
Semua ID menggunakan **UUID v4** (string), contoh: `"550e8400-e29b-41d4-a716-446655440000"`.

### Money Format
Semua nominal uang menggunakan **BigDecimal** dengan presisi 2 desimal (rupiah). Dikirim sebagai number JSON (bukan string). Contoh: `5000000` atau `5000000.00`.

### Date/Time Format
- `createdAt`, `endTime`, dll → **ISO 8601 LocalDateTime**: `"2026-06-01T18:00:00"`
- `expiresAt` (session) → **ISO 8601 Instant**: `"2026-05-25T10:00:00Z"`

---

## Authentication Flow

```
Register ──▶ (token saved) ──▶ Login ──▶ accessToken (15 menit)
                                  │
                                  ├─ MFA enabled? ──▶ tempToken ──▶ verify-mfa ──▶ accessToken
                                  │
                                  └─ refreshToken (7 hari) ──▶ /api/auth/refresh ──▶ new tokens
```

**FE wajib menyimpan**:
- `accessToken` — disertakan di header semua request authenticated
- `refreshToken` — untuk regenerate access token saat expired

**Token expiry**:
- Access token: **15 menit** (900,000 ms)
- Refresh token: **7 hari** (604,800,000 ms)
- Temp MFA token: **5 menit** (300,000 ms)

---

## 1. Auth Module

Base: `/api/auth`

### 1.1 Register

```
POST /api/auth/register
```

**Auth:** No
**Body:**
```json
{
  "username": "testuser",
  "email": "testuser@example.com",
  "displayName": "Test User",
  "password": "password123"
}
```

**Validation:**
- `username`: 3–50 karakter, unique
- `email`: format email valid, unique
- `displayName`: tidak boleh kosong
- `password`: minimum 8 karakter

**Response 201 Created:**
```json
{
  "accessToken": null,
  "refreshToken": null,
  "username": "testuser",
  "email": "testuser@example.com",
  "displayName": "Test User",
  "role": "USER",
  "mfaRequired": false,
  "tempToken": null
}
```

> **Catatan:** Register tidak otomatis login. User harus call `/login` setelahnya. Wallet otomatis dibuat lewat event listener.

**Error:**
- `400` — Username/email sudah dipakai, atau validasi gagal

---

### 1.2 Login

```
POST /api/auth/login
```

**Auth:** No
**Body:**
```json
{
  "identifier": "testuser",
  "password": "password123"
}
```

> `identifier` bisa berupa **username ATAU email**.

**Response 200 OK** (MFA disabled):
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "username": "testuser",
  "email": "testuser@example.com",
  "displayName": "Test User",
  "role": "USER",
  "mfaRequired": false,
  "tempToken": null
}
```

**Response 200 OK** (MFA enabled):
```json
{
  "accessToken": null,
  "refreshToken": null,
  "username": null,
  "email": null,
  "displayName": null,
  "role": null,
  "mfaRequired": true,
  "tempToken": "eyJhbGc..."
}
```

> Jika `mfaRequired = true`, FE harus tampilkan input kode TOTP dan lanjut ke `/verify-mfa`.

**Error:**
- `400` — Password salah, user tidak ditemukan, atau akun deactivated

---

### 1.3 Verify MFA

```
POST /api/auth/verify-mfa
```

**Auth:** No (pakai tempToken di body)
**Body:**
```json
{
  "tempToken": "<dari response login>",
  "code": "123456"
}
```

**Response 200 OK:** Sama seperti login sukses (`accessToken` + `refreshToken`).

---

### 1.4 Refresh Token

```
POST /api/auth/refresh
```

**Auth:** No
**Body:**
```json
{
  "refreshToken": "eyJhbGc..."
}
```

**Response 200 OK:** Token baru (access + refresh). Refresh token lama otomatis di-revoke.

**FE Pattern (Recommended):**
Interceptor HTTP — saat response 401, otomatis call `/refresh`, retry request asli.

---

### 1.5 Verify Email

```
GET /api/auth/verify?token={emailVerificationToken}
```

**Auth:** No
**Response 200:** `"Email successfully verified!"` (plain text)
**Response 400:** `"Verification token is invalid or not found."`

---

## 2. User Profile Module

Base: `/api/users`

### 2.1 Get My Profile

```
GET /api/users/me
```

**Auth:** Bearer JWT
**Response 200:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "testuser",
  "email": "testuser@example.com",
  "displayName": "Test User",
  "phoneNumber": "08123456789",
  "role": "USER",
  "isEmailVerified": false
}
```

### 2.2 Update Profile

```
PUT /api/users/me
```

**Auth:** Bearer JWT
**Body** (semua field optional, hanya yang non-null/non-blank yang di-update):
```json
{
  "displayName": "Updated Display Name",
  "phoneNumber": "08198765432"
}
```

**Response 200:** Profile yang baru (sama format dengan GET /me).

### 2.3 Delete My Account

```
DELETE /api/users/me
```

**Auth:** Bearer JWT
**Response 204 No Content**

> ⚠️ Permanen. Semua session juga dihapus.

---

## 3. Session Module

Base: `/api/sessions`

### 3.1 List My Active Sessions

```
GET /api/sessions
```

**Auth:** Bearer JWT
**Response 200:**
```json
[
  {
    "id": "uuid",
    "deviceInfo": "Mozilla/5.0 ...",
    "createdAt": "2026-05-18T10:00:00Z",
    "expiresAt": "2026-05-25T10:00:00Z"
  }
]
```

### 3.2 Revoke Session

```
DELETE /api/sessions/{sessionId}
```

**Auth:** Bearer JWT (hanya bisa revoke session milik sendiri)
**Response 204 No Content**

> **Session limit:** Max 3 session aktif per user. Login baru saat sudah penuh akan otomatis me-revoke session terlama.

---

## 4. Admin Module

> ⚠️ Hanya bisa diakses oleh user dengan role `ADMIN`.

### 4.1 Deactivate User

```
PUT /api/admin/users/{userId}/deactivate
```

**Auth:** Bearer JWT (role ADMIN)
**Response 200:** `"The user account has been successfully deactivated and all sessions have been revoked."`

**Side effects:**
- Semua session user di-revoke
- Semua listing aktif milik user di-cancel (UNSOLD)
- Semua hold dana user di-release

### 4.2 Roles & Permissions

```
POST   /api/admin/roles                                          # Create role
POST   /api/admin/roles/{roleName}/permissions/{permissionName}  # Assign permission
DELETE /api/admin/roles/{roleName}/permissions/{permissionName}  # Revoke permission
```

---

## 5. Wallet Module

Base: `/api/wallet`

> Wallet otomatis dibuat saat user register. Endpoint `/register` hanya untuk kasus khusus admin.

### 5.1 Get Balance

```
GET /api/wallet/balance
```

**Auth:** Bearer JWT
**Response 200:**
```json
{
  "id": "uuid",
  "userId": "uuid",
  "balanceAvailable": 5000000,
  "balanceLocked": 1500000
}
```

- `balanceAvailable`: saldo yang bisa dipakai
- `balanceLocked`: saldo yang sedang ditahan untuk bid aktif

### 5.2 Top Up

```
POST /api/wallet/{userId}/top-up
```

**Auth:** Bearer JWT (harus sama dengan `{userId}`)
**Body:**
```json
{
  "amount": 1000000,
  "method": "BANK",
  "paymentDetails": {
    "bankName": "BCA",
    "accountNumber": "1234567890"
  },
  "idempotencyKey": "uuid-random"
}
```

**Validation:**
- `amount`: > 0, max 100,000,000
- `method`: enum `BANK` | `GOPAY` | `OVO` | `CREDIT_CARD` (saat ini hanya `BANK` yang fully implemented)
- `paymentDetails` (untuk BANK): `bankName` + `accountNumber` wajib
- `idempotencyKey`: UUID/string unik untuk mencegah double-charge saat retry

**Response 200:**
```json
{
  "userId": "uuid",
  "balanceAvailable": 6000000,
  "balanceLocked": 1500000
}
```

### 5.3 Withdraw

```
POST /api/wallet/withdraw
```

**Auth:** Bearer JWT
**Body:** Sama dengan top-up (`amount`, `method`, `paymentDetails`, `idempotencyKey`).

**Error:**
- `400` — Saldo tidak mencukupi (`InsufficientBalanceException`)

### 5.4 Transaction History

```
GET /api/wallet/transactions
```

**Auth:** Bearer JWT
**Response 200:**
```json
[
  {
    "id": "uuid",
    "type": "TOPUP",
    "amount": 1000000,
    "referenceId": "Top-Up via BCA (VA: 1234567890)",
    "createdAt": "2026-05-18T10:00:00"
  },
  {
    "id": "uuid",
    "type": "HOLD",
    "amount": 500000,
    "referenceId": "<listingId>",
    "createdAt": "2026-05-18T11:00:00"
  }
]
```

**TransactionType enum:** `TOPUP` · `HOLD` · `REFUND` · `PAYMENT` · `WITHDRAWAL` · `INCOME`

### 5.5 Transaction Detail

```
GET /api/wallet/transactions/{transactionId}
```

**Auth:** Bearer JWT (hanya pemilik transaksi)
**Response 200:** Satu objek transaksi.

### 5.6 Endpoint Internal (untuk service backend lain)

Dipanggil oleh modul Bidding/Order — tidak dipanggil dari FE:
- `POST /api/wallet/hold` — Hold dana untuk bid
- `POST /api/wallet/release` — Lepas hold (saat outbid)
- `POST /api/wallet/settle` — Konversi hold jadi payment (saat menang lelang)
- `POST /api/wallet/confirm-delivery` — Transfer ke seller (saat order delivered)

---

## 6. Listing Module

Base: `/api/listings`

### 6.1 Create Listing

```
POST /api/listings
```

**Auth:** Bearer JWT (role `SELLER` atau `ADMIN`)
**Body:**
```json
{
  "categoryId": "uuid",
  "title": "Laptop Gaming Asus ROG",
  "description": "Kondisi sangat baik, baterai masih bagus",
  "imageUrl": "https://example.com/laptop.jpg",
  "startingPrice": 5000000,
  "reservePrice": 8000000,
  "endTime": "2026-06-01T18:00:00",
  "auctionType": "ENGLISH"
}
```

**Validation:**
- `title`: tidak boleh blank
- `startingPrice`: minimum 0.01
- `reservePrice`: minimum 0.01 (optional — jika null, lelang otomatis WON saat ada bid)
- `endTime`: harus di masa depan
- `auctionType`: enum (saat ini hanya `ENGLISH`)

**Response 200:**
```json
{
  "id": "uuid",
  "sellerId": "uuid",
  "categoryId": "uuid",
  "title": "Laptop Gaming Asus ROG",
  "description": "...",
  "imageUrl": "...",
  "startingPrice": 5000000,
  "reservePrice": 8000000,
  "endTime": "2026-06-01T18:00:00",
  "status": "ACTIVE",
  "auctionType": "ENGLISH",
  "currentHighestBid": null,
  "currentHighestBidderId": null,
  "version": 0,
  "createdAt": "2026-05-18T10:00:00"
}
```

### 6.2 Get All Listings

```
GET /api/listings?page=0&size=20
```

**Auth:** No
**Query Parameters:**
- `page`: zero-based page index, default `0`
- `size`: page size, default `20`, max `100`

**Response 200:** `PaginatedResponse<Listing>`

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "last": true
}
```

### 6.3 Get Active Listings Only

```
GET /api/listings/active?page=0&size=20
```

**Auth:** No
**Response 200:** `PaginatedResponse<Listing>` dengan status `ACTIVE` atau `EXTENDED` saja.

### 6.4 Search Listings

```
GET /api/listings/search?keyword=...&category=...&minPrice=...&maxPrice=...&page=0&size=20
```

**Auth:** No
**Query Parameters (semua optional):**
- `keyword`: cari di title + description (case-insensitive)
- `category`: UUID category
- `minPrice`, `maxPrice`: BigDecimal
- `page`: zero-based page index, default `0`
- `size`: page size, default `20`, max `100`

**Response 200:** `PaginatedResponse<Listing>`

**Response 200:** `List<Listing>`

### 6.5 Get Listing by ID

```
GET /api/listings/{id}
```

**Auth:** No
**Response 200:** Satu objek listing.
**Response 404:** Listing tidak ditemukan.

### 6.6 Update Listing

```
PUT /api/listings/{id}
```

**Auth:** Bearer JWT
**Body:** Sama struktur dengan create.
**Constraint:** Hanya bisa update saat status BUKAN `ACTIVE` atau `EXTENDED`. Kalau auction sedang berjalan → 400.

### 6.7 Delete Listing

```
DELETE /api/listings/{id}
```

**Auth:** Bearer JWT
**Constraint:** Sama dengan update — hanya saat tidak ada auction aktif.
**Response 204 No Content**

---

## 7. Category Module

Base: `/api/categories`

### 7.1 Get All Categories

```
GET /api/categories
```

**Auth:** No
**Response 200:**
```json
[
  {
    "id": "uuid",
    "name": "Elektronik",
    "parentId": null
  },
  {
    "id": "uuid",
    "name": "Smartphone",
    "parentId": "<parent uuid>"
  }
]
```

### 7.2 Create Category (Admin)

```
POST /api/categories
```

**Auth:** Bearer JWT (ADMIN)
**Body:**
```json
{
  "name": "Elektronik",
  "parentId": null
}
```

---

## 8. Bidding Module

Base: `/api/bids`

### 8.1 Place Bid

```
POST /api/bids
```

**Auth:** Bearer JWT
**Body:**
```json
{
  "listingId": "uuid",
  "amount": 5500000,
  "proxyBid": false,
  "proxyMaxLimit": null
}
```

**Untuk proxy bid (auto-bid)**:
```json
{
  "listingId": "uuid",
  "amount": 5500000,
  "proxyBid": true,
  "proxyMaxLimit": 9000000
}
```

**Validation:**
- `amount` > 0
- `amount` ≥ starting price
- `amount` > current highest bid
- Jika `proxyBid = true`, `proxyMaxLimit` wajib dan ≥ `amount`
- Buyer tidak boleh bid di listing miliknya sendiri
- Auction harus berstatus `ACTIVE` atau `EXTENDED`

**Response 201:**
```json
{
  "id": "uuid",
  "listingId": "uuid",
  "buyerId": "uuid",
  "amount": 5500000,
  "proxyBid": false,
  "proxyMaxLimit": null,
  "createdAt": "2026-05-18T10:00:00"
}
```

**Error:**
| Code | Kondisi |
|---|---|
| 400 | Validasi gagal (di bawah starting price, di bawah current highest, seller bid sendiri, dsb.) |
| 404 | Listing tidak ditemukan |
| 409 | Race condition — bid bentrok, FE retry |
| 422 | Saldo tidak mencukupi |

**Side effects (penting untuk FE):**
1. Dana di-hold di wallet sebesar `amount` (atau `proxyMaxLimit` jika proxy)
2. Bidder sebelumnya (jika ada dan berbeda) akan menerima dana hold-nya kembali → notifikasi `OUTBID`
3. Jika bid masuk dalam 2 menit terakhir → endTime extend 2 menit, status jadi `EXTENDED` → notifikasi `AUCTION_EXTENDED` ke seller
4. Event `BidPlacedEvent` → notifikasi `NEW_BID` ke bidder

### 8.2 Get Bids by Listing

```
GET /api/bids/listing/{listingId}
```

**Auth:** Bearer JWT
**Response 200:** Array `BidResponse`, urut dari terbaru.

### 8.3 Get Highest Bid

```
GET /api/bids/listing/{listingId}/highest
```

**Auth:** Bearer JWT
**Response 200:** Satu `BidResponse`.
**Response 404:** Belum ada bid.

### 8.4 Get Minimum Next Bid

```
GET /api/bids/listing/{listingId}/minimum-bid
```

**Auth:** Bearer JWT
**Response 200:** BigDecimal (number JSON).

> Berguna untuk FE: tampilkan placeholder di input "Min: Rp X" sebelum user bid.

### 8.5 Get My Bids

```
GET /api/bids/me
```

**Auth:** Bearer JWT
**Response 200:** Semua bid yang pernah ditempatkan user (urut terbaru).

### 8.6 Get Bids by Buyer

```
GET /api/bids/buyer/{buyerId}
```

**Auth:** Bearer JWT (hanya bisa lihat milik sendiri — 403 jika berbeda).

---

## 9. Notification Module

Base: `/api/notifications`

### 9.1 Get User Notifications

```
GET /api/notifications/user/{userId}
```

**Auth:** Bearer JWT (hanya milik sendiri kecuali ADMIN)
**Response 200:**
```json
[
  {
    "id": "uuid",
    "userId": "uuid",
    "type": "OUTBID",
    "message": "Anda dikalahkan! Penawaran tertinggi sekarang: Rp 6000000",
    "read": false,
    "createdAt": "2026-05-18T10:00:00",
    "deliveryStatus": "DELIVERED"
  }
]
```

### 9.2 Get Unread Only

```
GET /api/notifications/user/{userId}/unread
```

### 9.3 Mark as Read

```
PATCH /api/notifications/{notificationId}/read
```

### 9.4 Mark All as Read

```
PATCH /api/notifications/user/{userId}/read-all
```

**Response 200:**
```json
{ "message": "Semua notifikasi berhasil ditandai sudah dibaca" }
```

### 9.5 Delete Notification

```
DELETE /api/notifications/{notificationId}
```

### 9.6 Notification Preferences

```
GET /api/notifications/user/{userId}/preferences
PUT /api/notifications/user/{userId}/preferences
```

**Body PUT:**
```json
{
  "emailEnabled": true,
  "pushEnabled": true,
  "inAppEnabled": true,
  "mutedTypes": ["NEW_BID", "BALANCE_TOPUP"]
}
```

### Notification Types (lengkap)

| Type | Trigger |
|---|---|
| `NEW_BID` | User berhasil place bid |
| `OUTBID` | User dikalahkan bidder lain |
| `AUCTION_WON` | User memenangkan lelang |
| `AUCTION_SOLD` | Listing seller terjual |
| `AUCTION_CLOSED_NO_WINNER` | Listing seller tidak terjual |
| `AUCTION_EXTENDED` | Listing seller diperpanjang karena anti-sniping |
| `ORDER_DELIVERED` | Order dikonfirmasi diterima |
| `ORDER_REFUNDED` | Sengketa berakhir dengan refund |
| `BALANCE_TOPUP` | Top up berhasil |
| `BALANCE_HELD` | Saldo dikunci untuk bid |
| `BALANCE_RELEASED` | Hold saldo dilepas |
| `BALANCE_WITHDRAW` | Withdraw berhasil |
| `BALANCE_SETTLED` | Pembayaran selesai (menang lelang) |
| `BALANCE_INCOME` | Income masuk (penjualan) |

---

## 10. Order Module

Base: `/api/orders`

> Order otomatis dibuat saat lelang ditutup dengan pemenang (`AuctionWonEvent` → `OrderEventListener`).

### 10.1 Get My Orders (as Buyer)

```
GET /api/orders/buyer/{buyerId}
```

**Auth:** Bearer JWT (hanya milik sendiri kecuali ADMIN)
**Response 200:**
```json
[
  {
    "id": "uuid",
    "listingId": "uuid",
    "buyerId": "uuid",
    "sellerId": "uuid",
    "amount": 7500000,
    "status": "CREATED",
    "trackingNumber": null,
    "disputeReason": null,
    "createdAt": "2026-05-18T10:00:00"
  }
]
```

### 10.2 Update Tracking Number (Seller)

```
PATCH /api/orders/{orderId}/tracking
```

**Auth:** Bearer JWT (hanya seller order tersebut)
**Body:**
```json
{ "trackingNumber": "JNE-1234567890" }
```

**Side effect:** Status order otomatis menjadi `SHIPPED`.

### 10.3 Confirm Delivery (Buyer)

```
PATCH /api/orders/{orderId}/confirm
```

**Auth:** Bearer JWT (hanya buyer order tersebut)
**Side effects:**
- Status → `DELIVERED`
- Dana hold buyer di-settle, lalu masuk wallet seller
- Notifikasi `ORDER_DELIVERED` ke seller

### 10.4 Dispute Order (Buyer)

```
PATCH /api/orders/{orderId}/dispute
```

**Auth:** Bearer JWT (hanya buyer)
**Body:**
```json
{ "reason": "Barang tidak sesuai deskripsi" }
```

### 10.5 Resolve Dispute (Admin)

```
PATCH /api/orders/{orderId}/resolve-dispute
```

**Auth:** Bearer JWT (ADMIN)
**Body:**
```json
{ "refundBuyer": true }
```

- `true` → refund ke buyer, order CANCELLED
- `false` → bayar ke seller, order DELIVERED

### 10.6 Update Order Status (Manual / Admin)

```
PATCH /api/orders/{orderId}/status
```

**Body:**
```json
{ "status": "SHIPPED" }
```

### Order Status Lifecycle

```
DRAFT → CREATED → SHIPPED → DELIVERED
             │        │         │
             └──CANCELLED       └─→ DISPUTED → DELIVERED
                                         └──→ CANCELLED (refund)
```

**Valid transitions:**

| Current → Next | Allowed |
|---|---|
| DRAFT → CREATED | ✅ |
| DRAFT → CANCELLED | ✅ |
| CREATED → SHIPPED | ✅ |
| CREATED → CANCELLED | ✅ |
| SHIPPED → DELIVERED | ✅ |
| SHIPPED → DISPUTED | ✅ |
| DELIVERED → DISPUTED | ✅ |
| DISPUTED → DELIVERED | ✅ |
| DISPUTED → CANCELLED | ✅ |
| CANCELLED → anything | ❌ |

---

## 11. WebSocket Real-Time Notifications

**Endpoint:** `ws://localhost:8080/ws-notifications` (SockJS + STOMP)
**Subscription topic:** `/user/queue/notifications`

**Contoh client (JavaScript):**
```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws-notifications'),
  connectHeaders: {
    Authorization: `Bearer ${accessToken}`,
  },
  onConnect: () => {
    client.subscribe(`/user/${userId}/queue/notifications`, (msg) => {
      const notif = JSON.parse(msg.body);
      console.log('Real-time notif:', notif);
      // Update UI badge, toast, etc.
    });
  },
});

client.activate();
```

> Payload yang diterima sama dengan response `GET /api/notifications/user/{userId}` (single object).

---

## Error Response Format

### Format umum (sebagian besar modul)
```json
{
  "code": "BAD_REQUEST",
  "message": "Bid harus lebih tinggi dari bid tertinggi saat ini: 5000000",
  "timestamp": "2026-05-18T10:00:00"
}
```

### Format wallet
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Saldo tidak mencukupi.",
  "timestamp": "2026-05-18T10:00:00"
}
```

### Format user module
```json
{
  "status": 400,
  "message": "Username is already taken.",
  "timestamp": "2026-05-18T10:00:00Z"
}
```

> ⚠️ Format error TIDAK konsisten antar modul. FE perlu handle keduanya (cek `message` field — selalu ada).

### Error Code Reference (Bidding)
| Code | HTTP | Meaning |
|---|---|---|
| `BAD_REQUEST` | 400 | Validasi bid gagal |
| `NOT_FOUND` | 404 | Listing / bid tidak ditemukan |
| `FORBIDDEN` | 403 | Bukan owner resource |
| `CONFLICT` | 409 | Race condition (retry recommended) |
| `INSUFFICIENT_BALANCE` | 422 | Saldo wallet kurang |
| `INTERNAL_ERROR` | 500 | Server error |

---

## Domain Models / Enums

### AuctionStatus
```
DRAFT     — Belum dipublish
ACTIVE    — Lelang berjalan, menerima bid
EXTENDED  — Diperpanjang karena anti-sniping
CLOSED    — Ditutup manual
WON       — Selesai dengan pemenang (reserve price terpenuhi)
UNSOLD    — Selesai tanpa pemenang
```

### AuctionType
```
ENGLISH — Bid harus lebih tinggi dari highest current
```
(Strategy pattern siap untuk type lain di masa depan.)

### OrderStatus
```
DRAFT, CREATED, SHIPPED, DELIVERED, DISPUTED, CANCELLED
```

### TransactionType
```
TOPUP, HOLD, REFUND, PAYMENT, WITHDRAWAL, INCOME
```

### PaymentMethod
```
BANK, GOPAY, OVO, CREDIT_CARD
```
(Saat ini hanya `BANK` yang fully implemented.)

### User Roles
```
USER, SELLER, ADMIN, INTERNAL_SERVICE
```

---

## Recommended FE Flow

### A. Onboarding Flow
1. `POST /api/auth/register` → daftar
2. `POST /api/auth/login` → simpan `accessToken` + `refreshToken`
3. `GET /api/users/me` → simpan profile (userId, role, dsb.)
4. (Optional) `POST /api/wallet/{userId}/top-up` → user isi saldo

### B. Browse → Bid Flow
1. `GET /api/listings/active` → list halaman utama
2. `GET /api/categories` → sidebar filter
3. `GET /api/listings/search?keyword=...&minPrice=...` → search/filter
4. `GET /api/listings/{id}` → detail
5. `GET /api/bids/listing/{listingId}` → riwayat bid
6. `GET /api/bids/listing/{listingId}/minimum-bid` → minimum bid berikutnya
7. Connect WebSocket → subscribe real-time updates
8. `POST /api/bids` → submit bid

### C. Seller Flow
1. `POST /api/listings` → buat lelang
2. `GET /api/notifications/user/{userId}/unread` → cek bid masuk
3. Setelah auction selesai & buyer order:
4. `PATCH /api/orders/{orderId}/tracking` → input resi (status → SHIPPED)
5. Tunggu buyer confirm → dana cair ke wallet

### D. Buyer Pasca Menang Flow
1. Notifikasi `AUCTION_WON` masuk
2. `GET /api/orders/buyer/{userId}` → cek order baru (otomatis dibuat)
3. Tunggu seller input resi
4. `PATCH /api/orders/{orderId}/confirm` → konfirmasi terima (dana ke seller)
5. Atau `PATCH /api/orders/{orderId}/dispute` jika bermasalah

### E. Token Refresh Pattern (FE Interceptor)
```javascript
// Pseudocode
axios.interceptors.response.use(
  (res) => res,
  async (error) => {
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true;
      const { accessToken, refreshToken } = await refreshAuth();
      saveTokens(accessToken, refreshToken);
      error.config.headers.Authorization = `Bearer ${accessToken}`;
      return axios(error.config);
    }
    return Promise.reject(error);
  }
);
```

---

## Catatan untuk FE Team

### ⚠️ Hal yang perlu diperhatikan

1. **CORS:** Backend mengizinkan semua origin (`*`) untuk development. Production perlu di-whitelist.
2. **Race condition bid:** Backend pakai optimistic locking + retry. Jika tetap dapat 409, retry dari FE OK setelah delay singkat.
3. **Anti-sniping:** Bid dalam 2 menit terakhir → `endTime` mundur 2 menit. FE harus poll/listen untuk update `endTime` real-time.
4. **Wallet auto-create:** Tidak perlu call `/api/wallet/register` — wallet otomatis dibuat saat register via event listener.
5. **Idempotency:** Untuk `top-up` dan `withdraw`, selalu kirim `idempotencyKey` unik (UUID v4) untuk mencegah double-charge saat user double-click submit atau retry.
6. **Format error tidak konsisten:** Selalu baca field `message` (ada di semua format). Status code di header lebih reliable daripada field `code`/`status` di body.
7. **WebSocket auth:** Token JWT dikirim di `connectHeaders`. Kalau token expired, koneksi disconnect — FE perlu refresh + reconnect.
8. **Field naming case:**
   - JSON response field menggunakan camelCase
   - `isRead` di Notification serialized sebagai `read` (Lombok `@Getter` boolean rule)
   - `isEmailVerified` di User serialized sebagai `emailVerified` / `isEmailVerified` (lihat response untuk konfirmasi)

### 💡 Tips development

- Gunakan akun terpisah untuk seller & buyer saat testing flow lengkap
- Untuk test anti-sniping: buat listing dengan `endTime` 1-2 menit dari sekarang
- Untuk test outbid: login Buyer A, bid → login Buyer B, bid lebih tinggi → cek notif Buyer A
- Untuk test auto-close: tunggu scheduler 30 detik setelah `endTime` lewat

---

**End of API Contract v2.0**
