/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion;

/**
 * The kinds of environment-scoped automation resources that can be promoted from one environment to another (for
 * example, from {@code DEVELOPMENT} to {@code STAGING}). One
 * {@link com.bytechef.ee.automation.promotion.handler.EnvironmentPromotionHandler} implementation exists per resource
 * type.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum PromotionResourceType {

    API_COLLECTION, MCP_SERVER, A2A_SERVER, PROJECT_DEPLOYMENT
}
