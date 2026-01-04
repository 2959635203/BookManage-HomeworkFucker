package com.northgod.server.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TransactionType {
    PURCHASE("进货"),
    SALE("销售"),
    RETURN("退货");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return this.name();
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static TransactionType fromValue(String value) {
        for (TransactionType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("无效的交易类型: " + value);
    }

    public static boolean isValid(String value) {
        for (TransactionType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}