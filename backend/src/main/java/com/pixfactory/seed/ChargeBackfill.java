package com.pixfactory.seed;

import com.pixfactory.service.ChargeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class ChargeBackfill implements CommandLineRunner {
    private final ChargeService chargeService;

    public ChargeBackfill(ChargeService chargeService) {
        this.chargeService = chargeService;
    }

    @Override
    public void run(String... args) {
        chargeService.ensureSchedulesForAllContracts();
        chargeService.refreshOverdueAndPromises();
    }
}
