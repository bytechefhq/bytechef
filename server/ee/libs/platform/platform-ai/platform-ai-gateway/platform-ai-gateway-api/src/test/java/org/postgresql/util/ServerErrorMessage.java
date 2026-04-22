/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package org.postgresql.util;

/**
 * Test-only stub of {@code org.postgresql.util.ServerErrorMessage}. Lives in the matching package so the reflective
 * {@code getServerErrorMessage().getConstraint()} probe in
 * {@code com.bytechef.ee.platform.ai.gateway.util.AiGatewayConstraintMatchers} resolves the same accessors as the
 * production driver class — without pulling the real Postgres JDBC driver onto the unit-test classpath.
 *
 * <p>
 * The production class returns null when the field is absent; this stub mirrors that contract by accepting null in the
 * constructor.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class ServerErrorMessage {

    private final String constraint;

    public ServerErrorMessage(String constraint) {
        this.constraint = constraint;
    }

    public String getConstraint() {
        return constraint;
    }
}
