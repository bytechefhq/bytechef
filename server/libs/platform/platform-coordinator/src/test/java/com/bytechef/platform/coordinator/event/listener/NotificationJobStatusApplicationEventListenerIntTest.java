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

package com.bytechef.platform.coordinator.event.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.coordinator.config.PlatformCoordinatorIntTestConfiguration;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.service.NotificationService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformCoordinatorIntTestConfiguration.class)
class NotificationJobStatusApplicationEventListenerIntTest {

    private static final String CONTENT = "Job Test Job (ID: 7) execution completed.";
    private static final String FIRST_EMAIL = "first@example.com";
    private static final long JOB_ID = 7L;
    private static final String SECOND_EMAIL = "second@example.com";
    private static final String SUBJECT = "Job Execution Status - Job Test Job (ID: 7) completed";

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private MailService mailService;

    @MockitoBean
    private MessageBroker messageBroker;

    @Autowired
    private NotificationJobStatusApplicationEventListener notificationJobStatusApplicationEventListener;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private TaskExecutionService taskExecutionService;

    @BeforeEach
    void beforeEach() {
        Job job = new Job(JOB_ID);

        job.setLabel("Test Job");

        when(jobService.getJob(JOB_ID)).thenReturn(job);
        when(notificationService.getNotifications(NotificationEvent.Type.JOB_COMPLETED))
            .thenReturn(List.of(createEmailNotification(1L, FIRST_EMAIL), createEmailNotification(2L, SECOND_EMAIL)));
    }

    @Test
    void testSendJobStatusEmails() {
        notificationJobStatusApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(JOB_ID, Job.Status.COMPLETED));

        verify(mailService, timeout(5000)).sendEmailOrThrow(FIRST_EMAIL, SUBJECT, CONTENT, false, false);
        verify(mailService, timeout(5000)).sendEmailOrThrow(SECOND_EMAIL, SUBJECT, CONTENT, false, false);
    }

    @Test
    void testFailedEmailSendDoesNotStopTheRemainingNotifications() {
        doThrow(new MailSendException("SMTP unavailable")).when(mailService)
            .sendEmailOrThrow(eq(FIRST_EMAIL), anyString(), anyString(), anyBoolean(), anyBoolean());

        assertThatCode(
            () -> notificationJobStatusApplicationEventListener.onApplicationEvent(
                new JobStatusApplicationEvent(JOB_ID, Job.Status.COMPLETED)))
                    .doesNotThrowAnyException();

        verify(mailService, timeout(5000)).sendEmailOrThrow(SECOND_EMAIL, SUBJECT, CONTENT, false, false);
    }

    @Test
    void testSendEmailsOffTheListenerThread() throws Exception {
        CompletableFuture<Thread> sendThreadFuture = new CompletableFuture<>();

        doAnswer(invocation -> sendThreadFuture.complete(Thread.currentThread())).when(mailService)
            .sendEmailOrThrow(eq(FIRST_EMAIL), anyString(), anyString(), anyBoolean(), anyBoolean());

        notificationJobStatusApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(JOB_ID, Job.Status.COMPLETED));

        assertThat(sendThreadFuture.get(5, TimeUnit.SECONDS)).isNotSameAs(Thread.currentThread());
    }

    private static Notification createEmailNotification(long id, String email) {
        Notification notification = new Notification();

        notification.setId(id);
        notification.setSettings(Map.of("email", email));
        notification.setType(Notification.Type.EMAIL);

        return notification;
    }
}
