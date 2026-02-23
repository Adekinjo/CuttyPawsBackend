package com.cuttypaws.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class RedisCacheConfig {

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("🔧 Configuring RedisTemplate...");

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.setEnableTransactionSupport(true);
        template.afterPropertiesSet();

        log.info("✅ RedisTemplate configured successfully");
        return template;
    }

    @Bean
    @Primary
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("🚀 Initializing Redis Cache Manager...");

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .prefixCacheNameWith("beauthrist:");

        // Cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Product caches
        cacheConfigs.put("products", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put("productById", defaultConfig.entryTtl(Duration.ofHours(2)));

        // Category caches
        cacheConfigs.put("allCategories", defaultConfig.entryTtl(Duration.ofHours(4)));
        cacheConfigs.put("categoryById", defaultConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigs.put("categoryWithSubCategories", defaultConfig.entryTtl(Duration.ofHours(4)));
        cacheConfigs.put("categorySearch", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // SubCategory caches
        cacheConfigs.put("allSubCategories", defaultConfig.entryTtl(Duration.ofHours(4)));
        cacheConfigs.put("subCategoryById", defaultConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigs.put("subCategorySearch", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put("subCategoriesByCategory", defaultConfig.entryTtl(Duration.ofHours(3)));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();

        log.info("✅ Redis Cache Manager initialized with {} cache configurations", cacheConfigs.size());
        return cacheManager;
    }
}
