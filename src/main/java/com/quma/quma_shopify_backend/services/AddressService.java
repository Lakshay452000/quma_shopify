package com.quma.quma_shopify_backend.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.models.dtos.AddressDTO;
import com.quma.quma_shopify_backend.models.mongo.Address;
import com.quma.quma_shopify_backend.repositories.mongo.AddressRepository;
import com.quma.quma_shopify_backend.utilities.UserContext;
import com.quma.quma_shopify_backend.utilities.UtilityFunctions;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository repository;

    private final ObjectMapper objectMapper;

    // List all addresses for a username
    public List<AddressDTO> getAddresses() {
        String username = UserContext.get().getUsername();
        return repository.findByUsername(username).stream()
                .map(address -> objectMapper.convertValue(address, AddressDTO.class))
                .toList();
    }

    public AddressDTO addAddress(AddressDTO addressDTO) {
        Address address = objectMapper.convertValue(addressDTO, Address.class);
        String username = UserContext.get().getUsername();
        address.setUsername(username);
        address.setAddressId("ADDR-" + UtilityFunctions.getRandomId());

        repository.findByUsernameAndIsDefaultTrue(username).ifPresentOrElse(
                d -> address.setDefault(false),
                () -> address.setDefault(true));

        return objectMapper.convertValue(repository.save(address), AddressDTO.class);
    }

    public AddressDTO updateAddress(String addressId, AddressDTO update) {

        String username = UserContext.get().getUsername();
        Address updated = repository.findByUsernameAndAddressId(username, addressId)
                .map(addr -> {
                    addr.setHouse(update.getHouse());
                    addr.setArea(update.getArea());
                    addr.setCity(update.getCity());
                    addr.setState(update.getState());
                    addr.setPincode(update.getPincode());
                    return repository.save(addr);
                })
                .orElseThrow(() -> new ApiException("Address not found", 404));

        // Convert to DTO using ObjectMapper
        return objectMapper.convertValue(updated, AddressDTO.class);
    }

    public boolean removeAddress(String addressId) {
        String username = UserContext.get().getUsername();
        return repository.findByUsernameAndAddressId(username, addressId).map(addr -> {
            boolean wasDefault = addr.isDefault();
            repository.delete(addr);

            if (wasDefault) {
                repository.findByUsername(username).stream().findFirst().ifPresent(a -> {
                    a.setDefault(true);
                    repository.save(a);
                });
            }
            return true;
        }).orElseThrow(() -> new ApiException("Address not found", 404));
    }

    public AddressDTO setDefault(String addressId) {
        String username = UserContext.get().getUsername();

        Address newDefault = repository.findByUsernameAndAddressId(username, addressId)
                .orElseThrow(() -> new ApiException("Address not found", 404));

        if (!newDefault.isDefault()) {
            repository.findByUsernameAndIsDefaultTrue(username).ifPresent(currentDefault -> {
                if (!currentDefault.getAddressId().equals(addressId)) {
                    currentDefault.setDefault(false);
                    repository.save(currentDefault);
                }
            });

            newDefault.setDefault(true);
            newDefault = repository.save(newDefault);
        }

        return objectMapper.convertValue(newDefault, AddressDTO.class);
    }

    // Get selected/default address for order
    // public Address getSelectedAddress(String selectedAddressId) {

    // if (selectedAddressId == null) {
    // throw new ApiException("Selected address ID cannot be null", 400);
    // }

    // String username = UserContext.get().getUsername();
    // return repository.findByUsernameAndAddressId(username, selectedAddressId)
    // .orElseThrow(() -> new ApiException("Address not found", 404));

    // }
}
