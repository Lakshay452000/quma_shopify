package com.quma.quma_shopify_backend.models.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "addresses")
public class Address {

    @Id
    private String id;
    private String addressId;
    private String name;
    private Long phoneNumber;
    private String username;
    private String house;
    private String area;
    private String city;
    private String state;
    @NotBlank
    private String pincode;
    private boolean isDefault;
}
