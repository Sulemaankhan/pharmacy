package com.pharmacy.drugstore.service;


import java.time.Instant;
import java.util.Objects;
import com.pharmacy.drugstore.client.CustomerClient;
import com.pharmacy.drugstore.dto.CustomerProfile;
import com.pharmacy.drugstore.dto.ProfileUpdateRequest;
import com.pharmacy.drugstore.dto.UserProfileResponse;
import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.logging.RequestMdc;
import com.pharmacy.drugstore.parallel.StructuredFanout;
import com.pharmacy.drugstore.repository.CartItemRepository;
import com.pharmacy.drugstore.repository.CustomerOrderRepository;
import com.pharmacy.drugstore.repository.WishlistItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
        Long localId = user.getId();
        StructuredFanout.Pair<CustomerProfile, ProfileStats> loaded = StructuredFanout.both(
                () -> remoteProfile(user, bearerToken),
                () -> loadStats(localId));
        CustomerProfile profile = loaded.left() == null ? localProfile(user) : loaded.left();
        Long id = profile.userId() != null ? profile.userId() : localId;
        ProfileStats stats = Objects.equals(id, localId) ? loaded.right() : loadStats(id);
        return assemble(profile, id, stats);
    }

    private CustomerProfile remoteProfile(User user, String bearerToken) {
        try {
            return customers.profile(bearerToken);
        } catch (RuntimeException ex) {
            log.warn("Customer profile unavailable, using local user {}", RequestMdc.describe(user));
            return null;
        }
    }

    public UserProfileResponse update(User user, String bearerToken, ProfileUpdateRequest req) {
        CustomerProfile profile = customers.updateProfile(bearerToken, req);
        log.info("Profile updated userId={} email={}", profile.userId(), profile.email());
        return toResponse(user, profile);
    }

    private static CustomerProfile localProfile(User user) {
        return new CustomerProfile(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPhone(),
                user.getAddress(),
                user.getCity(),
                user.getState(),
                user.getPincode(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                null);
    }

    private UserProfileResponse toResponse(User localUser, CustomerProfile profile) {
        Long id = profile.userId() != null ? profile.userId() : localUser.getId();
        return assemble(profile, id, loadStats(id));
    }

    private ProfileStats loadStats(Long id) {
        String lastOrder = orders.findFirstByUser_IdOrderByCreatedAtDesc(id)
                .map(CustomerOrder::getOrderNumber)
                .orElse(null);
        return new ProfileStats(
                orders.countByUserId(id),
                cartItems.countByUser_Id(id),
                wishlist.countByUser_Id(id),
                lastOrder);
    }

    private static UserProfileResponse assemble(CustomerProfile profile, Long id, ProfileStats stats) {
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
                stats.orderCount(),
                stats.cartCount(),
                stats.wishlistCount(),
                stats.lastOrderNumber());
    }

    private record ProfileStats(long orderCount, long cartCount, long wishlistCount, String lastOrderNumber) {}
}
