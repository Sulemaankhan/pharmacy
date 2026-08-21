package com.pharmacy.drugstore.controller;

import com.pharmacy.drugstore.dto.CartRequest;
import com.pharmacy.drugstore.entity.CartItem;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.entity.WishlistItem;
import com.pharmacy.drugstore.service.AuthService;
import com.pharmacy.drugstore.service.StoreService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StoreController {
    private final StoreService storeService;
    private final AuthService authService;

    public StoreController(StoreService storeService, AuthService authService) {
        this.storeService = storeService;
        this.authService = authService;
    }

    @GetMapping("/cart")
    public List<CartItem> cart(Authentication auth) {
        return storeService.getCart(user(auth));
    }

    @PostMapping("/cart")
    public CartItem addCart(Authentication auth, @RequestBody CartRequest request) {
        return storeService.addToCart(user(auth), request);
    }

    @PatchMapping("/cart/{id}")
    public CartItem updateCart(Authentication auth, @PathVariable Long id, @RequestBody CartRequest request) {
        return storeService.updateCart(user(auth), id, request.quantity());
    }

    @DeleteMapping("/cart/{id}")
    public Map<String, String> removeCart(Authentication auth, @PathVariable Long id) {
        storeService.removeCartItem(user(auth), id);
        return Map.of("status", "removed");
    }

    @GetMapping("/wishlist")
    public List<WishlistItem> wishlist(Authentication auth) {
        return storeService.getWishlist(user(auth));
    }

    @PostMapping("/wishlist/{productId}")
    public Map<String, Object> toggleWishlist(Authentication auth, @PathVariable Long productId) {
        WishlistItem item = storeService.toggleWishlist(user(auth), productId);
        return Map.of("wishlisted", item != null);
    }

    private User user(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in");
        }
        return authService.requireUser(auth.getName());
    }
}
