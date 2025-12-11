package com.exam.eventhub.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RememberMeTokenCleanupScheduler {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public RememberMeTokenCleanupScheduler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanExpiredTokens() {
        int deleted = jdbcTemplate.update("DELETE FROM persistent_logins WHERE last_used < (NOW() - INTERVAL 30 DAY)");

        if (deleted > 0) {
            log.info("Remember-Me cleanup job: {} old tokens removed.", deleted);
        } else {
            log.info("No expired Remember-Me tokens found.");
        }

    }
}
