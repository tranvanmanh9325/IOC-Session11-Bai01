# [Bài tập] Caching danh mục thuốc (Redis Cache)

Spring Boot REST API quản lý danh mục thuốc tích hợp **Redis Cache** theo **Cache-aside pattern** bằng annotation `@Cacheable`.

## Yêu cầu cài đặt

| Công cụ | Phiên bản tối thiểu |
|---------|-------------------|
| Java    | 17+               |
| Maven   | 3.8+              |
| Docker  | 20+               |

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

| Method   | URL                    | Mô tả                              | Cache              |
|----------|------------------------|------------------------------------|--------------------|
| `GET`    | `/api/medicines`       | Lấy tất cả thuốc                   | —                  |
| `GET`    | `/api/medicines/{id}`  | Lấy chi tiết thuốc theo ID          | `@Cacheable`       |
| `POST`   | `/api/medicines`       | Thêm thuốc mới                      | —                  |
| `PUT`    | `/api/medicines/{id}`  | Cập nhật thuốc (refresh cache)      | `@CachePut`        |
| `DELETE` | `/api/medicines/{id}`  | Xóa thuốc (evict cache)             | `@CacheEvict`      |

### Ví dụ tạo thuốc mới

```json
POST /api/medicines
{
  "name": "Aspirin 100mg",
  "category": "Tim mạch",
  "price": 6000,
  "description": "Chống đông máu, phòng ngừa nhồi máu cơ tim",
  "manufacturer": "Bayer"
}
```

---

## Cấu trúc dự án

```
src/main/java/com/example/pharmacy/
├── PharmacyApplication.java       # Entry point
├── config/
│   └── RedisConfig.java           # Cấu hình RedisCacheManager (JSON, TTL 10 phút)
├── controller/
│   └── MedicineController.java    # REST endpoints
├── service/
│   └── MedicineService.java       # Business logic + @Cacheable
├── repository/
│   └── MedicineRepository.java    # JPA Repository
└── model/
    └── Medicine.java              # JPA Entity (implements Serializable)
```

---

## Cache Strategy

```
Client → GET /api/medicines/1
           │
           ▼
    Spring Cache Manager
           │
    ┌──────┴──────┐
    │  Redis Hit? │
    └──────┬──────┘
        YES│                    NO
           │              ┌────┴────┐
           │              │  Query  │
           │              │   DB    │
           │              └────┬────┘
           │                   │ Save to Redis
           └──────────┬────────┘
                      ▼
               Return Medicine
```

| Annotation      | Khi nào dùng              | Hành vi                            |
|-----------------|---------------------------|------------------------------------|
| `@Cacheable`    | `getMedicineById`         | Đọc cache trước, nếu miss thì hit DB |
| `@CachePut`     | `updateMedicine`          | Luôn hit DB và update cache          |
| `@CacheEvict`   | `deleteMedicine`          | Xóa entry khỏi cache                 |