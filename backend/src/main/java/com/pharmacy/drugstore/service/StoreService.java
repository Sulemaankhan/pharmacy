package com.pharmacy.drugstore.service;

import com.pharmacy.drugstore.dto.CartRequest;
import com.pharmacy.drugstore.entity.CartItem;
import com.pharmacy.drugstore.entity.Product;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.entity.WishlistItem;
import com.pharmacy.drugstore.repository.CartItemRepository;
import com.pharmacy.drugstore.repository.ProductRepository;
import com.pharmacy.drugstore.repository.WishlistItemRepository;
import com.pharmacy.drugstore.logging.RequestMdc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class StoreService {
    private static final Logger log = LoggerFactory.getLogger(StoreService.class);
    private final ProductRepository products;
    private final CartItemRepository cartItems;
    private final WishlistItemRepository wishlistItems;

    public StoreService(ProductRepository products, CartItemRepository cartItems, WishlistItemRepository wishlistItems) {
        this.products = products;
        this.cartItems = cartItems;
        this.wishlistItems = wishlistItems;
    }

    @Transactional(readOnly = true)
    public List<CartItem> getCart(User user) {
        List<CartItem> items = cartItems.findByUserId(user.getId());
        log.info("Cart list {} items={} productIds={}",
                RequestMdc.describe(user),
                items.size(),
                items.stream().map(i -> i.getProduct().getId()).toList());
        return items;
    }

    @Transactional
    public CartItem addToCart(User user, CartRequest req) {
        log.info("Cart add {} productId={} qty={}", RequestMdc.describe(user), req.productId(), req.quantity());
        Product product = products.findById(req.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        int qty = Math.max(1, req.quantity());
        CartItem item = cartItems.findByUserIdAndProductId(user.getId(), product.getId()).orElseGet(() -> {
            CartItem created = new CartItem();
            created.setUser(user);
            created.setProduct(product);
            created.setQuantity(0);
            return created;
        });
        item.setQuantity(item.getQuantity() + qty);
        CartItem saved = cartItems.save(item);
        log.info("Cart saved {} itemId={} productId={} qty={}",
                RequestMdc.describe(user), saved.getId(), product.getId(), saved.getQuantity());
        return saved;
    }

    @Transactional
    public CartItem updateCart(User user, Long itemId, int quantity) {
        log.info("Cart update {} itemId={} qty={}", RequestMdc.describe(user), itemId, quantity);
        CartItem item = cartItems.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
        if (!item.getUser().getId().equals(user.getId())) {
            log.warn("Cart update forbidden {} itemId={} ownerId={}",
                    RequestMdc.describe(user), itemId, item.getUser().getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your cart");
        }
        if (quantity < 1) {
            cartItems.delete(item);
            log.info("Cart item removed {} itemId={}", RequestMdc.describe(user), itemId);
            return item;
        }
        item.setQuantity(quantity);
        return cartItems.save(item);
    }

    @Transactional
    public void removeCartItem(User user, Long itemId) {
        log.info("Cart remove {} itemId={}", RequestMdc.describe(user), itemId);
        CartItem item = cartItems.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
        if (!item.getUser().getId().equals(user.getId())) {
            log.warn("Cart remove forbidden {} itemId={} ownerId={}",
                    RequestMdc.describe(user), itemId, item.getUser().getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your cart");
        }
        cartItems.delete(item);
        log.info("Cart item deleted {} itemId={}", RequestMdc.describe(user), itemId);
    }

    @Transactional(readOnly = true)
    public List<WishlistItem> getWishlist(User user) {
        List<WishlistItem> items = wishlistItems.findByUserId(user.getId());
        log.info("Wishlist list {} items={}", RequestMdc.describe(user), items.size());
        return items;
    }

    @Transactional
    public void clearCart(User user) {
        int removed = cartItems.deleteByUserId(user.getId());
        log.info("Cart cleared {} removed={}", RequestMdc.describe(user), removed);
    }

    @Transactional
    public WishlistItem toggleWishlist(User user, Long productId) {
        log.info("Wishlist toggle {} productId={}", RequestMdc.describe(user), productId);
        var existing = wishlistItems.findByUserIdAndProductId(user.getId(), productId);
        if (existing.isPresent()) {
            wishlistItems.delete(existing.get());
            log.info("Wishlist removed {} productId={}", RequestMdc.describe(user), productId);
            return null;
        }
        Product product = products.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setProduct(product);
        WishlistItem saved = wishlistItems.save(item);
        log.info("Wishlist added {} productId={} itemId={}", RequestMdc.describe(user), productId, saved.getId());
        return saved;
    }
}
