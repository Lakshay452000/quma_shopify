package com.quma.quma_shopify_backend.enums;

public enum SortType {
    ASC("asc"),
    DESC("desc");

    private final String value;

    SortType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SortType fromValue(String value) {
        for (SortType type : SortType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown sort type: " + value);
    }
}
