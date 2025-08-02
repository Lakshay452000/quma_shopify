package com.quma.quma_shopify_backend.utilities;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class EncryptionUtil {

    private static final BCryptPasswordEncoder bcryptEncoder = new BCryptPasswordEncoder();

    // BCrypt: Hash the plain password
    public static String bcryptHash(String plainText) {
        return bcryptEncoder.encode(plainText);
    }

    // BCrypt: Verify plain text against the hashed value
    public static boolean bcryptMatches(String plainText, String hashedText) {
        return bcryptEncoder.matches(plainText, hashedText);
    }
}
