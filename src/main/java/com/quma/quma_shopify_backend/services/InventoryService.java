package com.quma.quma_shopify_backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.quma.quma_shopify_backend.models.mongo.ProductInventoryDTO;
import com.quma.quma_shopify_backend.models.mongo.ProductInventoryRequestDTO;
import com.quma.quma_shopify_backend.repositories.mongo.InventoryRepository;

@Service
public class InventoryService {

    @Autowired
    InventoryRepository inventoryRepository;

    public void addItems(ProductInventoryRequestDTO productInventoryRequestDTO) {
        if (productInventoryRequestDTO == null)
            return;

        // Generate ID as combination of productId and identifier
        String id = productInventoryRequestDTO.getProductId() + "-" + productInventoryRequestDTO.getIdentifier();

        // Convert DTO to Mongo entity
        ProductInventoryDTO entity = new ProductInventoryDTO();
        entity.setId(id);
        entity.setProductId(productInventoryRequestDTO.getProductId());
        entity.setIdentifier(productInventoryRequestDTO.getIdentifier());
        entity.setAvailableQuantity(productInventoryRequestDTO.getAvailableQuantity());
        entity.setPrice(productInventoryRequestDTO.getPrice());

        // Save to Mongo
        inventoryRepository.save(entity);
    }

}
