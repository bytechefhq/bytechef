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

package com.bytechef.component.twilio.util;

import static com.bytechef.component.twilio.constant.TwilioConstants.BODY;
import static com.bytechef.component.twilio.constant.TwilioConstants.CONTENT;
import static com.bytechef.component.twilio.constant.TwilioConstants.FROM;
import static com.bytechef.component.twilio.constant.TwilioConstants.MEDIA_URL;
import static com.bytechef.component.twilio.constant.TwilioConstants.MESSAGING_SERVICE_SID;
import static com.bytechef.component.twilio.constant.TwilioConstants.SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.Parameters;
import com.bytechef.hermes.component.definition.Property;
import com.bytechef.hermes.definition.BaseProperty.ValueProperty.ControlType;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class TwilioUtilsTest {

    private final Parameters mockedParameters = mock(Parameters.class);
    private final ActionContext mockedContext = mock(ActionContext.class);

    @Test
    void testGetContentPropertiesForMediaUrl() {
        when(mockedParameters.getString(CONTENT))
            .thenReturn(MEDIA_URL);

        List<? extends Property.ValueProperty<?>> mediaUrlProperties =
            TwilioUtils.getContentProperties(mockedParameters, mockedParameters, mockedContext);

        assertEquals(1, mediaUrlProperties.size());
        assertEquals("Media URL", mediaUrlProperties.getFirst().getLabel().get());
        assertEquals(
            "The URL of media to include in the Message content. jpeg, jpg, gif, and png file types are " +
                "fully supported by Twilio and content is formatted for delivery on destination devices. The " +
                "media size limit is 5 MB for supported file types (jpeg, jpg, png, gif) and 500 KB for " +
                "other types of accepted media. To send more than one image in the message, provide multiple " +
                "media_url parameters in the POST request. You can include up to ten media_url parameters " +
                "per message. International and carrier limits apply.",
            mediaUrlProperties.getFirst().getDescription().get());
        assertEquals(true, mediaUrlProperties.getFirst().getRequired().get());
    }

    @Test
    void testGetContentPropertiesForBody() {
        when(mockedParameters.getString(CONTENT))
            .thenReturn(BODY);

        List<? extends Property.ValueProperty<?>> bodyProperties =
            TwilioUtils.getContentProperties(mockedParameters, mockedParameters, mockedContext);

        assertEquals(1, bodyProperties.size());
        assertEquals("Body", bodyProperties.getFirst().getLabel().get());
        assertEquals(
            "The text content of the outgoing message. Can be up to 1,600 characters in length. SMS only: If " +
                "the body contains more than 160 GSM-7 characters (or 70 UCS-2 characters), the message is " +
                "segmented and charged accordingly. For long body text, consider using the send_as_mms " +
                "parameter.",
            bodyProperties .getFirst().getDescription().get());
        assertEquals(true, bodyProperties.getFirst().getRequired().get());
    }

    @Test
    void testGetSourcePropertiesForFrom() {
        when(mockedParameters.getString(SOURCE))
            .thenReturn(FROM);

        List<? extends Property.ValueProperty<?>> fromProperties =
            TwilioUtils.getSourceProperties(mockedParameters, mockedParameters, mockedContext);

        assertEquals(1, fromProperties.size());
        assertEquals("From", fromProperties.getFirst().getLabel().get());
        assertEquals(
            "The sender's Twilio phone number (in E.164 format), alphanumeric sender ID, Wireless SIM, short " +
                "code, or channel address (e.g., whatsapp:+15554449999). The value of the from parameter " +
                "must be a sender that is hosted within Twilio and belongs to the Account creating the " +
                "Message. If you are using messaging_service_sid, this parameter can be empty (Twilio " +
                "assigns a from value from the Messaging Service's Sender Pool) or you can provide a " +
                "specific sender from your Sender Pool.",
            fromProperties.getFirst().getDescription().get());
        assertEquals(ControlType.PHONE, fromProperties.getFirst().getControlType());
        assertEquals(true, fromProperties.getFirst().getRequired().get());
    }

    @Test
    void testGetSourcePropertiesForMessagingServiceSid() {
        when(mockedParameters.getString(SOURCE))
            .thenReturn(MESSAGING_SERVICE_SID);

        List<? extends Property.ValueProperty<?>> messagingServiceSidProperties =
            TwilioUtils.getSourceProperties(mockedParameters, mockedParameters, mockedContext);

        assertEquals(1, messagingServiceSidProperties.size());
        assertEquals(
            "Messaging Service SID", messagingServiceSidProperties .getFirst().getLabel().get());
        assertEquals(
            "The SID of the Messaging Service you want to associate with the Message. When this parameter is " +
                "provided and the from parameter is omitted, Twilio selects the optimal sender from the " +
                "Messaging Service's Sender Pool. You may also provide a from parameter if you want to use a " +
                "specific Sender from the Sender Pool.",
            messagingServiceSidProperties.getFirst().getDescription().get());
        assertEquals(true, messagingServiceSidProperties.getFirst().getRequired().get());
    }
}
