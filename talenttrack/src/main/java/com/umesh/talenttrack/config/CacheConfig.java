package com.umesh.talenttrack.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    // Redis cache config is automatically set up by Spring Boot's cache starter using the application properties.
}
