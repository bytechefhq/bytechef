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

package com.bytechef.component.email.commons;

import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.email.constant.EmailConstants.CRYPTOGRAPHIC_PROTOCOL;
import static com.bytechef.component.email.constant.EmailConstants.HOST;
import static com.bytechef.component.email.constant.EmailConstants.SSL;
import static com.bytechef.component.email.constant.EmailConstants.TLS;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.email.EmailProtocol;
import java.util.Properties;

/**
 * @author Igor Beslic
 */
public class EmailUtils {

    public static Properties
        getMailSessionProperties(int port, EmailProtocol protocol, Parameters connectionParameters) {
        Properties props = new Properties();

        props.setProperty("mail.store.protocol", protocol.name());
        props.setProperty("mail.debug", "true");

        switch (connectionParameters.getRequiredString(CRYPTOGRAPHIC_PROTOCOL)) {
            case TLS -> props.put(String.format("mail.%s.starttls.enable", protocol), "true");
            case SSL -> {
                props.put(String.format("mail.%s.ssl.enable", protocol), "true");
                props.put(String.format("mail.%s.ssl.trust", protocol), connectionParameters.getRequiredString(HOST));
            }
            default -> {
            }
        }

        if (connectionParameters.containsKey(USERNAME)) {
            props.put(String.format("mail.%s.user", protocol), connectionParameters.getRequiredString(USERNAME));
            props.put(String.format("mail.%s.auth", protocol), true);
        }

        props.put(String.format("mail.%s.host", protocol), connectionParameters.getRequiredString(HOST));
        props.put(String.format("mail.%s.port", protocol), port);

        return props;
    }

}
