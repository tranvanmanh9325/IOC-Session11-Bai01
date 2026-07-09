package com.example.pharmacy.service;

import com.example.pharmacy.model.Medicine;
import com.example.pharmacy.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;

    /**
     * Cache-aside pattern:
     * - Lần 1: Cache MISS → Spring thực thi phương thức, truy vấn DB (log SQL xuất hiện),
     *          sau đó lưu kết quả vào Redis với key "medicines::{id}".
     * - Lần 2: Cache HIT → Spring trả kết quả thẳng từ Redis,
     *          KHÔNG thực thi phương thức → KHÔNG có log SQL.
     */
    @Cacheable(value = "medicines", key = "#id")
    public Medicine getMedicineById(Long id) {
        log.info(">>> [DB HIT] Cache MISS - Fetching medicine id={} from PostgreSQL...", id);
        return medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicine not found with id: " + id));
    }

    public List<Medicine> getAllMedicines() {
        return medicineRepository.findAll();
    }

    public Medicine createMedicine(Medicine medicine) {
        return medicineRepository.save(medicine);
    }

    // Cập nhật DB và đồng thời refresh lại cache entry cho id này
    @CachePut(value = "medicines", key = "#id")
    public Medicine updateMedicine(Long id, Medicine updated) {
        Medicine existing = medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicine not found with id: " + id));
        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setPrice(updated.getPrice());
        existing.setDescription(updated.getDescription());
        existing.setManufacturer(updated.getManufacturer());
        log.info(">>> [CACHE PUT] Refreshing cache for medicine id={}", id);
        return medicineRepository.save(existing);
    }

    // Xóa khỏi DB và evict cache tương ứng để tránh stale data
    @CacheEvict(value = "medicines", key = "#id")
    public void deleteMedicine(Long id) {
        log.info(">>> [CACHE EVICT] Removing medicine id={} from cache", id);
        medicineRepository.deleteById(id);
    }
}
