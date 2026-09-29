package com.zovira.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zovira.catalog.dto.BrandResponse;
import com.zovira.catalog.dto.CategoryNode;
import com.zovira.catalog.dto.HomeResponse;
import com.zovira.catalog.dto.ProductDetailResponse;
import com.zovira.settings.dto.PlatformSettings;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Read-heavy catalog data is cached. Each cache has an explicit value type so Redis entries are
 * plain JSON (no polymorphic type metadata), which keeps them debuggable and safe to deserialize.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CATEGORY_TREE = "categoryTree";
    public static final String BRANDS = "brands";
    public static final String HOME = "home";
    public static final String PRODUCT_DETAIL = "productDetail";
    public static final String SETTINGS = "platformSettings";

    private record Spec(Duration ttl, long maxSize, JavaType type) {
    }

    private static Map<String, Spec> specs() {
        TypeFactory tf = TypeFactory.defaultInstance();
        Map<String, Spec> specs = new HashMap<>();
        specs.put(CATEGORY_TREE, new Spec(Duration.ofMinutes(30), 10,
                tf.constructCollectionType(List.class, CategoryNode.class)));
        specs.put(BRANDS, new Spec(Duration.ofMinutes(30), 10,
                tf.constructCollectionType(List.class, BrandResponse.class)));
        specs.put(HOME, new Spec(Duration.ofMinutes(5), 10, tf.constructType(HomeResponse.class)));
        specs.put(PRODUCT_DETAIL, new Spec(Duration.ofMinutes(10), 5_000,
                tf.constructType(ProductDetailResponse.class)));
        specs.put(SETTINGS, new Spec(Duration.ofMinutes(10), 1, tf.constructType(PlatformSettings.class)));
        return specs;
    }

    @Bean
    @ConditionalOnProperty(name = "zovira.redis.enabled", havingValue = "false", matchIfMissing = true)
    CacheManager localCacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(specs().entrySet().stream()
                .map(e -> new CaffeineCache(e.getKey(), Caffeine.newBuilder()
                        .expireAfterWrite(e.getValue().ttl())
                        .maximumSize(e.getValue().maxSize())
                        .recordStats()
                        .build()))
                .toList());
        return manager;
    }

    @Bean
    @ConditionalOnProperty(name = "zovira.redis.enabled", havingValue = "true")
    CacheManager redisCacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        ObjectMapper mapper = objectMapper.copy();
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        specs().forEach((name, spec) -> configs.put(name, RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("zovira:")
                .entryTtl(spec.ttl())
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(mapper, spec.type())))));
        return RedisCacheManager.builder(connectionFactory)
                .withInitialCacheConfigurations(configs)
                .disableCreateOnMissingCache()
                .build();
    }
}
