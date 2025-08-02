package com.quma.quma_shopify_backend.models.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "users-info")
public class User {
    @Id
    private String id;
    private String name;
    @Indexed(unique = true)
    private String phone;
    private String email;
    private String password;
    private String macAddress;
    private String address;
    private String city;
    private String state;
    private String country;
}