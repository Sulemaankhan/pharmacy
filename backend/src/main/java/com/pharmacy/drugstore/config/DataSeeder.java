package com.pharmacy.drugstore.config;

import com.pharmacy.drugstore.entity.Category;
import com.pharmacy.drugstore.entity.Product;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.repository.CategoryRepository;
import com.pharmacy.drugstore.repository.ProductRepository;
import com.pharmacy.drugstore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
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
                log.info("Seeded demo user id={} email={} name={} role={}",
                        demo.getId(), demo.getEmail(), demo.getName(), demo.getRole());
            }
            if (products.count() > 0) {
                applyInrPrices(products);
                seedLabReports(categories, products);
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
            seedLabReports(categories, products);
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

    private void seedLabReports(CategoryRepository categories, ProductRepository products) {
        Category lab = categories.findByName("Lab Reports").orElseGet(() -> cat(categories, "Lab Reports", "flask"));
        String thyroidImg = "https://images.unsplash.com/photo-1582719471384-894fbb16e074?w=600";
        String kidneyImg = "https://images.unsplash.com/photo-1579684385127-1ef15d508118?w=600";
        String liverImg = "https://images.unsplash.com/photo-1579154204601-01588f351e67?w=600";
        String heartImg = "https://images.unsplash.com/photo-1530026405186-ed1f139313f8?w=600";
        String diabetesImg = "https://images.unsplash.com/photo-1576091160550-2173dba999ef?w=600";
        List<Product> tests = List.of(
                labTest("Thyroid Profile (T3, T4, TSH)",
                        "Complete thyroid panel covering Total T3, Total T4 and TSH to assess thyroid gland function.",
                        "499.00", "799.00", thyroidImg, "PathCare", lab, "THYROID", 4.8, 412),
                labTest("TSH Ultrasensitive",
                        "High-sensitivity TSH test to detect early hypo or hyperthyroidism.",
                        "299.00", "449.00", thyroidImg, "PathCare", lab, "THYROID", 4.7, 286),
                labTest("Free T3 & Free T4",
                        "Free thyroid hormone test for a clearer picture when TSH is abnormal.",
                        "699.00", "999.00", thyroidImg, "PathCare", lab, "THYROID", 4.6, 174),
                labTest("Kidney Function Test (KFT)",
                        "Kidney panel including urea, creatinine, uric acid and electrolytes.",
                        "599.00", "899.00", kidneyImg, "PathCare", lab, "KIDNEY", 4.8, 338),
                labTest("Serum Creatinine",
                        "Creatinine blood test to monitor kidney filtration and chronic kidney disease risk.",
                        "249.00", "399.00", kidneyImg, "PathCare", lab, "KIDNEY", 4.5, 201),
                labTest("Urea & Uric Acid",
                        "Urea and uric acid test useful for kidney health, gout and metabolic follow-up.",
                        "349.00", "549.00", kidneyImg, "PathCare", lab, "KIDNEY", 4.4, 156),
                labTest("Liver Function Test (LFT)",
                        "Liver panel covering bilirubin, SGOT, SGPT, ALP, proteins and albumin.",
                        "699.00", "999.00", liverImg, "PathCare", lab, "LIVER", 4.8, 365),
                labTest("SGPT / ALT",
                        "Alanine aminotransferase test to check liver enzyme elevation.",
                        "249.00", "399.00", liverImg, "PathCare", lab, "LIVER", 4.5, 188),
                labTest("Bilirubin Total & Direct",
                        "Bilirubin test to assess jaundice, bile flow and liver processing.",
                        "299.00", "449.00", liverImg, "PathCare", lab, "LIVER", 4.4, 142),
                labTest("Lipid Profile",
                        "Cholesterol panel: total cholesterol, HDL, LDL, triglycerides and VLDL.",
                        "599.00", "899.00", heartImg, "PathCare", lab, "HEART", 4.9, 521),
                labTest("Cardiac Risk Markers",
                        "Heart risk package with hs-CRP, homocysteine and lipid markers.",
                        "1499.00", "1999.00", heartImg, "PathCare", lab, "HEART", 4.6, 98),
                labTest("ECG + Lipid Combo",
                        "Resting ECG with lipid profile for a combined heart health snapshot.",
                        "899.00", "1299.00", heartImg, "PathCare", lab, "HEART", 4.7, 210),
                labTest("HbA1c (Glycated Hemoglobin)",
                        "3-month average blood sugar test used to diagnose and monitor diabetes.",
                        "449.00", "649.00", diabetesImg, "PathCare", lab, "DIABETES", 4.9, 640),
                labTest("Fasting Blood Sugar",
                        "Fasting glucose test to screen for diabetes and prediabetes.",
                        "149.00", "249.00", diabetesImg, "PathCare", lab, "DIABETES", 4.6, 430),
                labTest("Diabetes Screening Package",
                        "Fasting sugar, HbA1c and postprandial glucose in one screening pack.",
                        "799.00", "1199.00", diabetesImg, "PathCare", lab, "DIABETES", 4.8, 274)
        );
        int added = 0;
        for (Product test : tests) {
            if (!products.existsByName(test.getName())) {
                products.save(test);
                added++;
            }
        }
        if (added > 0) {
            log.info("Seeded lab report tests count={}", added);
        }
    }

    private Product labTest(String name, String desc, String price, String compare, String img,
                            String brand, Category category, String panel, double rating, int reviews) {
        Product p = product(name, desc, price, compare, img, brand, category, false, false, null, rating, reviews);
        p.setLabPanel(panel);
        p.setStock(999);
        return p;
    }
}
