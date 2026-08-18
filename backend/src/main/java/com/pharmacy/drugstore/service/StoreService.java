package com.pharmacy.drugstore.service;

import com.pharmacy.drugstore.dto.CartRequest;
import com.pharmacy.drugstore.entity.CartItem;
import com.pharmacy.drugstore.entity.Product;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.entity.WishlistItem;
import com.pharmacy.drugstore.repository.CartItemRepository;
import com.pharmacy.drugstore.repository.ProductRepository;
import com.pharmacy.drugstore.repository.WishlistItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class StoreService {
    private final ProductRepository products;
    private final CartItemRepository cartItems;
    private final WishlistItemRepository wishlistItems;

    public StoreService(ProductRepository products, CartItemRepository cartItems, WishlistItemRepository wishlistItems) {
        this.products = products;
        this.cartItems = cartItems;
        this.wishlistItems = wishlistItems;
    }

    public List<CartItem> getCart(User user) {
        return cartItems.findByUser(user);
    }

    public CartItem addToCart(User user, CartRequest req) {
        Product product = products.findById(req.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        int qty = Math.max(1, req.quantity());
        CartItem item = cartItems.findByUserAndProductId(user, product.getId()).orElseGet(() -> {
            CartItem created = new CartItem();
            created.setUser(user);
            created.setProduct(product);
            created.setQuantity(0);
            return created;
        });
        item.setQuantity(item.getQuantity() + qty);
        return cartItems.save(item);
    }

    public CartItem updateCart(User user, Long itemId, int quantity) {
        CartItem item = cartItems.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
        if (!item.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your cart");
        }
        if (quantity < 1) {
            cartItems.delete(item);
            return item;
        }
        item.setQuantity(quantity);
        return cartItems.save(item);
    }

    public void removeCartItem(User user, Long itemId) {
        CartItem item = cartItems.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
        if (!item.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your cart");
        }
        cartItems.delete(item);
    }

    public List<WishlistItem> getWishlist(User user) {
        return wishlistItems.findByUser(user);
    }

    @Transactional
    public void clearCart(User user) {
        cartItems.deleteByUser(user);
    }

    @Transactional
    public WishlistItem toggleWishlist(User user, Long productId) {
        var existing = wishlistItems.findByUserAndProductId(user, productId);
        if (existing.isPresent()) {
            wishlistItems.deleteByUserAndProductId(user, productId);
            return null;
        }
        Product product = products.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setProduct(product);
        return wishlistItems.save(item);
    }
}
