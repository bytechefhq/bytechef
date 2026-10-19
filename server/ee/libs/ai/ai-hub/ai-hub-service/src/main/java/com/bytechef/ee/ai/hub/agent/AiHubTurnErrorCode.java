/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

/**
 * Codes the AI Hub turn endpoint answers in its {@code {"error": ...}} body when it refuses a turn, so the client can
 * show a dedicated message.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiHubTurnErrorCode {

    CHAT_NOT_FOUND,
    TURN_IN_FLIGHT
}
