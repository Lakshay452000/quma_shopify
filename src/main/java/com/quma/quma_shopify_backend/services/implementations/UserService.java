package com.quma.quma_shopify_backend.services.implementations;

import com.quma.quma_shopify_backend.enums.OtpPurpose;
import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.interfaces.IUserService;
import com.quma.quma_shopify_backend.mappers.UserMapper;
import com.quma.quma_shopify_backend.models.dtos.ResetPasswordRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.UserRequestDTO;
import com.quma.quma_shopify_backend.models.mongo.User;
import com.quma.quma_shopify_backend.repositories.mongo.UserInfoRepository;
import com.quma.quma_shopify_backend.services.AuthService;
import com.quma.quma_shopify_backend.utilities.EncryptionUtil;
import com.quma.quma_shopify_backend.validators.UserValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class UserService implements IUserService {

    @Autowired
    UserInfoRepository userInfoRepository;

    @Autowired
    UserValidator userValidator;

    @Autowired
    OtpService otpService;

    @Autowired
    AuthService authService;

    @Override
    public Map<String, Object> registerUser(UserRequestDTO userRequestDTO, HttpServletResponse httpServletResponse) {
        userValidator.validateUserRequest(userRequestDTO);
        otpService.ensureOtpVerified(userRequestDTO.getPhone(), OtpPurpose.REGISTRATION);
        User user = UserMapper.toUserModel(userRequestDTO);
        user.setPassword(EncryptionUtil.bcryptHash(userRequestDTO.getPassword()));
        try {
            userInfoRepository.save(user);
            return authService.login(user.getPhone(), httpServletResponse); // return map with expiry
        } catch (DuplicateKeyException e) {
            throw new ApiException("Phone number already exists", 409);
        }
    }

    @Override
    public Map<String, Object> login(UserRequestDTO userRequestDTO, HttpServletResponse httpServletResponse) {
        userValidator.validateUserRequest(userRequestDTO);
        User user = userInfoRepository.findByPhone(userRequestDTO.getPhone()).orElseThrow(
                () -> new ApiException("Invalid phone number or password", 404));
        if (!EncryptionUtil.bcryptMatches(userRequestDTO.getPassword(), user.getPassword())) {
            throw new ApiException("Invalid phone number or password", 404);
        }
        return authService.login(user.getPhone(), httpServletResponse); // return map with expiry
    }

    @Override
    public void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        authService.logout(httpServletRequest, httpServletResponse);
    }

    @Override
    public void resetPassword(ResetPasswordRequestDTO resetPasswordRequestDTO) {
        otpService.ensureOtpVerified(resetPasswordRequestDTO.getPhone(), OtpPurpose.PASSWORD_RESET);
        User user = userInfoRepository.findByPhone(resetPasswordRequestDTO.getPhone())
                .orElseThrow(() -> new ApiException("User not found", 404));
        user.setPassword(EncryptionUtil.bcryptHash(resetPasswordRequestDTO.getNewPassword()));
        userInfoRepository.save(user);
    }
}
