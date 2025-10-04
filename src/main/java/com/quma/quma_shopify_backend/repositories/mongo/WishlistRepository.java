package com.quma.quma_shopify_backend.repositories.mongo;

import com.quma.quma_shopify_backend.models.mongo.Wishlist;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends MongoRepository<Wishlist, String> {

    List<Wishlist> findByUsername(String username);

    List<Wishlist> findByUsernameAndProductId(String username, String productId);

    Optional<Wishlist> findByUsernameAndProductIdAndIdentifier(String username, String productId, String identifier);

    void deleteByUsernameAndProductIdAndIdentifier(String username, String productId, String identifier);
}
