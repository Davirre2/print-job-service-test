package com.adobe.printservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides a single worker lock across application instances.
 *
 * In production PostgreSQL's transaction-scoped advisory lock is used. The lock is held
 * until the transaction that acquired it completes, which allows the worker to keep the
 * lock for the entire render operation without creating a database table just for locking.
 *
 * A local lock strategy is also supported for H2/unit tests because H2 does not implement
 * PostgreSQL advisory locks.
 */
@Service
public class WorkerLockService {

    private static final long POSTGRES_LOCK_KEY = 728346192L;

    private final JdbcTemplate jdbcTemplate;
    private final String strategy;
    private final ReentrantLock localLock = new ReentrantLock();

    public WorkerLockService(
            JdbcTemplate jdbcTemplate,
            @Value("${worker.lock.strategy:postgres-advisory}") String strategy) {
        this.jdbcTemplate = jdbcTemplate;
        this.strategy = strategy;
    }

    public Optional<LockHandle> tryAcquire() {
        if ("local".equalsIgnoreCase(strategy)) {
            if (!localLock.tryLock()) {
                return Optional.empty();
            }
            return Optional.of(localLock::unlock);
        }

        Boolean acquired = jdbcTemplate.queryForObject(
                "SELECT pg_try_advisory_xact_lock(?)",
                Boolean.class,
                POSTGRES_LOCK_KEY
        );

        if (Boolean.TRUE.equals(acquired)) {
            // PostgreSQL releases transaction-scoped advisory locks automatically when
            // the surrounding Spring transaction commits or rolls back.
            return Optional.of(() -> { });
        }

        return Optional.empty();
    }

    @FunctionalInterface
    public interface LockHandle extends AutoCloseable {
        @Override
        void close();
    }
}
