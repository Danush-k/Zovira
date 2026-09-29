package com.zovira.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zovira.catalog.entity.Brand;
import com.zovira.catalog.entity.Category;
import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductAttribute;
import com.zovira.catalog.entity.ProductImage;
import com.zovira.catalog.entity.ProductSpecification;
import com.zovira.catalog.entity.ProductVariant;
import com.zovira.catalog.repository.BrandRepository;
import com.zovira.catalog.repository.CategoryRepository;
import com.zovira.catalog.repository.ProductRepository;
import com.zovira.inventory.entity.Inventory;
import com.zovira.inventory.repository.InventoryRepository;
import com.zovira.review.entity.Review;
import com.zovira.review.entity.ReviewStatus;
import com.zovira.review.repository.ReviewRepository;
import com.zovira.seed.SeedModels.CatalogSeed;
import com.zovira.seed.SeedModels.CategorySeed;
import com.zovira.seed.SeedModels.PeopleSeed;
import com.zovira.seed.SeedModels.ProductSeed;
import com.zovira.seed.SeedModels.ReviewSeed;
import com.zovira.seed.SeedModels.SellerSeed;
import com.zovira.seed.SeedModels.VariantSeed;
import com.zovira.seller.entity.Seller;
import com.zovira.seller.repository.SellerRepository;
import com.zovira.storage.FileKind;
import com.zovira.storage.StorageService;
import com.zovira.user.entity.Address;
import com.zovira.user.entity.AddressType;
import com.zovira.user.entity.Role;
import com.zovira.user.entity.User;
import com.zovira.user.repository.AddressRepository;
import com.zovira.user.repository.RoleRepository;
import com.zovira.user.repository.UserRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds people (admin, sellers, customers with addresses), the category tree, brands, 175
 * products with variants, stock, images and specifications, and customer reviews.
 */
@Component
@Order(10)
class CatalogSeedStep implements SeedStep {

    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository users;
    private final RoleRepository roles;
    private final AddressRepository addresses;
    private final SellerRepository sellers;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final ProductRepository products;
    private final InventoryRepository inventory;
    private final ReviewRepository reviews;
    private final StorageService storage;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final Map<String, String> uploadedMedia = new HashMap<>();

