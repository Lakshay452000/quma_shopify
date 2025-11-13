package com.quma.quma_shopify_backend.repositories.mongo;

import com.quma.quma_shopify_backend.models.mongo.Wishlist;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WishlistRepository extends MongoRepository<Wishlist, String> {

    // ✅ Paginated version
    Page<Wishlist> findByUsername(String username, Pageable pageable);

    Optional<Wishlist> findByUsernameAndProductId(String username, String productId);

    List<Wishlist> findAllByUsernameAndProductIdIn(String username, List<String> productIds);

    void deleteByUsernameAndProductId(String username, String productId);

    void deleteByUsernameAndProductIdIn(String username, List<String> productIds);

}
