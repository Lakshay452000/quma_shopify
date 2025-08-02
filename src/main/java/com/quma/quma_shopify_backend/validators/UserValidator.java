package com.quma.quma_shopify_backend.validators;

import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.models.dtos.UserRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class UserValidator {

    public void validateUserRequest(UserRequestDTO dto) {
        if (dto == null || dto.getPhone() == null || dto.getPassword() == null) {
            throw new ApiException("Phone and Password are required", 400);
        }
    }
}
