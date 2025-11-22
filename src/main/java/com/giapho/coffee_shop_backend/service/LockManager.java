package com.giapho.coffee_shop_backend.service;

import java.util.concurrent.TimeUnit;

/**
 * Interface for managing distributed locks to prevent concurrent operations.
 */
public interface LockManager {
    /**
     * Attempts to acquire a lock with the specified key.
     *
     * @param lockKey the key to lock on
     * @param timeout the maximum time to wait for the lock
     * @param timeUnit the time unit of the timeout argument
     * @return true if the lock was acquired, false otherwise
     */
    boolean acquireLock(String lockKey, long timeout, TimeUnit timeUnit);

    /**
     * Releases the lock with the specified key.
     *
     * @param lockKey the key of the lock to release
     */
    void releaseLock(String lockKey);
}
