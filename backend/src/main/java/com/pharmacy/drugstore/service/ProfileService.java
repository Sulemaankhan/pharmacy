package com.pharmacy.drugstore.service;

import com.pharmacy.drugstore.dto.ProfileUpdateRequest;
import com.pharmacy.drugstore.dto.UserProfileResponse;
import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.logging.RequestMdc;
import com.pharmacy.drugstore.repository.CartItemRepository;
import com.pharmacy.drugstore.repository.CustomerOrderRepository;
import com.pharmacy.drugstore.repository.UserRepository;
import com.pharmacy.drugstore.repository.WishlistItemRepository;
import com.pharmacy.drugstore.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Locale;

@Service
public class ProfileService {
    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final UserRepository users;
    private final CustomerOrderRepository orders;
    private final CartItemRepository cartItems;
    private final WishlistItemRepository wishlist;
    private final JwtService jwt;

    public ProfileService(
            UserRepository users,
            CustomerOrderRepository orders,
            CartItemRepository cartItems,
            WishlistItemRepository wishlist,
            JwtService jwt) {
        this.users = users;
        this.orders = orders;
        this.cartItems = cartItems;
        this.wishlist = wishlist;
        this.jwt = jwt;
    }

    public UserProfileResponse get(User user) {
        log.info("Profile get {}", RequestMdc.describe(user));
        return toResponse(user, null);
    }

    @Transactional
    public UserProfileResponse update(User user, ProfileUpdateRequest req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter your name");
        }
        String email = req.email() == null ? "" : req.email().trim().toLowerCase(Locale.ROOT);
        if (email.isBlank() || !email.contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid email ID");
        }
        String phone = digits(req.phone());
        if (!phone.isEmpty() && phone.length() != 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a 10-digit contact number");
        }
        String pin = digits(req.pincode());
        if (!pin.isEmpty() && pin.length() != 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a 6-digit pincode");
        }
        if (!email.equalsIgnoreCase(user.getEmail()) && users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        boolean emailChanged = !email.equalsIgnoreCase(user.getEmail());
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPhone(phone.isEmpty() ? null : phone);
        user.setAddress(blankToNull(req.address()));
        user.setCity(blankToNull(req.city()));
        user.setState(blankToNull(req.state()));
        user.setPincode(pin.isEmpty() ? null : pin);
        user.setUpdatedAt(Instant.now());
        users.saveAndFlush(user);
        RequestMdc.setUser(user);

        String token = emailChanged ? jwt.generate(user.getEmail()) : null;
        log.info("Profile updated {} emailChanged={}", RequestMdc.describe(user), emailChanged);
        return toResponse(user, token);
    }

    private UserProfileResponse toResponse(User user, String token) {
        Long id = user.getId();
        String lastOrder = orders.findFirstByUser_IdOrderByCreatedAtDesc(id)
                .map(CustomerOrder::getOrderNumber)
                .orElse(null);
        return new UserProfileResponse(
                id,
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
                Instant.now(),
                token,
                orders.countByUserId(id),
                cartItems.countByUser_Id(id),
                wishlist.countByUser_Id(id),
                lastOrder
        );
    }

    private static String digits(String value) {
        if (value == null) return "";
        return value.replaceAll("\\D", "");
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
