package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DatabaseConduit {
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BalanceService balanceService;
    private final RestClient restClient;
    private final TokenBucket outboundBucket;
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConduit.class);

    @Value("${incentive.api.url:http://localhost:8080/incentive}")
    private String incentiveApiUrl;

    public DatabaseConduit(UserRepository userRepository,
                           TransactionRepository transactionRepository,
                           BalanceService balanceService,
                           @org.springframework.beans.factory.annotation.Qualifier("outboundBucket") TokenBucket outboundBucket) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.balanceService = balanceService;
        this.outboundBucket = outboundBucket;
        this.restClient = RestClient.create();
    }

    public void save(UserRecord userRecord) {
        userRepository.save(userRecord);
    }

    public void save(TransactionRecord transactionRecord) {
        transactionRepository.save(transactionRecord);
    }

    public UserRecord getUserById(long id) {
        return userRepository.findById(id);
    }

    public record IncentiveRequest(Long senderId, Long recipientId, float amount) {
    }

    public void processTransaction(com.jpmc.midascore.foundation.Transaction transaction) {
        UserRecord sender = userRepository.findById(transaction.getSenderId());
        UserRecord recipient = userRepository.findById(transaction.getRecipientId());

        logger.info("method called: processing transaction for {} to {}", transaction.getSenderId(), transaction.getRecipientId());
        
        if (sender != null && recipient != null && sender.getBalance() >= transaction.getAmount()) {
            sender.setBalance(sender.getBalance() - transaction.getAmount());
            recipient.setBalance(recipient.getBalance() + transaction.getAmount());

            userRepository.save(sender);
            userRepository.save(recipient);
            balanceService.evict(sender.getId());
            balanceService.evict(recipient.getId());

            TransactionRecord transcriptRecord = new TransactionRecord(sender, recipient, transaction.getAmount());
            transactionRepository.save(transcriptRecord);

            if (!outboundBucket.tryAcquire()) {
                logger.warn("Outbound rate limit hit — skipping incentive API call for {}->{}",
                        sender.getId(), recipient.getId());
            } else {
                try {
                    IncentiveRequest requestBody = new IncentiveRequest(
                            sender.getId(),
                            recipient.getId(),
                            transaction.getAmount());

                    Incentive incentive = restClient.post()
                            .uri(incentiveApiUrl)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(requestBody)
                            .retrieve()
                            .body(Incentive.class);

                    if (incentive != null && incentive.getAmount() > 0) {
                        recipient.setBalance(recipient.getBalance() + incentive.getAmount());
                        userRepository.save(recipient);
                        balanceService.evict(recipient.getId());
                        logger.info("Incentive {} credited to recipient {}", incentive.getAmount(), recipient.getId());
                    } else {
                        logger.info("No incentive returned for {}->{}", sender.getId(), recipient.getId());
                    }
                } catch (Exception e) {
                    logger.error("Failed to call incentive API: {}", e.getMessage());
                }
            }
        } else {
            logger.warn("Transaction failed: sender={} or recipient={} is null or insufficient funds", transaction.getSenderId(), transaction.getRecipientId());
        }
    }
}