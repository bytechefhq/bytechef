/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AiHubChatDeletedEvent(long chatId, String threadId) {
}
