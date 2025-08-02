package com.quma.quma_shopify_backend.mappers;

import com.quma.quma_shopify_backend.models.dtos.UserRequestDTO;
import com.quma.quma_shopify_backend.models.mongo.User;

public class UserMapper {
    private UserMapper() {
        // Utility class - prevent instantiation
    }

    public static User toUserModel(UserRequestDTO dto) {
        User user = new User();
        user.setPhone(dto.getPhone());
        user.setPassword(dto.getPassword());
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());
        return user;
    }
}

