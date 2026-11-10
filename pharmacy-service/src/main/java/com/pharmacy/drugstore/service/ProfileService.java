package com.pharmacy.drugstore.service;

import com.pharmacy.drugstore.client.CustomerClient;
import com.pharmacy.drugstore.dto.CustomerProfile;
import com.pharmacy.drugstore.dto.ProfileUpdateRequest;
import com.pharmacy.drugstore.dto.UserProfileResponse;
import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.logging.RequestMdc;
import com.pharmacy.drugstore.repository.CartItemRepository;
import com.pharmacy.drugstore.repository.CustomerOrderRepository;
import com.pharmacy.drugstore.repository.WishlistItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class ProfileService {
    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final CustomerClient customers;
    private final CustomerOrderRepository orders;
    private final CartItemRepository cartItems;
    private final WishlistItemRepository wishlist;

    public ProfileService(
            CustomerClient customers,
            CustomerOrderRepository orders,
            CartItemRepository cartItems,
            WishlistItemRepository wishlist) {
        this.customers = customers;
        this.orders = orders;
        this.cartItems = cartItems;
        this.wishlist = wishlist;
    }

    public UserProfileResponse get(User user, String bearerToken) {
        log.info("Profile get {}", RequestMdc.describe(user));
        return toResponse(user, customers.profile(bearerToken));
    }

    public UserProfileResponse update(User user, String bearerToken, ProfileUpdateRequest req) {
        CustomerProfile profile = customers.updateProfile(bearerToken, req);
        log.info("Profile updated userId={} email={}", profile.userId(), profile.email());
        return toResponse(user, profile);
    }

    private UserProfileResponse toResponse(User localUser, CustomerProfile profile) {
        Long id = profile.userId() != null ? profile.userId() : localUser.getId();
        String lastOrder = orders.findFirstByUser_IdOrderByCreatedAtDesc(id)
                .map(CustomerOrder::getOrderNumber)
                .orElse(null);
        return new UserProfileResponse(
                id,
                profile.name(),
                profile.email(),
                profile.role(),
                profile.phone(),
                profile.address(),
                profile.city(),
                profile.state(),
                profile.pincode(),
                profile.createdAt(),
                profile.updatedAt(),
                Instant.now(),
                profile.token(),
                orders.countByUserId(id),
                cartItems.countByUser_Id(id),
                wishlist.countByUser_Id(id),
                lastOrder
        );
    }
}
