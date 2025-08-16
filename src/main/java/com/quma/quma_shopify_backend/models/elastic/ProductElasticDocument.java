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
    private String id; // Same as Mongo _id

    @Field(type = FieldType.Keyword)
    private String ownerId; // For access control (creator)

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Integer)
    private String categoryId; // Category ID for filtering

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Double)
    private BigDecimal discountedPrice;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Keyword)
    private String imageUrl; // One main product image

    // Review summary
    @Field(type = FieldType.Double)
    private Double averageRating;

    @Field(type = FieldType.Integer)
    private Integer totalReviews;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    @Field(type = FieldType.Date)
    private Instant updatedAt;
    @Transient
    private Object[] sortValues;

}

