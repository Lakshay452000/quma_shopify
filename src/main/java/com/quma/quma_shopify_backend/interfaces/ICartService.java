package com.quma.quma_shopify_backend.interfaces;

import java.util.List;

import com.quma.quma_shopify_backend.models.dtos.CartCheckoutRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.CartItemRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.CartResponseDTO;

public interface ICartService {
    CartResponseDTO getCartItems();

    CartResponseDTO addItem(CartItemRequestDTO item);

    CartResponseDTO removeItem(CartItemRequestDTO item);

    CartResponseDTO bulkUpdate(List<CartItemRequestDTO> items);

    boolean checkout(CartCheckoutRequestDTO request);
}
