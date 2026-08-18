package com.pharmacy.drugstore.config;

import com.pharmacy.drugstore.entity.Category;
import com.pharmacy.drugstore.entity.Product;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.repository.CategoryRepository;
import com.pharmacy.drugstore.repository.ProductRepository;
import com.pharmacy.drugstore.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
public class DataSeeder {
    @Bean
    CommandLineRunner seedData(
            CategoryRepository categories,
            ProductRepository products,
            UserRepository users,
            com.pharmacy.drugstore.repository.LedgerAccountRepository ledgerAccounts,
            PasswordEncoder encoder) {
        return args -> {
            seedLedger(ledgerAccounts);
            if (users.count() == 0) {
                User demo = new User();
                demo.setName("Demo User");
                demo.setEmail("demo@pharmacy.com");
                demo.setPassword(encoder.encode("demo123"));
                demo.setRole("USER");
                users.save(demo);
            }
            if (products.count() > 0) {
                applyInrPrices(products);
                return;
            }

            Category devices = cat(categories, "Medical Devices", "thermometer");
            Category masks = cat(categories, "Masks & Protection", "mask");
            Category care = cat(categories, "Personal Care", "sanitizer");
            Category firstAid = cat(categories, "First Aid", "kit");
            Category vitamins = cat(categories, "Vitamins", "pills");
            Category wellness = cat(categories, "Wellness", "heart");

            Instant ends = Instant.now().plus(2, ChronoUnit.DAYS);

            products.saveAll(List.of(
                    product("Smart Infrared Thermometer", "Non-contact forehead thermometer with 1-second reading and fever alarm.",
                            "1299.00", "1799.00", "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=600",
                            "MediTech", devices, true, true, ends, 4.8, 214),
                    product("Boldfit N95 Mask For Face", "ISI-certified N95 respirator mask. Pack of 10, comfortable ear loops.",
                            "349.00", "499.00", "https://images.unsplash.com/photo-1584634731339-252c581abfc5?w=600",
                            "Boldfit", masks, true, false, ends, 4.6, 480),
                    product("Adult Digital IR Thermometer", "Clinical-grade infrared thermometer for adults and children.",
                            "899.00", "1199.00", "https://images.unsplash.com/photo-1581595220892-b2452acd2da2?w=600",
                            "CarePlus", devices, true, false, ends, 4.5, 156),
                    product("Sphygmomanometer BP Monitor", "Automatic upper-arm blood pressure monitor with large LCD display.",
                            "1899.00", "2499.00", "https://images.unsplash.com/photo-1615486511484-92e172b4d7b0?w=600",
                            "Omron", devices, true, true, ends, 4.7, 302),
                    product("Hand Sanitizer 500ml", "Kills 99.9% of bacteria. Non-irritating, moisturizing formula.",
                            "89.00", "129.00", "https://images.unsplash.com/photo-1584483766114-2cea6facdf57?w=600",
                            "PureGuard", care, false, true, null, 4.9, 890),
                    product("Pulse Oximeter Fingertip", "SpO2 and pulse rate monitor with OLED display.",
                            "799.00", "1099.00", "https://images.unsplash.com/photo-1603398938378-e54eab446dde?w=600",
                            "MediTech", devices, false, true, null, 4.4, 198),
                    product("Vitamin D3 60 Capsules", "High-potency vitamin D3 for bone and immune support.",
                            "399.00", "499.00", "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500",
                            "NutriLife", vitamins, false, true, null, 4.6, 410),
                    product("First Aid Kit Compact", "40-piece home and travel first aid kit in a durable case.",
                            "599.00", "799.00", "https://images.unsplash.com/photo-1603398938472-2d8d0a095d43?w=600",
                            "CarePlus", firstAid, false, false, null, 4.3, 95),
                    product("Surgical Face Masks 50pcs", "3-ply disposable surgical masks, ASTM Level 2.",
                            "199.00", "299.00", "https://images.unsplash.com/photo-1584634731339-252c581abfc5?w=500",
                            "SafeMed", masks, false, false, null, 4.5, 670),
                    product("Digital Weighing Scale", "Tempered glass body scale with BMI calculation.",
                            "899.00", "1299.00", "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=600",
                            "FitHome", wellness, false, true, null, 4.2, 140)
            ));
        };
    }

