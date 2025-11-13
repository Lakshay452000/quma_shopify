package com.quma.quma_shopify_backend.models.dtos;

import com.quma.quma_shopify_backend.models.mongo.Variant;

import lombok.Data;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Data
public class ProductSaveRequestDTO {

    @NotBlank
    private String title;
    private List<String> description;
    @NotBlank
    private String brand;
    @NotEmpty
    private List<String> categories;
    @NotEmpty
    private List<String> types;
    private List<String> tags;
    @NotBlank
    private String ownerId;
    @NotBlank
    private String productAddress;
    @NotEmpty
    private List<Variant> variants;
}
