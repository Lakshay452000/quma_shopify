package com.quma.quma_shopify_backend.models.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.quma.quma_shopify_backend.enums.Gender;

@Data
@Document(collection = "users-info")
public class User {
    @Id
    private String id;
    @Indexed(unique = true)
    private String phone;
    private String password;
    private String name;
    private String email;
    private Gender gender;
    private String dateOfBirth;
    private boolean profileCompleted = false;
}