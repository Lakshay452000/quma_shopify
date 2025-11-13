package com.quma.quma_shopify_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quma.quma_shopify_backend.models.mongo.ProductInventoryRequestDTO;
import com.quma.quma_shopify_backend.services.InventoryService;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    InventoryService inventoryService;

    @PostMapping("/add")
    public ResponseEntity<Void> addItaddOrUpdateInventory(
            @RequestBody ProductInventoryRequestDTO productInventoryRequestDTO) {
        inventoryService.addOrUpdateInventory(productInventoryRequestDTO);
        return ResponseEntity.noContent().build();
    }
}
