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

package com.bytechef.platform.billing.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.billing.audit.BillingAuditEvents;
import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.platform.billing.config.BillingIntTestConfiguration;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.handler.prepaid.PrepaidStripeClient;
import com.bytechef.platform.billing.repository.BillingSubscriptionRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
@SpringBootTest(
    classes = {
        BillingIntTestConfiguration.class, BillingSubscriptionFacadeAuditIntTest.AuditConfiguration.class
    },
    properties = {
        "bytechef.edition=ee", "bytechef.audit.enabled=true"
    })
class BillingSubscriptionFacadeAuditIntTest {

    private static final String ADMIN_LOGIN = "admin@localhost.com";
    private static final String CHECKOUT_URL = "https://checkout.stripe.com/confidential-session";

    @Autowired
    private BillingSubscriptionFacade billingSubscriptionFacade;

    @Autowired
    private BillingSubscriptionRepository billingSubscriptionRepository;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @MockitoBean
    private PrepaidStripeClient prepaidStripeClient;

    @MockitoBean
    private StripeClient stripeClient;

    @BeforeEach
    void beforeEach() {
        billingSubscriptionRepository.deleteAll();

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(ADMIN_LOGIN, "n/a", List.of()));

        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        billingSubscriptionRepository.deleteAll();
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testCreateCheckoutSessionRecordsThePlanWithoutTheSessionUrl() {
        Session session = mock(Session.class);

        when(session.getUrl()).thenReturn(CHECKOUT_URL);
        when(stripeClient.fetchCustomerId(any(), any())).thenReturn(Optional.of("cus_audit"));
        when(prepaidStripeClient.createSubscriptionCheckoutSession(any(), any(), any(), any(), any(), any()))
            .thenReturn(session);

        assertThat(billingSubscriptionFacade.createCheckoutSession("GROWTH")).isEqualTo(CHECKOUT_URL);

        assertThat(findSingleEvent(BillingAuditEvents.BILLING_CHECKOUT_SESSION_CREATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("planName", "GROWTH")
            .doesNotContainValue(CHECKOUT_URL);
    }

    @Test
    void testSubscriptionChangesRecordTheChange() {
        billingSubscriptionRepository.save(starterSubscription());

        when(stripeClient.retrieveSubscription("sub_starter")).thenReturn(mock(Subscription.class));

        billingSubscriptionFacade.updateSubscription("GROWTH");

        assertThat(findSingleEvent(BillingAuditEvents.BILLING_SUBSCRIPTION_UPDATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("planName", "GROWTH");

        billingSubscriptionFacade.cancelSubscription();

        assertThat(findSingleEvent(BillingAuditEvents.BILLING_SUBSCRIPTION_CANCELLED))
            .containsEntry("result", "SUCCESS");

        billingSubscriptionFacade.reactivateSubscription();

        assertThat(findSingleEvent(BillingAuditEvents.BILLING_SUBSCRIPTION_REACTIVATED))
            .containsEntry("result", "SUCCESS");
    }

    @Test
    void testCancelWithoutAnActiveSubscriptionRecordsAnError() {
        assertThatThrownBy(() -> billingSubscriptionFacade.cancelSubscription())
            .isInstanceOf(IllegalStateException.class);

        assertThat(findSingleEvent(BillingAuditEvents.BILLING_SUBSCRIPTION_CANCELLED))
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalStateException.class.getName());
    }

    private Map<String, String> findSingleEvent(String eventType) {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> pageContent = page.getContent();

        List<PersistentAuditEvent> persistentAuditEvents = pageContent.stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.getFirst();

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);

        return persistentAuditEvent.getData();
    }

    private static BillingSubscription starterSubscription() {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setSubscriptionId("sub_starter");
        subscription.setProductId("si_flat_starter");
        subscription.setUsageProductId("si_usage_starter");
        subscription.setPlanName("STARTER");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);
        subscription.setSubscriptionModel(BillingSubscription.SubscriptionModel.METERED);
        subscription.setProductUnitLimit(5000);
        subscription.setCurrentPeriodStart(Instant.parse("2026-06-01T00:00:00Z"));
        subscription.setCurrentPeriodEnd(Instant.parse("2026-07-01T00:00:00Z"));

        return subscription;
    }

    @Configuration
    @EnableConfigurationProperties(ApplicationProperties.class)
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class
    })
    static class AuditConfiguration {
    }
}
