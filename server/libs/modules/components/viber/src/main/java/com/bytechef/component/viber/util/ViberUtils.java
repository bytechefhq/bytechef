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

package com.bytechef.component.viber.util;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.viber.constant.ViberConstants;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class ViberUtils {

    private ViberUtils() {
    }

    public static Map<String, Object> checkStatus(Map<String, Object> response) {
        Object status = response.get("status");

        if (status instanceof Number number && number.intValue() == 0) {
            return response;
        }

        throw new ProviderException(
            "Viber rejected the request: %s (%d)".formatted(
                response.get("status_message"), status instanceof Number number ? number.intValue() : -1));
    }

    public static Map<String, Object> getAccountInfo(Context context) {
        Map<String, Object> response = context
            .http(http -> http.post("/get_account_info"))
            .body(Http.Body.of(Map.of()))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return checkStatus(response);
    }

    public static Map<String, Object> buildSender(Parameters inputParameters, String senderNameKey, Context context) {
        Map<String, Object> sender = new HashMap<>();

        sender.put("name", resolveSenderName(inputParameters.getString(senderNameKey), context));

        String senderAvatar = inputParameters.getString(ViberConstants.SENDER_AVATAR);

        if (senderAvatar != null && !senderAvatar.isBlank()) {
            sender.put("avatar", senderAvatar);
        }

        return sender;
    }

    public static String resolveSenderName(@Nullable String senderName, Context context) {
        String name = senderName;

        if (name == null || name.isBlank()) {
            Map<String, Object> accountInfo = getAccountInfo(context);

            name = accountInfo.get("name") instanceof String botName ? botName : null;
        }

        if (name == null || name.isBlank()) {
            throw new ProviderException("Viber did not return a bot name; set a sender name explicitly.");
        }

        if (name.length() > ViberConstants.SENDER_NAME_MAX_LENGTH) {
            return name.substring(0, ViberConstants.SENDER_NAME_MAX_LENGTH);
        }

        return name;
    }
}
