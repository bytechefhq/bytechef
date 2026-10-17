/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

/**
 * Gates AI Hub beans on both the hub and the copilot toggles.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({
    ElementType.TYPE, ElementType.METHOD
})
@ConditionalOnExpression("${bytechef.ai.hub.enabled:false} and ${bytechef.ai.copilot.enabled:false}")
public @interface ConditionalOnAiHub {
}
