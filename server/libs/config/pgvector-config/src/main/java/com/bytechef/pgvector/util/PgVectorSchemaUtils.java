/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.pgvector.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public final class PgVectorSchemaUtils {

    private static final Logger log = LoggerFactory.getLogger(PgVectorSchemaUtils.class);

    static final int MAX_ATTEMPTS = 5;

    private static final Set<String> CONCURRENT_CREATION_SQL_STATES = Set.of("23505", "42P07", "42710");
    private static final long MAX_PAUSE_MILLIS = 500;
    private static final long MIN_PAUSE_MILLIS = 100;

    private PgVectorSchemaUtils() {
    }

    public static void initializeSchema(Runnable schemaInitializer) {
        for (int attempt = 1;; attempt++) {
            try {
                schemaInitializer.run();

                return;
            } catch (RuntimeException runtimeException) {
                if (attempt == MAX_ATTEMPTS || !isConcurrentCreation(runtimeException)) {
                    throw runtimeException;
                }

                log.info(
                    "Another node created part of the pgvector schema at the same time, initializing it again " +
                        "(attempt {} of {})",
                    attempt + 1, MAX_ATTEMPTS);

                pause();
            }
        }
    }

    private static boolean isConcurrentCreation(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException &&
                CONCURRENT_CREATION_SQL_STATES.contains(sqlException.getSQLState())) {

                return true;
            }
        }

        return false;
    }

    @SuppressFBWarnings("PREDICTABLE_RANDOM")
    private static void pause() {
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();

        try {
            Thread.sleep(threadLocalRandom.nextLong(MIN_PAUSE_MILLIS, MAX_PAUSE_MILLIS + 1));
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException("Interrupted while initializing the pgvector schema", interruptedException);
        }
    }
}
