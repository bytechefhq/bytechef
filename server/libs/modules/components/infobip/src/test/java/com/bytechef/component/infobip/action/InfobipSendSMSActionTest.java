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

package com.bytechef.component.infobip.action;

import static com.bytechef.component.definition.Authorization.VALUE;
import static com.bytechef.component.infobip.constant.InfobipConstants.AMOUNT;
import static com.bytechef.component.infobip.constant.InfobipConstants.BULK_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.INCLUDE_SMS_COUNT_IN_RESPONSE;
import static com.bytechef.component.infobip.constant.InfobipConstants.TIME_UNIT;
import static com.bytechef.component.infobip.constant.InfobipConstants.TRACKING;
import static com.bytechef.component.infobip.constant.InfobipConstants.URL_OPTIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.infobip.util.InfobipUtils;
import com.infobip.ApiException;
import com.infobip.api.SmsApi;
import com.infobip.model.SmsAdvancedTextualRequest;
import com.infobip.model.SmsResponse;
import com.infobip.model.SmsTextualMessage;
import com.infobip.model.SmsTracking;
import com.infobip.model.SmsUrlOptions;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class InfobipSendSMSActionTest extends AbstractInfobipActionTest {

    private final SmsApi.SendSmsMessageRequest mockedSendSmsMessageRequest = mock(SmsApi.SendSmsMessageRequest.class);
    private final SmsResponse mockedSmsResponse = mock(SmsResponse.class);
    private final ArgumentCaptor<SmsAdvancedTextualRequest> smsAdvancedTextualRequestArgumentCaptor =
        ArgumentCaptor.forClass(SmsAdvancedTextualRequest.class);

    @Test
    void testPerform() throws ApiException {
        SmsUrlOptions smsUrlOptions = new SmsUrlOptions().shortenUrl(true);
        SmsTracking smsTracking = new SmsTracking().track("track");
        List<SmsTextualMessage> smsTextualMessages = List.of(new SmsTextualMessage().from("from"));

        when(mockedParameters.getRequiredString(VALUE))
            .thenReturn("value");
        when(mockedParameters.getString(BULK_ID))
            .thenReturn("bulkID");
        when(mockedParameters.getInteger(AMOUNT))
            .thenReturn(1);
        when(mockedParameters.getString(TIME_UNIT))
            .thenReturn("MINUTE");
        when(mockedParameters.get(URL_OPTIONS, SmsUrlOptions.class))
            .thenReturn(smsUrlOptions);
        when(mockedParameters.get(TRACKING, SmsTracking.class))
            .thenReturn(smsTracking);
        when(mockedParameters.getBoolean(INCLUDE_SMS_COUNT_IN_RESPONSE))
            .thenReturn(true);

        try (MockedConstruction<SmsApi> smsApiMockedConstruction = mockConstruction(
            SmsApi.class,
            ((smsApi, context) -> when(smsApi.sendSmsMessage(any()))
                .thenReturn(mockedSendSmsMessageRequest)))) {
            when(mockedSendSmsMessageRequest.execute())
                .thenReturn(mockedSmsResponse);

            try (MockedStatic<InfobipUtils> infobipUtilsMockedStatic = mockStatic(InfobipUtils.class)) {
                infobipUtilsMockedStatic.when(() -> InfobipUtils.createSmsTextualMessageList(anyList()))
                    .thenReturn(smsTextualMessages);

                SmsResponse result = InfobipSendSMSAction.perform(mockedParameters, mockedParameters, mockedContext);

                List<SmsApi> smsApis = smsApiMockedConstruction.constructed();

                assertEquals(1, smsApis.size());
                assertEquals(mockedSmsResponse, result);

                SmsApi smsApi = smsApis.getFirst();

                verify(smsApi, times(1))
                    .sendSmsMessage(smsAdvancedTextualRequestArgumentCaptor.capture());

                SmsAdvancedTextualRequest smsAdvancedTextualRequest =
                    smsAdvancedTextualRequestArgumentCaptor.getValue();

                assertEquals("bulkID", smsAdvancedTextualRequest.getBulkId());
                assertEquals(1, smsAdvancedTextualRequest.getSendingSpeedLimit()
                    .getAmount());
                assertEquals("MINUTE", smsAdvancedTextualRequest.getSendingSpeedLimit()
                    .getTimeUnit()
                    .toString());
                assertEquals(smsUrlOptions, smsAdvancedTextualRequest.getUrlOptions());
                assertEquals(smsTracking, smsAdvancedTextualRequest.getTracking());
                assertEquals(true, smsAdvancedTextualRequest.getIncludeSmsCountInResponse());
                assertEquals(smsTextualMessages, smsAdvancedTextualRequest.getMessages());
            }
        }
    }
}
