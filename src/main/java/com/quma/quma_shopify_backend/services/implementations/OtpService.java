package com.quma.quma_shopify_backend.services.implementations;

import com.quma.quma_shopify_backend.enums.OtpPurpose;
import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.interfaces.IOtpService;
import com.quma.quma_shopify_backend.interfaces.IStore;
import com.quma.quma_shopify_backend.models.dtos.OtpDTO;
import com.quma.quma_shopify_backend.models.mongo.User;
import com.quma.quma_shopify_backend.models.redis.OtpRedisModel;
import com.quma.quma_shopify_backend.repositories.mongo.UserInfoRepository;
import com.quma.quma_shopify_backend.validators.OtpRequestValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpService implements IOtpService {

    @Autowired
    RedisStore redisStore;

    @Autowired
    IStore iStore;

    @Autowired
    OtpRequestValidator otpRequestValidator;

    @Autowired
    UserInfoRepository userInfoRepository;

    public static final String REDIS_OTP_KEY_PREFIX = "otp_";
    public static final String REDIS_RESET_PASSWORD_KEY_PREFIX = "reset_password_";

    @Override
    public void ensureOtpVerified(String phone, OtpPurpose otpPurpose) {
        String redisKeyPrefix = getRedisPrefixKey(phone, otpPurpose);
        OtpRedisModel otpRedisModel = redisStore.get(redisKeyPrefix, OtpRedisModel.class);
        if (otpRedisModel == null || !otpRedisModel.isVerified()) {
            throw new ApiException("Otp not verified", 403);
        }
    }

    private String generateOtp() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    private static String getRedisPrefixKey(String phone, OtpPurpose otpPurpose) {
        return switch (otpPurpose) {
            case REGISTRATION -> REDIS_OTP_KEY_PREFIX + phone;
            case PASSWORD_RESET -> REDIS_RESET_PASSWORD_KEY_PREFIX + phone;
        };
    }

    @Override
    public void sendOtp(OtpDTO otpDTO) {
        otpRequestValidator.validateOtpRequest(otpDTO);
        if (otpDTO.getOtpPurpose() == OtpPurpose.REGISTRATION) {
            Optional<User> existingUser = userInfoRepository.findByPhone(otpDTO.getPhone());
            if (existingUser.isPresent()) {
                throw new ApiException("Phone number already exists", 409);
            }
        }

        String phone = otpDTO.getPhone();
        String otp = generateOtp();
        // whatsAppService.sendOtp(phone, otp);
        OtpRedisModel otpRedisModel = new OtpRedisModel(phone, otp, false);
        String redisPrefixKey = getRedisPrefixKey(phone, otpDTO.getOtpPurpose());
        iStore.save(redisPrefixKey, otpRedisModel, Duration.ofHours(1));
    }

    @Override
    public void verifyOtp(OtpDTO otpDTO) {
        otpRequestValidator.validateOtpRequest(otpDTO);
        String key = getRedisPrefixKey(otpDTO.getPhone(), otpDTO.getOtpPurpose());
        // Get the OTP data
        OtpRedisModel otpRedisModel = redisStore.get(key, OtpRedisModel.class);
        if (otpRedisModel == null) {
            throw new ApiException("OTP not found or expired", 404);
        }
        // Check if the OTP matches
        if (!otpRedisModel.getOtp().equals(otpDTO.getOtp())) {
            throw new ApiException("Invalid OTP", 403);
        }
        // Set verified true
        otpRedisModel.setVerified(true);
        // Save back with remaining TTL
        Long ttlSeconds = redisStore.getTimeToExpire(key, TimeUnit.SECONDS);
        if (ttlSeconds != null && ttlSeconds > 0) {
            redisStore.save(key, otpRedisModel, Duration.ofSeconds(ttlSeconds));
        } else {
            redisStore.save(key, otpRedisModel, Duration.ofHours(1)); // fallback
        }
    }
}
