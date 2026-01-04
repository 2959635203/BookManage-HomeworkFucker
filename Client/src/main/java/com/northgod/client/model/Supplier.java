package com.northgod.client.model;

import lombok.Data;

@Data
public class Supplier {
    private Long id;
    private String name;
    private String contactPhone;
    private String email;
    private String address;
    private String contactPerson;
    private String creditRating;
    private String paymentTerms;
    private Boolean isActive;
    private String notes;

    @Override
    public String toString() {
        return name != null ? name : "未知供应商";
    }
}
