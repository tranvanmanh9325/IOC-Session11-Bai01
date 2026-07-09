package com.example.pharmacy.service;

import com.example.pharmacy.model.Medicine;
import com.example.pharmacy.repository.MedicineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicineServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @InjectMocks
    private MedicineService medicineService;

    private Medicine medicine;

    @BeforeEach
    void setUp() {
        medicine = Medicine.builder()
                .id(1L)
                .name("Paracetamol 500mg")
                .category("Giảm đau - Hạ sốt")
                .price(new BigDecimal("5000.00"))
                .description("Thuốc giảm đau hạ sốt thông dụng")
                .manufacturer("Dược Hậu Giang")
                .build();
    }

    @Test
    @DisplayName("getMedicineById - should return medicine when found in DB")
    void getMedicineById_ShouldReturnMedicine_WhenFound() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicine));

        Medicine result = medicineService.getMedicineById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Paracetamol 500mg");
        assertThat(result.getPrice()).isEqualByComparingTo("5000.00");
        verify(medicineRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getMedicineById - should throw RuntimeException when not found")
    void getMedicineById_ShouldThrow_WhenNotFound() {
        when(medicineRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicineService.getMedicineById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Medicine not found with id: 99");

        verify(medicineRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("createMedicine - should save and return medicine")
    void createMedicine_ShouldSaveAndReturn() {
        when(medicineRepository.save(any(Medicine.class))).thenReturn(medicine);

        Medicine result = medicineService.createMedicine(medicine);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(medicineRepository, times(1)).save(any(Medicine.class));
    }

    @Test
    @DisplayName("getAllMedicines - should return list of medicines")
    void getAllMedicines_ShouldReturnList() {
        when(medicineRepository.findAll()).thenReturn(List.of(medicine));

        List<Medicine> result = medicineService.getAllMedicines();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Paracetamol 500mg");
    }

    @Test
    @DisplayName("deleteMedicine - should call deleteById on repository")
    void deleteMedicine_ShouldCallDeleteById() {
        doNothing().when(medicineRepository).deleteById(1L);

        medicineService.deleteMedicine(1L);

        verify(medicineRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("updateMedicine - should update fields and save")
    void updateMedicine_ShouldUpdateAndSave() {
        Medicine updated = Medicine.builder()
                .name("Paracetamol 650mg")
                .category("Giảm đau - Hạ sốt")
                .price(new BigDecimal("7000.00"))
                .description("Liều mạnh hơn")
                .manufacturer("Dược Hậu Giang")
                .build();

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicine));
        when(medicineRepository.save(any(Medicine.class))).thenReturn(medicine);

        Medicine result = medicineService.updateMedicine(1L, updated);

        assertThat(result).isNotNull();
        verify(medicineRepository, times(1)).findById(1L);
        verify(medicineRepository, times(1)).save(any(Medicine.class));
    }
}
