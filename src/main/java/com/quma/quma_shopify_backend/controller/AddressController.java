package com.quma.quma_shopify_backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.quma.quma_shopify_backend.models.dtos.AddressDTO;
import com.quma.quma_shopify_backend.services.AddressService;

import java.util.List;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public List<AddressDTO> getAddresses() {
        return addressService.getAddresses();
    }

    @PostMapping("create-address")
    public AddressDTO addAddress(@RequestBody AddressDTO address) {
        return addressService.addAddress(address);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressDTO> updateAddress(@PathVariable String addressId,
            @RequestBody AddressDTO update) {
        return ResponseEntity.ok(addressService.updateAddress(addressId, update));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> removeAddress(@PathVariable String addressId) {
        boolean removed = addressService.removeAddress(addressId);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{addressId}/default")
    public ResponseEntity<AddressDTO> setDefault(@PathVariable String addressId) {
        return ResponseEntity.ok(addressService.setDefault(addressId));
    }

}
