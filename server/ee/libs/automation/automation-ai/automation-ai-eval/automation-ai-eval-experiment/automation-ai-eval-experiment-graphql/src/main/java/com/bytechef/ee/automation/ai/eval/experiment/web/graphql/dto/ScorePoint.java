/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto;

import java.math.BigDecimal;
import org.apache.commons.lang3.Validate;

/**
 * Flat projection of a single {@code AiEvalScore} — keeps the GraphQL surface stable even if the storage model changes.
 * {@code dataType} is serialized as a string so clients don't need the server-side enum.
 *
 * <p>
 * The compact constructor enforces that {@code name} and {@code dataType} are non-blank (matching the GraphQL schema's
 * {@code String!}), but tolerates a null {@code value}/{@code stringValue} combination since legacy scores persisted
 * before {@code applyTypedValue} was the sole entry point may not have a populated typed field. Cross-field validation
 * belongs upstream where the {@code AiEvalScore} domain enforces it.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record ScorePoint(String name, BigDecimal value, String stringValue, String dataType) {

    public ScorePoint {
        Validate.notBlank(name, "name must not be blank");
        Validate.notBlank(dataType, "dataType must not be blank");
    }
}