    CatalogSeedStep(ObjectMapper objectMapper, PasswordEncoder passwordEncoder, UserRepository users,
            RoleRepository roles, AddressRepository addresses, SellerRepository sellers,
            CategoryRepository categories, BrandRepository brands, ProductRepository products,
            InventoryRepository inventory, ReviewRepository reviews, StorageService storage, JdbcTemplate jdbc,
            Clock clock) {
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.users = users;
        this.roles = roles;
        this.addresses = addresses;
        this.sellers = sellers;
        this.categories = categories;
        this.brands = brands;
        this.products = products;
        this.inventory = inventory;
        this.reviews = reviews;
        this.storage = storage;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public String name() {
        return "people, catalog and reviews";
    }

    @Override
    public boolean shouldRun() {
        return users.count() == 0;
    }

    @Override
    public void run() {
        PeopleSeed people = read("seed/people.json", PeopleSeed.class);
        CatalogSeed catalog = read("seed/catalog.json", CatalogSeed.class);
        Instant now = clock.instant();

        Role customerRole = roles.findByName(Role.CUSTOMER).orElseThrow();
        Role sellerRole = roles.findByName(Role.SELLER).orElseThrow();
        Role adminRole = roles.findByName(Role.ADMIN).orElseThrow();
        Map<String, String> hashes = new HashMap<>();

        // People -------------------------------------------------------------------------------
        User admin = user(people.admin().fullName(), people.admin().email(), people.admin().password(), hashes,
                customerRole, adminRole);
        backdate("users", admin.getId(), now.minus(Duration.ofDays(420)));

        List<User> customers = new ArrayList<>();
        for (int i = 0; i < people.customers().size(); i++) {
            var c = people.customers().get(i);
            User user = user(c.fullName(), c.email(), people.customerPassword(), hashes, customerRole);
            if (c.line1() != null) {
                user.setPhone(c.phone());
                Address address = new Address(user);
                address.setFullName(c.fullName());
                address.setPhone(c.phone());
                address.setLine1(c.line1());
                address.setLine2(c.line2());
                address.setCity(c.city());
                address.setState(c.state());
                address.setPincode(c.pincode());
                address.setType(AddressType.HOME);
                address.setDefaultAddress(true);
                addresses.save(address);
            }
            customers.add(user);
            backdate("users", user.getId(), now.minus(Duration.ofDays(8 + (long) i * 7)));
        }

        Map<String, Seller> sellersByKey = new HashMap<>();
        int s = 0;
        for (SellerSeed seed : catalog.sellers()) {
            User owner = user(seed.fullName(), seed.email(), people.sellerPassword(), hashes, customerRole, sellerRole);
            owner.setPhone(seed.phone());
            Seller seller = new Seller(owner, seed.storeName(), seed.key());
            applySeller(seller, seed.description(), seed.gstin(), seed.email(), seed.phone(), seed.line1(),
                    seed.city(), seed.state(), seed.pincode());
            seller.approve(now.minus(Duration.ofDays(300 - s * 20L)));
            sellersByKey.put(seed.key(), sellers.save(seller));
            backdate("users", owner.getId(), now.minus(Duration.ofDays(310 - s * 20L)));
            s++;
        }
        var pending = people.pendingSeller();
        User applicant = user(pending.fullName(), pending.email(), people.sellerPassword(), hashes, customerRole);
        Seller pendingSeller = new Seller(applicant, pending.storeName(), "artisan-loom");
        applySeller(pendingSeller, pending.description(), pending.gstin(), pending.email(), pending.phone(),
                pending.line1(), pending.city(), pending.state(), pending.pincode());
        sellers.save(pendingSeller);

        // Taxonomy -----------------------------------------------------------------------------
        Map<String, Category> categoryBySlug = new LinkedHashMap<>();
        int order = 0;
        for (CategorySeed top : catalog.categories()) {
            Category parent = category(top, null, order++);
            categoryBySlug.put(top.slug(), parent);
            int childOrder = 0;
            for (CategorySeed child : top.children()) {
                categoryBySlug.put(child.slug(), category(child, parent, childOrder++));
            }
        }
        Map<String, Brand> brandByName = new HashMap<>();
        for (var b : catalog.brands()) {
            Brand brand = new Brand(b.name(), b.slug());
            brand.setFeatured(b.featured());
            brandByName.put(b.name(), brands.save(brand));
        }

        // Products -----------------------------------------------------------------------------
        List<Object[]> counters = new ArrayList<>();
        List<Object[]> published = new ArrayList<>();
        Map<Long, List<Integer>> ratingsBySeller = new HashMap<>();
        int index = 0;
        for (ProductSeed seed : catalog.products()) {
            Category category = Objects.requireNonNull(categoryBySlug.get(seed.category()), seed.category());
            Seller seller = Objects.requireNonNull(sellersByKey.get(seed.seller()), seed.seller());
            Product product = new Product(seller, category, brandByName.get(seed.brand()), seed.title(), seed.slug());
            product.setShortDescription(seed.shortDescription());
            product.setDescription(seed.description());
            product.setHighlights(seed.highlights());
            product.setTags(seed.tags());
            product.setWarranty(seed.warranty());
            product.setReturnable(seed.returnable());
            product.setReturnWindowDays(seed.returnWindowDays());
            product.setCodAvailable(seed.cod());
            product.setFeatured(Boolean.TRUE.equals(seed.featured()));
            product.setSearchKeywords(keywords(seed, category));
            if (seed.model() != null) {
                product.setModelUrl(seed.model().url());
                product.setModelPosterUrl(media(seed.model().poster()));
            }

            int position = 0;
            for (var attr : seed.attributes()) {
                product.addAttribute(new ProductAttribute(attr.name(), position++, attr.options()));
            }
            position = 0;
            Map<String, ProductVariant> firstVariantByColour = new HashMap<>();
            for (VariantSeed v : seed.variants()) {
                ProductVariant variant = new ProductVariant(v.sku(), v.name(), v.price(), v.mrp());
                variant.setOptions(v.options());
                variant.setDefaultVariant(v.isDefault());
                variant.setPosition(position++);
                product.addVariant(variant);
                String colour = v.options().get("Colour");
                if (colour != null) {
                    firstVariantByColour.putIfAbsent(colour, variant);
                }
            }
            position = 0;
            if (seed.variantImages() != null && !seed.variantImages().isEmpty()) {
                for (var entry : seed.variantImages().entrySet()) {
                    for (String url : entry.getValue()) {
                        ProductImage image = new ProductImage(media(url), seed.title() + ", " + entry.getKey(),
                                position++);
                        image.setVariant(firstVariantByColour.get(entry.getKey()));
                        product.addImage(image);
                    }
                }
            } else {
                for (String url : seed.images()) {
                    product.addImage(new ProductImage(media(url), seed.title(), position++));
                }
            }
            position = 0;
            for (List<String> spec : seed.specs()) {
                product.addSpecification(new ProductSpecification(spec.get(0), spec.get(1), spec.get(2), position++));
            }
            product.refreshPricing();
            product.setTotalStock(seed.variants().stream().mapToInt(VariantSeed::stock).sum());
            product.publish(now);
            products.save(product);

            for (int i = 0; i < seed.variants().size(); i++) {
                inventory.save(new Inventory(product.getVariants().get(i), seed.variants().get(i).stock()));
            }

            // Reviews and denormalized rating
            int sum = 0;
            for (ReviewSeed r : seed.reviews()) {
                Review review = new Review(product, customers.get(r.customer() % customers.size()), r.rating(),
                        r.title(), r.body(), false, ReviewStatus.PUBLISHED);
                reviews.save(review);
                backdate("reviews", review.getId(), now.minus(Duration.ofDays(r.daysAgo())));
                sum += r.rating();
                ratingsBySeller.computeIfAbsent(seller.getId(), k -> new ArrayList<>()).add(r.rating());
            }
            if (!seed.reviews().isEmpty()) {
                product.updateRating(BigDecimal.valueOf(sum)
                        .divide(BigDecimal.valueOf(seed.reviews().size()), 2, RoundingMode.HALF_UP), seed.reviews().size());
            }

            counters.add(new Object[] {seed.soldCount() == null ? 0 : seed.soldCount(),
                    seed.viewCount() == null ? 0 : seed.viewCount(), product.getId()});
            Instant publishedAt = now.minus(Duration.ofDays(3 + (index * 37L) % 160));
            published.add(new Object[] {Timestamp.from(publishedAt), Timestamp.from(publishedAt), product.getId()});
            index++;
        }

        // Category tiles use a representative product image
        for (Category category : categoryBySlug.values()) {
            catalog.products().stream()
                    .filter(p -> p.category().equals(category.getSlug())
                            || (categoryBySlug.get(p.category()).getParent() != null
                                    && categoryBySlug.get(p.category()).getParent().getSlug().equals(category.getSlug())))
                    .filter(p -> Boolean.TRUE.equals(p.featured()) || p.images().size() > 1)
                    .findFirst()
                    .or(() -> catalog.products().stream().filter(p -> p.category().equals(category.getSlug())).findFirst())
                    .ifPresent(p -> category.setImageUrl(media(p.images().getFirst())));
        }

        ratingsBySeller.forEach((sellerId, ratings) -> sellers.findById(sellerId).ifPresent(seller -> seller.updateRating(
                BigDecimal.valueOf(ratings.stream().mapToInt(Integer::intValue).average().orElse(0))
                        .setScale(2, RoundingMode.HALF_UP), ratings.size())));

        products.flush();
        jdbc.batchUpdate("UPDATE products SET sold_count = ?, view_count = ? WHERE id = ?", counters);
        jdbc.batchUpdate("UPDATE products SET published_at = ?, created_at = ? WHERE id = ?", published);
    }

    private User user(String fullName, String email, String password, Map<String, String> hashes, Role... grants) {
        User user = new User(email, hashes.computeIfAbsent(password, passwordEncoder::encode), fullName);
        user.setEmailVerified(true);
        user.getRoles().addAll(List.of(grants));
        return users.save(user);
    }

    private static void applySeller(Seller seller, String description, String gstin, String email, String phone,
            String line1, String city, String state, String pincode) {
        seller.setDescription(description);
        seller.setGstin(gstin);
        seller.setSupportEmail(email);
        seller.setSupportPhone(phone);
        seller.setPickupLine1(line1);
        seller.setPickupCity(city);
        seller.setPickupState(state);
        seller.setPickupPincode(pincode);
    }

    private Category category(CategorySeed seed, Category parent, int sortOrder) {
        Category category = new Category(seed.name(), seed.slug());
        category.setParent(parent);
        category.setIcon(seed.icon());
        category.setDescription(seed.description());
        category.setTaxRate(seed.taxRate() == null ? new BigDecimal("18") : seed.taxRate());
        category.setSortOrder(sortOrder);
        return categories.save(category);
    }

    /** Brand and category names are indexed alongside the title so "iphone smartphone" finds phones. */
    private static String keywords(ProductSeed seed, Category category) {
        List<String> parts = new ArrayList<>();
        parts.add(seed.brand());
        parts.add(category.getName());
        if (category.getParent() != null) {
            parts.add(category.getParent().getName());
        }
        if (seed.tags() != null) {
            parts.add(seed.tags().replace(',', ' '));
        }
        return String.join(" ", parts);
    }

    /** {@code seed:name.jpg} references are uploaded once to object storage; other URLs pass through. */
    private String media(String reference) {
        if (reference == null || !reference.startsWith("seed:")) {
            return reference;
        }
        return uploadedMedia.computeIfAbsent(reference, ref -> {
            try (InputStream in = new ClassPathResource("seed/media/" + ref.substring(5)).getInputStream()) {
                return storage.store(in.readAllBytes(), "products", FileKind.IMAGE).url();
            } catch (IOException e) {
                throw new UncheckedIOException("Missing seed media " + ref, e);
            }
        });
    }

    private void backdate(String table, Long id, Instant when) {
        jdbc.update("UPDATE " + table + " SET created_at = ? WHERE id = ?", Timestamp.from(when), id);
    }

    private <T> T read(String path, Class<T> type) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + path, e);
        }
    }
}
