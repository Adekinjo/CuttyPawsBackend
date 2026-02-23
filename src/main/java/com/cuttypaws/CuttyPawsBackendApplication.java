package com.cuttypaws;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@SpringBootApplication
public class CuttyPawsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CuttyPawsBackendApplication.class, args);
    }

    @Bean
    public CommandLineRunner clearRedisCache(RedisConnectionFactory connectionFactory) {
        return args -> {
            try {
                // This command sends the FLUSHALL signal to your cloud Redis
                connectionFactory.getConnection().serverCommands().flushAll();
                System.out.println("✅ SUCCESS: Redis cache has been cleared!");
            } catch (Exception e) {
                System.err.println("❌ FAILED to clear Redis: " + e.getMessage());
            }
        };
    }
}
