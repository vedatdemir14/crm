# CRM & İK Platformu

[![CI](https://github.com/vedatdemir14/crm/actions/workflows/ci.yml/badge.svg)](https://github.com/vedatdemir14/crm/actions/workflows/ci.yml)

Kurumsal CRM ve İK platformunun backend'i. Modüler monolit mimari, Spring Boot 4 / Java 21 / PostgreSQL.

Tasarım dokümanları (SRS, mimari, veri modeli, API tasarımı, kriptografi standartları, yedekleme stratejisi)
ayrı bir doküman setinde tutulmaktadır.

## Gereksinimler

- JDK 21
- Docker (PostgreSQL, Redis, RabbitMQ için)

Maven kurulu olmasına gerek yoktur — repoda Maven Wrapper (`mvnw`) bulunur.

## Çalıştırma

Altyapı servislerini başlat:

```bash
docker compose up -d
```

Uygulamayı başlat:

```bash
./mvnw spring-boot:run
```

Uygulama `http://localhost:8080` üzerinde çalışır. Swagger arayüzü: `http://localhost:8080/swagger-ui.html`

### İlk giriş

`dev` profilinde, veritabanı boşsa bir `admin` kullanıcısı otomatik oluşturulur. Şifre `ADMIN_BOOTSTRAP_PASSWORD`
ortam değişkeninden alınır; tanımlı değilse rastgele üretilip **başlangıç loglarına bir kez yazılır**.

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin","password":"<log-daki-sifre>"}'
```

## Proje Yapısı

Mimari dokümanındaki paket yapısı izlenir:

```
com.sirket.platform
├── common
│   ├── identity      Kullanıcı, rol, kimlik doğrulama
│   ├── security      JWT, şifre hashleme, Spring Security yapılandırması
│   └── error         Ortak hata formatı ve exception handler
└── config            OpenAPI, dev bootstrap
```

CRM ve İK modülleri (`crm`, `hr` paketleri ve şemaları) henüz eklenmedi — veritabanı şemaları
migration'da oluşturuldu, tablolar ilgili modüller geliştirildikçe eklenecek.

## Güvenlik Notları

- Şifreler **Argon2id** ile hashlenir (16 byte salt, 32 byte hash, 19 MiB bellek, 2 iterasyon).
- Access token'lar **RS256** ile imzalanır. Anahtar çifti `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY`
  ortam değişkenlerinden (PEM) okunur. Tanımlı değilse **geçici bir anahtar çifti üretilir** —
  yalnızca geliştirme içindir, uygulama yeniden başlayınca tüm token'lar geçersiz olur.
- Refresh token'lar veritabanında **hash'lenmiş** olarak saklanır, her yenilemede **rotasyona** girer.
  Kullanılmış bir token tekrar sunulursa, sızıntı varsayılarak o kullanıcının tüm oturumları iptal edilir.
- Hiçbir anahtar veya şifre repoya commit edilmez.

## Testler

```bash
./mvnw test
```
