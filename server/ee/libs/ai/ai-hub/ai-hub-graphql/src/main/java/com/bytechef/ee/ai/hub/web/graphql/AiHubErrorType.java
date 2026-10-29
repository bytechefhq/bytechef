/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import graphql.ErrorClassification;

/**
 * Error classifications the AI Hub emits that Spring GraphQL's {@code ErrorType} has no value for.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
enum AiHubErrorType implements ErrorClassification {

    CONFLICT
}
