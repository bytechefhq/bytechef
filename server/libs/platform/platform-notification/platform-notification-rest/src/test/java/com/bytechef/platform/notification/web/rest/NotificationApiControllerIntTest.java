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

package com.bytechef.platform.notification.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.facade.NotificationFacade;
import com.bytechef.platform.notification.handler.NotificationTestResult;
import com.bytechef.platform.notification.web.rest.model.NotificationModel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
    NotificationApiController.class,
    NotificationApiControllerIntTest.NotificationApiControllerIntTestConfiguration.class
})
@WebMvcTest(controllers = NotificationApiController.class, properties = "bytechef.coordinator.enabled=true")
class NotificationApiControllerIntTest {

    private static final String NOTIFICATION_JSON = """
        {"name":"Datadog","type":"WEBHOOK","settings":{"webhook":"https://example.com/ingest"},\
        "notificationEventIds":[1]}""";

    private static final String TEST_PATH = "/internal/notifications/test";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationFacade notificationFacade;

    @Test
    void testTestNotificationUsesTheSavedNotificationId() throws Exception {
        when(notificationFacade.testNotification(any())).thenReturn(
            new NotificationTestResult(true, 200, null, null, true, null));

        mockMvc.perform(post(TEST_PATH).queryParam("notificationId", "42")
            .contentType(MediaType.APPLICATION_JSON)
            .content(NOTIFICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signed").value(true));

        assertThat(captureTestedNotification().getId()).isEqualTo(42L);
    }

    @Test
    void testTestNotificationWithoutIdTestsAnUnsavedNotification() throws Exception {
        when(notificationFacade.testNotification(any())).thenReturn(
            new NotificationTestResult(true, 200, null, null, false, "NO_SECRET"));

        mockMvc.perform(post(TEST_PATH).contentType(MediaType.APPLICATION_JSON)
            .content(NOTIFICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unsignedReason").value("NO_SECRET"));

        assertThat(captureTestedNotification().getId()).isNull();
    }

    private Notification captureTestedNotification() {
        ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

        verify(notificationFacade).testNotification(notificationArgumentCaptor.capture());

        return notificationArgumentCaptor.getValue();
    }

    @TestConfiguration
    static class NotificationApiControllerIntTestConfiguration {

        @Bean
        ConversionService conversionService() {
            GenericConversionService conversionService = new GenericConversionService();

            conversionService.addConverter(NotificationModel.class, Notification.class, notificationModel -> {
                Notification notification = new Notification();

                notification.setId(notificationModel.getId());
                notification.setName(notificationModel.getName());
                notification.setSettings(notificationModel.getSettings());
                notification.setType(Notification.Type.valueOf(notificationModel.getType()
                    .getValue()));

                return notification;
            });

            return conversionService;
        }
    }
}
