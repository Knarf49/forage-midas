package com.jpmc.midascore.config;

import com.jpmc.midascore.component.RateLimitInterceptor;
import com.jpmc.midascore.component.TokenBucket;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    @Value("${ratelimit.inbound.capacity:20}")
    private double inboundCapacity;

    @Value("${ratelimit.inbound.refill-per-second:10}")
    private double inboundRefill;

    @Value("${ratelimit.outbound.capacity:10}")
    private double outboundCapacity;

    @Value("${ratelimit.outbound.refill-per-second:5}")
    private double outboundRefill;

    @Bean
    @Qualifier("inboundBucket")
    public TokenBucket inboundBucket() {
        return new TokenBucket(inboundCapacity, inboundRefill);
    }

    @Bean
    @Qualifier("outboundBucket")
    public TokenBucket outboundBucket() {
        return new TokenBucket(outboundCapacity, outboundRefill);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(inboundBucket()))
                .addPathPatterns("/balance");
    }
}
