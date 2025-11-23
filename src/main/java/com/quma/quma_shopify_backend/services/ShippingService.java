package com.quma.quma_shopify_backend.services;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.handlers.ShippingProviderFactoryHandler;
import com.quma.quma_shopify_backend.interfaces.IShippingProvider;
import com.quma.quma_shopify_backend.models.mongo.CartItemMongoDTO;
import com.quma.quma_shopify_backend.repositories.mongo.CartRepository;
import com.quma.quma_shopify_backend.utilities.UserContext;

@Service
public class ShippingService {

    @Autowired
    CartRepository cartRepository;

    @Autowired
    private ShippingProviderFactoryHandler providerFactory;

    public BigDecimal calculateShipping(String providerName, String addressId) {
        String username = UserContext.get().getUsername();
        CartItemMongoDTO cart = cartRepository.findByUsername(username);
        if (cart == null || cart.getCartItemDTOs().isEmpty()) {
            throw new ApiException("Cart is empty", 404);
        }
        // Address address = addressRepository.findById(addressId)
        // .orElseThrow(() -> new RuntimeException("Address not found"));

        double weight = cart.getCartItemDTOs().stream()
                .mapToDouble(i -> (i.getWeight() == null ? 1 : i.getWeight()) * i.getQuantity())
                .sum();
        // String pincode = address.getPincode();
        String pincode = "560001"; // TODO: Replace with actual pincode from address
        IShippingProvider provider = providerFactory.getProvider(providerName);
        return provider.getShippingCharge(pincode, weight);
    }

    // Optionally return all providers with their charges
    // public List<ShippingOptionDTO> getAvailableOptions(String cartId, String
    // addressId) {
    // Cart cart = cartRepository.findById(cartId)
    // .orElseThrow(() -> new RuntimeException("Cart not found"));
    // Address address = addressRepository.findById(addressId)
    // .orElseThrow(() -> new RuntimeException("Address not found"));

    // double weight = cart.getItems().stream()
    // .mapToDouble(i -> i.getWeight() * i.getQuantity())
    // .sum();
    // String pincode = address.getPincode();

    // List<ShippingOptionDTO> options = new ArrayList<>();
    // for (ShippingProvider provider : providers) {
    // BigDecimal charge = provider.getShippingCharge(pincode, weight);
    // options.add(new ShippingOptionDTO(provider.getName(), charge));
    // }
    // return options;
    // }
}
