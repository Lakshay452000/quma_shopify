//package com.quma.quma_shopify_backend.services;
//
//import com.quma.quma_shopify_backend.models.dtos.UserRequestDTO;
//import com.quma.quma_shopify_backend.models.sql.User;
//import com.quma.quma_shopify_backend.repositories.sql.UserInfoRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.web.server.ResponseStatusException;
//
//@Service
//public class UserServiceSql {
//
//    @Autowired
//    UserInfoRepository userInfoRepository;
//
//    private User createUserDTO(UserRequestDTO requestDTO) {
//        if (requestDTO == null) {
//            return null;
//        }
//        User user = new User();
//        user.setName(requestDTO.getName());
//        user.setPhone(requestDTO.getPhone());
//        user.setPassword(requestDTO.getPassword());
//        user.setMacAddress(requestDTO.getMacAddress());
//        user.setAddress(requestDTO.getAddress());
//        user.setCity(requestDTO.getCity());
//        user.setState(requestDTO.getState());
//        user.setCountry(requestDTO.getCountry());
//        return user;
//    }
//
//    public void registerUser(UserRequestDTO requestDTO) {
//        if (userInfoRepository.existsByPhone(requestDTO.getPhone())) {
//            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already registered.");
//        }
//        User user = createUserDTO(requestDTO);
//        userInfoRepository.save(user);
//    }
//}
