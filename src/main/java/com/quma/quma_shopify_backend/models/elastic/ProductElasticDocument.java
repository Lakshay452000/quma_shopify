package com.quma.quma_shopify_backend.models.elastic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "products")
public class ProductElasticDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String productId;

    @Field(type = FieldType.Keyword)
    private String identifier;

    // --- Product Info ---
    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private List<String> description;

    @Field(type = FieldType.Keyword)
    private String brand;

    @Field(type = FieldType.Keyword)
    private List<String> categories;

    @Field(type = FieldType.Keyword)
    private List<String> types;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Keyword)
    private List<String> images;

    // --- Variant & Filter Info ---
    @Field(type = FieldType.Keyword)
    private String color;

    @Field(type = FieldType.Keyword)
    private String size;

    @Field(type = FieldType.Double)
    private Double weight;

    @Field(type = FieldType.Keyword)
    private List<String> materials;

    // --- Display Info ---
    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Double)
    private BigDecimal discountedPrice;

    @Field(type = FieldType.Double)
    private Double averageRating;

    @Field(type = FieldType.Long)
    private Long totalBuyers;

    @Field(type = FieldType.Boolean)
    private Boolean isActive;

    // --- Meta Info ---
    @Field(type = FieldType.Date)
    private Instant createdAt;

    @Field(type = FieldType.Date)
    private Instant updatedAt;

    // --- Sorting Support ---
    @Transient
    private Object[] sortValues;
}
