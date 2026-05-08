package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Balance;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class BalanceService {
    private static final Logger logger = LoggerFactory.getLogger(BalanceService.class);
    private final UserRepository userRepository;

    public BalanceService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Cacheable(value = "balance", key = "#id", unless = "#result == null")
    public Balance lookup(long id) {
        logger.info("Cache miss — DB lookup for userId={}", id);
        UserRecord user = userRepository.findById(id);
        if (user == null) {
            return null;
        }
        return new Balance(user.getBalance());
    }

    @CacheEvict(value = "balance", key = "#id")
    public void evict(long id) {
        logger.debug("Evicted balance cache for userId={}", id);
    }
}
