package com.northgod.server.controller;

import com.northgod.server.entity.Supplier;
import com.northgod.server.service.SupplierService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/suppliers")
@Validated
public class SupplierController {

    private static final Logger logger = LoggerFactory.getLogger(SupplierController.class);
    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllSuppliers(
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(value = "sortBy", defaultValue = "name") String sortBy,
            @RequestParam(value = "direction", defaultValue = "ASC") String direction) {
        try {
            logger.debug("获取供应商列表，页码: {}, 大小: {}", page, size);
            Page<Supplier> supplierPage = supplierService.getAllSuppliers(page, size, sortBy, direction);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", supplierPage.getContent());
            response.put("pagination", Map.of(
                    "currentPage", supplierPage.getNumber(),
                    "pageSize", supplierPage.getSize(),
                    "totalItems", supplierPage.getTotalElements(),
                    "totalPages", supplierPage.getTotalPages()
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取供应商列表失败", e);
            return createErrorResponse("获取供应商列表失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveSuppliers() {
        try {
            logger.debug("获取活跃供应商列表");
            List<Supplier> suppliers = supplierService.getAllActiveSuppliers();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", suppliers);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取活跃供应商列表失败", e);
            return createErrorResponse("获取活跃供应商列表失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getSupplierById(@PathVariable("id") Long id) {
        try {
            logger.debug("获取供应商详情，ID: {}", id);
            return supplierService.getSupplierById(id)
                    .map(supplier -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("data", supplier);
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("success", false);
                        errorResponse.put("message", "供应商不存在");
                        errorResponse.put("code", "SUPPLIER_NOT_FOUND");
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
                    });
        } catch (Exception e) {
            logger.error("获取供应商详情失败，ID: {}", id, e);
            return createErrorResponse("获取供应商详情失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchSuppliers(
            @RequestParam(value = "keyword", required = true) String keyword,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        try {
            logger.debug("搜索供应商，关键词: {}", keyword);
            Page<Supplier> supplierPage = supplierService.searchSuppliers(keyword, page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", supplierPage.getContent());
            response.put("pagination", Map.of(
                    "currentPage", supplierPage.getNumber(),
                    "pageSize", supplierPage.getSize(),
                    "totalItems", supplierPage.getTotalElements(),
                    "totalPages", supplierPage.getTotalPages()
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("搜索供应商失败，关键词: {}", keyword, e);
            return createErrorResponse("搜索供应商失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSupplier(@Valid @RequestBody Supplier supplier) {
        try {
            logger.debug("创建供应商: {}", supplier.getName());
            Supplier created = supplierService.createSupplier(supplier);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "供应商创建成功");
            response.put("data", created);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("创建供应商失败", e);
            return createErrorResponse("创建供应商失败: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateSupplier(
            @PathVariable("id") Long id,
            @Valid @RequestBody Supplier supplier) {
        try {
            logger.debug("更新供应商，ID: {}", id);
            Supplier updated = supplierService.updateSupplier(id, supplier);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "供应商更新成功");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新供应商失败，ID: {}", id, e);
            return createErrorResponse("更新供应商失败: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteSupplier(@PathVariable("id") Long id) {
        try {
            logger.debug("删除供应商，ID: {}", id);
            supplierService.deleteSupplier(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "供应商删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除供应商失败，ID: {}", id, e);
            return createErrorResponse("删除供应商失败: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * 获取回收站中的供应商（已删除的供应商）
     */
    @GetMapping("/deleted")
    public ResponseEntity<Map<String, Object>> getDeletedSuppliers(
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(value = "sortBy", defaultValue = "updatedAt") String sortBy,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction) {
        try {
            logger.debug("获取回收站供应商列表，页码: {}, 大小: {}", page, size);
            Page<Supplier> supplierPage = supplierService.getDeletedSuppliers(page, size, sortBy, direction);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", supplierPage.getContent());
            response.put("pagination", Map.of(
                    "currentPage", supplierPage.getNumber(),
                    "pageSize", supplierPage.getSize(),
                    "totalItems", supplierPage.getTotalElements(),
                    "totalPages", supplierPage.getTotalPages(),
                    "first", supplierPage.isFirst(),
                    "last", supplierPage.isLast()
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取回收站供应商列表失败", e);
            return createErrorResponse("获取回收站供应商列表失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 恢复已删除的供应商
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<Map<String, Object>> restoreSupplier(@PathVariable("id") Long id) {
        try {
            logger.debug("恢复供应商，ID: {}", id);
            supplierService.restoreSupplier(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "供应商恢复成功");
            response.put("supplierId", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("恢复供应商失败，ID: {}", id, e);
            return createErrorResponse("恢复供应商失败: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("status", status.value());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(status).body(response);
    }
}



