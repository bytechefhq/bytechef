/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.text.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.text.helper.constant.TextHelperConstants;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * @author Igor Beslic
 */
public class TextHelperBase64DecodeAction {

    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION =
        action(TextHelperConstants.BASE_64_DECODE)
            .title("Base64 Decode")
            .description("Decodes base64 encoded text into human readable plain text.")
            .properties(
                string(TextHelperConstants.ENCODING_SCHEMA)
                    .label("Encoding Scheme")
                    .options(option("Base64", TextHelperConstants.ENCODING_SCHEMA_BASE64),
                        option("Base64 URL", TextHelperConstants.ENCODING_SCHEMA_BASE64URL))
                    .controlType(Property.ControlType.SELECT)
                    .defaultValue(TextHelperConstants.ENCODING_SCHEMA_BASE64),
                string(TextHelperConstants.CONTENT)
                    .label("Base64 Content")
                    .description("The Base64 encoded content that needs to be decoded.")
                    .controlType(Property.ControlType.TEXT_AREA)
                    .required(true))
            .output()
            .perform(TextHelperBase64DecodeAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String base64Content = inputParameters.getRequiredString(TextHelperConstants.CONTENT);

        if (base64Content.isEmpty()) {
            return base64Content;
        }

        Base64.Decoder decoder = of(inputParameters.getRequiredString(TextHelperConstants.ENCODING_SCHEMA));

        return new String(decoder.decode(base64Content), StandardCharsets.UTF_8);
    }

    private static Base64.Decoder of(String schema) {
        if (Objects.equals(TextHelperConstants.ENCODING_SCHEMA_BASE64, schema)) {
            return Base64.getDecoder();
        } else if (Objects.equals(TextHelperConstants.ENCODING_SCHEMA_BASE64URL, schema)) {
            return Base64.getUrlDecoder();
        }

        throw new IllegalArgumentException("Unsupported schema: " + schema);
    }

}
