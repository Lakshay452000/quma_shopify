package com.quma.quma_shopify_backend.interfaces;

import com.quma.quma_shopify_backend.models.dtos.ResetPasswordRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.UserRequestDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface IUserService {

    void registerUser(UserRequestDTO userRequestDTO, HttpServletResponse httpResp);

    void login(UserRequestDTO userRequestDTO, HttpServletResponse httpResp);

    void logout(HttpServletRequest request, HttpServletResponse httpResp);

    void resetPassword(ResetPasswordRequestDTO resetPasswordRequestDTO);
}

