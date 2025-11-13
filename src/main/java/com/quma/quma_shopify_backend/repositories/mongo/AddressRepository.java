package com.quma.quma_shopify_backend.repositories.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.quma.quma_shopify_backend.models.mongo.Address;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends MongoRepository<Address, String> {
    List<Address> findByUsername(String username);

    Optional<Address> findByUsernameAndIsDefaultTrue(String username);

    Optional<Address> findByUsernameAndAddressId(String username, String addressId);

    Address findByAddressId(String addressId);
}
