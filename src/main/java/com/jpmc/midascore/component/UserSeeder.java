package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "seed.enabled", havingValue = "true")
public class UserSeeder implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(UserSeeder.class);

    private final UserRepository userRepository;
    private final ResourceLoader resourceLoader;

    @Value("${seed.users.path:classpath:seed/users.csv}")
    private String seedPath;

    public UserSeeder(UserRepository userRepository, ResourceLoader resourceLoader) {
        this.userRepository = userRepository;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            logger.info("Seed skipped: users already exist ({} rows)", userRepository.count());
            return;
        }
        Resource resource = resourceLoader.getResource(seedPath);
        if (!resource.exists()) {
            logger.warn("Seed file not found at {}", seedPath);
            return;
        }
        int loaded = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",");
                if (parts.length < 2) continue;
                String name = parts[0].trim();
                float balance = Float.parseFloat(parts[1].trim());
                UserRecord saved = userRepository.save(new UserRecord(name, balance));
                logger.info("Seeded user id={} name={} balance={}", saved.getId(), name, balance);
                loaded++;
            }
        }
        logger.info("Seed complete: {} users loaded", loaded);
    }
}
