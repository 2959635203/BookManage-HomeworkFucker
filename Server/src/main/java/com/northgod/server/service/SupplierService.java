package com.northgod.server.service;

import com.northgod.server.entity.Supplier;
import com.northgod.server.exception.BusinessException;
import com.northgod.server.repository.SupplierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class SupplierService {

    private static final Logger logger = LoggerFactory.getLogger(SupplierService.class);
    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public List<Supplier> getAllActiveSuppliers() {
        return supplierRepository.findByIsActiveTrue();
    }

    public Page<Supplier> getAllSuppliers(int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        // 只返回活跃的供应商（isActive = true）
        return supplierRepository.findByIsActiveTrue(pageable);
    }

    public Page<Supplier> searchSuppliers(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return supplierRepository.searchByKeyword(keyword, pageable);
    }

    public Optional<Supplier> getSupplierById(Long id) {
        return supplierRepository.findByIdAndIsActiveTrue(id);
    }

    @Transactional
    @CacheEvict(value = "suppliers", allEntries = true)
    public Supplier createSupplier(Supplier supplier) {
        validateSupplier(supplier);
        supplier.setIsActive(true);
        Supplier saved = supplierRepository.save(supplier);
        logger.info("创建供应商: {}", saved.getName());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "suppliers", allEntries = true)
    public Supplier updateSupplier(Long id, Supplier supplier) {
        Supplier existing = supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException("SUPPLIER_NOT_FOUND", "供应商不存在"));

        validateSupplier(supplier);
        existing.setName(supplier.getName());
        existing.setContactPhone(supplier.getContactPhone());
        existing.setEmail(supplier.getEmail());
        existing.setAddress(supplier.getAddress());
        existing.setContactPerson(supplier.getContactPerson());
        existing.setCreditRating(supplier.getCreditRating());
        existing.setPaymentTerms(supplier.getPaymentTerms());
        existing.setNotes(supplier.getNotes());

        Supplier updated = supplierRepository.save(existing);
        logger.info("更新供应商: {}", updated.getName());
        return updated;
    }

    @Transactional
    @CacheEvict(value = "suppliers", allEntries = true)
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException("SUPPLIER_NOT_FOUND", "供应商不存在"));
        
        // 软删除，只设置isActive为false
        supplier.setIsActive(false);
        supplierRepository.save(supplier);
        logger.info("删除供应商: {}", supplier.getName());
    }
    
    /**
     * 获取已删除的供应商列表（回收站）
     */
    public Page<Supplier> getDeletedSuppliers(int page, int size, String sortBy, String direction) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return supplierRepository.findByIsActiveFalse(pageable);
    }
    
    /**
     * 恢复已删除的供应商
     */
    @Transactional
    @CacheEvict(value = "suppliers", allEntries = true)
    public void restoreSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException("SUPPLIER_NOT_FOUND", "供应商不存在"));
        
        if (supplier.getIsActive()) {
            throw new BusinessException("SUPPLIER_ALREADY_ACTIVE", "供应商已经是激活状态，无需恢复");
        }
        
        supplier.setIsActive(true);
        supplierRepository.save(supplier);
        
        logger.info("恢复供应商: {}", supplier.getName());
    }

    private void validateSupplier(Supplier supplier) {
        if (supplier.getName() == null || supplier.getName().trim().isEmpty()) {
            throw new BusinessException("INVALID_SUPPLIER", "供应商名称不能为空");
        }
    }

    public long getActiveSupplierCount() {
        return supplierRepository.countActiveSuppliers();
    }
}



