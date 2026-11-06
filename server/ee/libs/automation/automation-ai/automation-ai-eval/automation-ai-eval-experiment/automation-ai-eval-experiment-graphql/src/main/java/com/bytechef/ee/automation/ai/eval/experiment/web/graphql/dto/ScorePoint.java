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
 * @author Ivica Cardic
 * @version ee
 */
public record ScorePoint(String name, BigDecimal value, String stringValue, String dataType) {

    public ScorePoint {
        Validate.notBlank(name, "name must not be blank");
        Validate.notBlank(dataType, "dataType must not be blank");
    }
}