    private void seedLedger(com.pharmacy.drugstore.repository.LedgerAccountRepository ledgerAccounts) {
        seedAccount(ledgerAccounts, "UPI_CLEARING", "UPI clearing", "ASSET");
        seedAccount(ledgerAccounts, "CARD_CLEARING", "Card clearing", "ASSET");
        seedAccount(ledgerAccounts, "NETBANKING_CLEARING", "Net banking clearing", "ASSET");
        seedAccount(ledgerAccounts, "WALLET_CLEARING", "Wallet clearing", "ASSET");
        seedAccount(ledgerAccounts, "COD_RECEIVABLE", "Cash on delivery receivable", "ASSET");
        seedAccount(ledgerAccounts, "MERCHANT_SALES", "Medicine Drugstore sales", "REVENUE");
    }

    private void seedAccount(com.pharmacy.drugstore.repository.LedgerAccountRepository repo, String code, String name, String type) {
        if (repo.findByCode(code).isPresent()) return;
        com.pharmacy.drugstore.entity.LedgerAccount account = new com.pharmacy.drugstore.entity.LedgerAccount();
        account.setCode(code);
        account.setName(name);
        account.setType(type);
        account.setBalance(BigDecimal.ZERO);
        repo.save(account);
    }

    private void applyInrPrices(ProductRepository products) {
        java.util.Map<String, String[]> prices = java.util.Map.ofEntries(
                java.util.Map.entry("Smart Infrared Thermometer", new String[]{"1299.00", "1799.00"}),
                java.util.Map.entry("Boldfit N95 Mask For Face", new String[]{"349.00", "499.00"}),
                java.util.Map.entry("Adult Digital IR Thermometer", new String[]{"899.00", "1199.00"}),
                java.util.Map.entry("Sphygmomanometer BP Monitor", new String[]{"1899.00", "2499.00"}),
                java.util.Map.entry("Hand Sanitizer 500ml", new String[]{"89.00", "129.00"}),
                java.util.Map.entry("Pulse Oximeter Fingertip", new String[]{"799.00", "1099.00"}),
                java.util.Map.entry("Vitamin D3 60 Capsules", new String[]{"399.00", "499.00"}),
                java.util.Map.entry("First Aid Kit Compact", new String[]{"599.00", "799.00"}),
                java.util.Map.entry("Surgical Face Masks 50pcs", new String[]{"199.00", "299.00"}),
                java.util.Map.entry("Digital Weighing Scale", new String[]{"899.00", "1299.00"})
        );
        for (Product p : products.findAll()) {
            String[] next = prices.get(p.getName());
            if (next != null) {
                p.setPrice(new BigDecimal(next[0]));
                p.setCompareAtPrice(new BigDecimal(next[1]));
                products.save(p);
            }
        }
    }

    private Category cat(CategoryRepository repo, String name, String icon) {
        Category c = new Category();
        c.setName(name);
        c.setIcon(icon);
        return repo.save(c);
    }

    private Product product(String name, String desc, String price, String compare, String img,
                            String brand, Category category, boolean deal, boolean featured,
                            Instant ends, double rating, int reviews) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(desc);
        p.setPrice(new BigDecimal(price));
        p.setCompareAtPrice(new BigDecimal(compare));
        p.setImageUrl(img);
        p.setBrand(brand);
        p.setCategory(category);
        p.setDealOfTheDay(deal);
        p.setFeatured(featured);
        p.setDealEndsAt(ends);
        p.setRating(rating);
        p.setReviewCount(reviews);
        p.setStock(120);
        return p;
    }
}
