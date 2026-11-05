/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.tenant;

import java.sql.Connection;
import java.sql.SQLException;
import org.quartz.impl.jdbcjobstore.LockException;
import org.quartz.impl.jdbcjobstore.Semaphore;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class MultiTenantSemaphore implements Semaphore {

    private final Semaphore semaphore;

    public MultiTenantSemaphore(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    boolean wraps(Semaphore otherSemaphore) {
        return semaphore == otherSemaphore;
    }

    @Override
    public boolean obtainLock(Connection conn, String lockName) throws LockException {
        if (!semaphore.requiresConnection()) {
            return semaphore.obtainLock(conn, lockName);
        }

        boolean lockObtained = false;

        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            lockObtained = semaphore.obtainLock(publicSearchPathScope.connection(), lockName);

            return lockObtained;
        } catch (SQLException sqlException) {
            LockException lockException = new LockException(
                "Failure switching search_path for lock " + lockName + ": " + sqlException.getMessage(), sqlException);

            if (lockObtained) {
                try {
                    semaphore.releaseLock(lockName);
                } catch (LockException releaseLockException) {
                    lockException.addSuppressed(releaseLockException);
                }
            }

            throw lockException;
        }
    }

    @Override
    public void releaseLock(String lockName) throws LockException {
        semaphore.releaseLock(lockName);
    }

    @Override
    public boolean requiresConnection() {
        return semaphore.requiresConnection();
    }
}
