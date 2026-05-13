PROJECT: BidMart — Platform Lelang Real-time
REPO: https://github.com/advprog-2026-B11-project/bidmart-backend
BRANCH UTAMA: staging (semua merge ke sini)
BRANCH MASING-MASING: feature/nama-modul

MODUL: autentikasi & manajemen pengguna
lokasi: src/main/java/com/example/bidmart/user

TECH STACK:
- Java 21, Spring Boot 4.x, Gradle
- PostgreSQL (database)
- Lombok (boilerplate reduction)
- Spring Security + JWT (autentikasi)
- Spring Events (komunikasi async antar modul)
- Spring Retry (retry mechanism)

ARSITEKTUR: Modular Monolith
- Semua modul dalam SATU Spring Boot application
- Setiap modul punya package sendiri: com.example.bidmart.[modul]
- Komunikasi SYNC: direct method call antar service (masih satu JVM)
- Komunikasi ASYNC: Spring ApplicationEventPublisher + @TransactionalEventListener

PRINSIP YANG WAJIB DIIKUTI:
- Constructor injection WAJIB (tidak boleh @Autowired field injection)
- Semua event cross-module HARUS di package com.example.bidmart.common.event
- SOLID principles: terutama SRP dan OCP
- Clean code: method kecil, nama deskriptif, tidak ada magic string