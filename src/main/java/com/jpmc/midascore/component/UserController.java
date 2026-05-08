package com.jpmc.midascore.component;

import com.jpmc.midascore.foundation.Balance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    private final BalanceService balanceService;

    public UserController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/balance")
    public ResponseEntity<Balance> getBalance(@RequestParam("userId") long id) {
        Balance balance = balanceService.lookup(id);
        if (balance == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(balance);
    }
}
