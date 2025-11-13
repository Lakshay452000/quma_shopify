package com.quma.quma_shopify_backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.quma.quma_shopify_backend.models.mongo.ProductInventory;
import com.quma.quma_shopify_backend.models.mongo.ProductInventoryRequestDTO;
import com.quma.quma_shopify_backend.repositories.mongo.InventoryRepository;

@Service
public class InventoryService {

    @Autowired
    InventoryRepository inventoryRepository;

    public void addOrUpdateInventory(ProductInventoryRequestDTO dto) {
        if (dto == null || dto.getProductId() == null)
            return;

        // Fetch existing inventory by productId, or create new
        ProductInventory entity = inventoryRepository.findByProductId(dto.getProductId())
                .orElseGet(ProductInventory::new);

        // Set productId (required)
        entity.setProductId(dto.getProductId());

        // Update only if fields are not null
        if (dto.getAvailableQuantity() != null) {
            entity.setAvailableQuantity(dto.getAvailableQuantity());
        }
        if (dto.getPrice() != null) {
            entity.setPrice(dto.getPrice());
        }
        if (dto.getDiscountedPrice() != null) {
            entity.setDiscountedPrice(dto.getDiscountedPrice());
        }

        // Save to Mongo
        inventoryRepository.save(entity);
    }

}
