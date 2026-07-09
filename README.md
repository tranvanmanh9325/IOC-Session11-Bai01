# [Bài tập] Caching danh mục thuốc (Redis Cache)

Spring Boot REST API quản lý danh mục thuốc tích hợp **Redis Cache**
theo **Cache-aside pattern** bằng annotation `@Cacheable`.

## Yêu cầu cài đặt

| Công cụ | Phiên bản tối thiểu |
| ------- | ------------------- |
| Java    | 17+                 |
| Maven   | 3.8+                |
| Docker  | 20+                 |

---

## Cách chạy

### Bước 1 — Khởi động PostgreSQL & Redis

```bash
docker-compose up -d
```

Lệnh này tạo:

- `pharmacy-postgres` tại `localhost:5432` (DB: `pharmacy_db`)
- `pharmacy-redis` tại `localhost:6379`

### Bước 2 — Chạy ứng dụng

```bash
mvn spring-boot:run
```

Ứng dụng tự động tạo bảng `medicines` và insert 10 bản ghi mẫu.

---

## Kiểm chứng Cache

### Lần 1 — Cache MISS (truy vấn từ PostgreSQL)

```bash
curl http://localhost:8080/api/medicines/1
```

**Log console xuất hiện SQL:**

```sql
>>> [DB HIT] Cache MISS - Fetching medicine id=1 from PostgreSQL...
Hibernate:
    select m1_0.id, m1_0.category, ...
    from medicines m1_0
    where m1_0.id=?
```

### Lần 2 — Cache HIT (lấy từ Redis, không có SQL)

```bash
curl http://localhost:8080/api/medicines/1
```

**Log console KHÔNG có SQL** → response trong vài ms.

### Kiểm tra Redis trực tiếp

```bash
docker exec -it pharmacy-redis redis-cli
> KEYS medicines::*
> GET "medicines::1"
```

---

## API Endpoints

| Method   | URL                   | Mo ta                           | Cache         |
| -------- | --------------------- | ------------------------------- | ------------- |
| GET      | /api/medicines        | Lay tat ca thuoc                | -             |
| GET      | /api/medicines/{id}   | Lay chi tiet thuoc (co cache)   | @Cacheable    |
| POST     | /api/medicines        | Them thuoc moi                  | -             |
| PUT      | /api/medicines/{id}   | Cap nhat thuoc (refresh cache)  | @CachePut     |
| DELETE   | /api/medicines/{id}   | Xoa thuoc (evict cache)         | @CacheEvict   |

### Ví dụ tạo thuốc mới

```json
{
  "name": "Aspirin 100mg",
  "category": "Tim mach",
  "price": 6000,
  "description": "Chong dong mau, phong ngua nhoi mau co tim",
  "manufacturer": "Bayer"
}
```

---

## Cấu trúc dự án

```text
src/main/java/com/example/pharmacy/
├── PharmacyApplication.java
├── config/
│   └── RedisConfig.java
├── controller/
│   └── MedicineController.java
├── service/
│   └── MedicineService.java
├── repository/
│   └── MedicineRepository.java
└── model/
    └── Medicine.java
```

---

## Cache Strategy

```text
Client -> GET /api/medicines/1
           |
           v
    Spring Cache Manager
           |
    +------+------+
    |  Redis Hit? |
    +------+------+
        YES|             NO
           |        +----+----+
           |        |  Query  |
           |        |   DB    |
           |        +----+----+
           |             | Save to Redis
           +------+------+
                  |
                  v
           Return Medicine
```

| Annotation    | Khi nao dung      | Hanh vi                           |
| ------------- | ----------------- | --------------------------------- |
| @Cacheable    | getMedicineById   | Doc cache truoc, neu miss hit DB  |
| @CachePut     | updateMedicine    | Luon hit DB va update cache       |
| @CacheEvict   | deleteMedicine    | Xoa entry khoi cache              |