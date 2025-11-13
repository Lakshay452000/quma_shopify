package com.quma.quma_shopify_backend.models.mongo;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

@Data
public class Variant {

    @NotBlank
    @Field(type = FieldType.Keyword)
    private String identifier;

    @Field(type = FieldType.Keyword)
    private String color;

    @Field(type = FieldType.Keyword)
    private String size;

    @Field(type = FieldType.Double)
    private Double weight;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Double)
    private BigDecimal discountedPrice;

    @Field(type = FieldType.Integer)
    private Integer stock;

    @Field(type = FieldType.Keyword)
    private List<String> materials;

    @Field(type = FieldType.Keyword)
    private List<String> images;

    private Integer buyers = 0;
}
