//package com.quma.quma_shopify_backend.models.sql;
//import jakarta.persistence.*;
//import lombok.*;
//
//@Entity
//@Table(name = "user_info", uniqueConstraints = @UniqueConstraint(columnNames = "phone"))
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor
//public class UserSql {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String name;
//
//    @Column(nullable = false, unique = true)
//    private String phone;
//
//    @Column(nullable = false)
//    private String password;
//
//    private String ipAddress;
//    private String macAddress;
//    private String address;
//    private String city;
//    private String state;
//    private String country;
//}
