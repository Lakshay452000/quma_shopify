package com.quma.quma_shopify_backend.interfaces;

import java.util.Map;

import com.quma.quma_shopify_backend.models.dtos.ResetPasswordRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.UserProfileRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.UserRequestDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface IUserService {

    Map<String, Object> registerUser(UserRequestDTO userRequestDTO, HttpServletResponse httpResp);

    Map<String, Object> login(UserRequestDTO userRequestDTO, HttpServletResponse httpResp);

    void logout(HttpServletRequest request, HttpServletResponse httpResp);

    void resetPassword(ResetPasswordRequestDTO resetPasswordRequestDTO);

    void updateProfile(UserProfileRequestDTO userProfileRequestDTO);

    Map<String, Object> getCurrentUser();

    Map<String, Object> getUserProfile();

}
