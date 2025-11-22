package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.service.LockManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An in-memory implementation of the {@link LockManager} interface.
 * This implementation uses a {@link ConcurrentHashMap} to store locks.
 */
@Slf4j
@Service
public class InMemoryLockManager implements LockManager {

    private final Map<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    @Override
    public boolean acquireLock(String lockKey, long timeout, TimeUnit timeUnit) {
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock(true));
        try {
            boolean acquired = lock.tryLock(timeout, timeUnit);
            if (!acquired) {
                log.debug("Failed to acquire lock for key: {}", lockKey);
                return false;
            }
            log.debug("Acquired lock for key: {}", lockKey);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread interrupted while trying to acquire lock for key: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public void releaseLock(String lockKey) {
        ReentrantLock lock = lockMap.get(lockKey);
        if (lock != null && lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
                log.debug("Released lock for key: {}", lockKey);
            } catch (IllegalMonitorStateException e) {
                log.warn("Attempted to release lock not held by current thread for key: {}", lockKey);
            }
        }
    }
}
