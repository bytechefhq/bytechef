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

package com.bytechef.component.whatsapp.constant;

/**
 * @author Luka Ljubić
 */
public class WhatsAppConstants {

    /**
     * The agent channel key, stored verbatim as the channel type of an agent's channel row. Lowercase, unlike
     * {@link #WHATS_APP}, because that is the key the existing rows already carry; the two must not be conflated.
     */
    public static final String AGENT_CHANNEL_NAME = "whatsapp";

    public static final String APP_SECRET = "appSecret";
    public static final String BODY = "body";
    public static final String CONTACTS = "contacts";
    public static final String GET_MESSAGE = "getMessage";
    public static final String ID = "id";
    public static final String INPUT = "input";
    public static final String MESSAGING_PRODUCT = "messaging_product";
    public static final String MESSAGES = "messages";
    public static final String PHONE_NUMBER_ID = "phoneNumberId";
    public static final String RECEIVE_USER = "to";
    public static final String RECIPIENT_TYPE = "recipient_type";
    public static final String SENDER_NUMBER = "senderNumber";
    public static final String SYSTEM_USER_ACCESS_TOKEN = "systemUserAccessToken";
    public static final String TEXT = "text";
    public static final String TYPE = "type";

    /**
     * The component's own name, which is also the name of its approval-channel cluster element. Note the capital A —
     * the agent channel is keyed by the lowercase {@link #AGENT_CHANNEL_NAME} instead.
     */
    public static final String WHATS_APP = "whatsApp";

    private WhatsAppConstants() {
    }
}
