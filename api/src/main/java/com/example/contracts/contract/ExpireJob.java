package com.example.contracts.contract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpireJob {
    private static final Logger log = LoggerFactory.getLogger(ExpireJob.class);
    private final ContractService contracts;

    public ExpireJob(ContractService contracts) {
        this.contracts = contracts;
    }

    @Scheduled(cron = "0 10 0 * * *")
    public void expire() {
        int n = contracts.expireOverdue();
        if (n > 0) {
            log.info("expired {} contracts", n);
        }
    }
}
