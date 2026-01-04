package com.northgod.server.repository;

import com.northgod.server.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByIsActiveTrue();

    Page<Supplier> findByIsActiveTrue(Pageable pageable);

    Optional<Supplier> findByIdAndIsActiveTrue(Long id);

    @Query("SELECT s FROM Supplier s WHERE s.isActive = true AND " +
            "(LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "s.contactPhone LIKE CONCAT('%', :keyword, '%'))")
    Page<Supplier> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Supplier s WHERE s.isActive = true")
    long countActiveSuppliers();
    
    @Query("SELECT s FROM Supplier s WHERE s.isActive = false")
    Page<Supplier> findByIsActiveFalse(Pageable pageable);
}



