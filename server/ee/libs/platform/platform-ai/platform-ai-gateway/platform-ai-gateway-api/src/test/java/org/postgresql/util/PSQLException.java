/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package org.postgresql.util;

/**
 * Test-only stub of {@code org.postgresql.util.PSQLException}. The reflective probe in
 * {@code com.bytechef.ee.platform.ai.gateway.util.AiGatewayConstraintMatchers} dispatches on
 * {@code getClass().getName()} — placing this stub in the same FQN lets unit tests exercise the structured-constraint
 * path without pulling the Postgres JDBC driver onto the unit-test classpath.
 *
 * <p>
 * Production callers see a {@code DuplicateKeyException} whose cause chain reaches a real {@code PSQLException}; the
 * end-to-end IntTest in {@code AiObservabilityOtlpIngestFacadeIntTest} covers that wiring against a real Postgres
 * container. This stub is the test-time stand-in that makes the reflective probe directly assertable.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class PSQLException extends RuntimeException {

    private final ServerErrorMessage serverErrorMessage;

    public PSQLException(String message, ServerErrorMessage serverErrorMessage) {
        super(message);
        this.serverErrorMessage = serverErrorMessage;
    }

    public ServerErrorMessage getServerErrorMessage() {
        return serverErrorMessage;
    }
}
