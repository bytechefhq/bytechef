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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.platform.billing.config.BillingIntTestConfiguration;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.dto.BillingSubscriptionDTO;
import com.bytechef.platform.billing.handler.SubscriptionModelHandlerRegistry;
import com.bytechef.platform.billing.handler.prepaid.PrepaidSubscriptionModelHandler;
import com.bytechef.platform.billing.repository.BillingSubscriptionRepository;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.stripe.model.Subscription;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Matija Petanjek
 */
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
@SpringBootTest(classes = BillingIntTestConfiguration.class)
class BillingSubscriptionFacadeIntTest {

    private static final String PRODUCT_STARTER_ID = "prod_starter_test";
    private static final String PRODUCT_GROWTH_ID = "prod_growth_test";
    private static final String PRODUCT_USAGE_ID = "prod_usage_test";

    @Autowired
    private BillingSubscriptionFacade billingSubscriptionFacade;

    @Autowired
    private BillingSubscriptionRepository billingSubscriptionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private StripeClient stripeClient;

    private final Subscription stripeSubscription = mock(Subscription.class);

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM principal_job");
        jdbcTemplate.update("DELETE FROM job");

        billingSubscriptionRepository.deleteAll();
    }

    @Test
    void testFetchCurrentSubscriptionReturnsDtoWithJobsExecuted() {
        BillingSubscription subscription = starterSubscription();

        Instant periodStart = Instant.now()
            .minus(7, ChronoUnit.DAYS)
            .truncatedTo(ChronoUnit.SECONDS);

        subscription.setCurrentPeriodStart(periodStart);

        BillingSubscription savedSubscription = billingSubscriptionRepository.save(subscription);

        seedDeployedJobs(42, Instant.now()
            .minus(1, ChronoUnit.DAYS));
        seedDeployedJobs(3, periodStart.minus(1, ChronoUnit.DAYS));

        when(stripeClient.fetchScheduledPlanName("sub_starter")).thenReturn(Optional.of("GROWTH"));

        Optional<BillingSubscriptionDTO> result = billingSubscriptionFacade.fetchCurrentSubscription();

        assertThat(result).isPresent();
        assertThat(result.get()
            .jobsExecuted()).isEqualTo(42);
        assertThat(result.get()
            .subscription()).isEqualTo(savedSubscription);
        assertThat(result.get()
            .scheduledPlanName()).isEqualTo("GROWTH");
    }

    @Test
    void testUpgradeSubscriptionCallsUpgradeNowForUpdatePath() {
        billingSubscriptionRepository.save(starterSubscription());

        when(stripeClient.retrieveSubscription("sub_starter")).thenReturn(stripeSubscription);
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_GROWTH_ID)).thenReturn("price_growth");

        billingSubscriptionFacade.updateSubscription("GROWTH");

        verify(stripeClient).upgradeSubscriptionNow(
            eq("sub_starter"), eq("si_flat_starter"), eq("price_growth"), eq("GROWTH"), any());
        verify(stripeClient, never()).scheduleDowngrade(any(), any(), any(), any(), any(), any(), any(), anyLong());
    }

    @Test
    void testUpdateSubscriptionSchedulesDowngrade() {
        billingSubscriptionRepository.save(growthSubscription());

        when(stripeClient.retrieveSubscription("sub_growth")).thenReturn(stripeSubscription);
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_STARTER_ID)).thenReturn("price_starter");
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_USAGE_ID)).thenReturn("price_usage");

        billingSubscriptionFacade.updateSubscription("STARTER");

        verify(stripeClient).scheduleDowngrade(
            eq("sub_growth"), eq("si_flat_growth"), eq("si_usage_growth"),
            eq("price_starter"), eq("price_usage"), eq("STARTER"), any(),
            eq(Instant.parse("2026-07-01T00:00:00Z")
                .getEpochSecond()));
        verify(stripeClient, never()).upgradeSubscriptionNow(any(), any(), any(), any(), any());
    }

    @Test
    void testUpgradeSubscriptionReleasesExistingScheduleBeforeUpgrading() {
        billingSubscriptionRepository.save(starterSubscription());

        when(stripeClient.retrieveSubscription(any())).thenReturn(stripeSubscription);
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_GROWTH_ID)).thenReturn("price_growth");

        billingSubscriptionFacade.updateSubscription("GROWTH");

        verify(stripeClient).releaseSubscriptionScheduleIfPresent(stripeSubscription);
        verify(stripeClient).upgradeSubscriptionNow(any(), any(), eq("price_growth"), eq("GROWTH"), any());
    }

    @Test
    void testCancelSubscriptionThrowsWhenNoActiveSubscription() {
        assertThatThrownBy(() -> billingSubscriptionFacade.cancelSubscription())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No active subscription found");
    }

    @Test
    void testReactivateSubscriptionThrowsWhenNoActiveSubscription() {
        assertThatThrownBy(() -> billingSubscriptionFacade.reactivateSubscription())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No active subscription found");
    }

    @Test
    void testUpdateSubscriptionThrowsForUnknownPlanName() {
        billingSubscriptionRepository.save(starterSubscription());

        when(stripeClient.retrieveSubscription(any())).thenReturn(stripeSubscription);

        assertThatThrownBy(() -> billingSubscriptionFacade.updateSubscription("ENTERPRISE"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown plan: ENTERPRISE");
    }

    private void seedDeployedJobs(int count, Instant endDate) {
        String workflowId = UUID.randomUUID()
            .toString();

        jdbcTemplate.update(
            """
                INSERT INTO job (workflow_id, status, current_task, priority, created_date, created_by,
                    last_modified_date, last_modified_by, version, end_date)
                SELECT ?, 2, 0, 0, now(), 'system', now(), 'system', 0, ? FROM generate_series(1, ?)
                """,
            workflowId, Timestamp.from(endDate), count);
        jdbcTemplate.update(
            "INSERT INTO principal_job (job_id, principal_id, type) SELECT id, 1, 0 FROM job WHERE workflow_id = ?",
            workflowId);
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

    private static BillingSubscription growthSubscription() {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setSubscriptionId("sub_growth");
        subscription.setProductId("si_flat_growth");
        subscription.setUsageProductId("si_usage_growth");
        subscription.setPlanName("GROWTH");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);
        subscription.setSubscriptionModel(BillingSubscription.SubscriptionModel.METERED);
        subscription.setProductUnitLimit(5000);
        subscription.setCurrentPeriodStart(Instant.parse("2026-06-01T00:00:00Z"));
        subscription.setCurrentPeriodEnd(Instant.parse("2026-07-01T00:00:00Z"));

        return subscription;
    }

    @Nested
    class MethodSecurityEnforcement {

        private static final String BODY_REACHED = "body reached";

        private final BillingSubscriptionFacade securedBillingSubscriptionFacade = secure(
            new BillingSubscriptionFacadeImpl(
                mock(BillingSubscriptionService.class, invocation -> {
                    throw new IllegalStateException(BODY_REACHED);
                }),
                mock(PrepaidSubscriptionModelHandler.class, invocation -> {
                    throw new IllegalStateException(BODY_REACHED);
                }),
                mock(SubscriptionModelHandlerRegistry.class, invocation -> {
                    throw new IllegalStateException(BODY_REACHED);
                })));

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @ParameterizedTest
        @MethodSource("subscriptionWrites")
        void testSubscriptionWriteDeniesACallerWithoutTheAdminAuthority(GuardedOperation guardedOperation) {
            authenticate(AuthorityConstants.USER);

            assertThatThrownBy(() -> guardedOperation.invoke(securedBillingSubscriptionFacade))
                .isInstanceOf(AccessDeniedException.class);
        }

        @ParameterizedTest
        @MethodSource("subscriptionWrites")
        void testSubscriptionWriteAllowsACallerHoldingTheAdminAuthority(GuardedOperation guardedOperation) {
            authenticate(AuthorityConstants.ADMIN);

            assertThatThrownBy(() -> guardedOperation.invoke(securedBillingSubscriptionFacade))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        }

        static Stream<Named<GuardedOperation>> subscriptionWrites() {
            return Stream.of(
                Named.of("cancelSubscription", (GuardedOperation) BillingSubscriptionFacade::cancelSubscription),
                Named.of(
                    "createCheckoutSession",
                    (GuardedOperation) guardedFacade -> guardedFacade.createCheckoutSession("STARTER")),
                Named.of(
                    "createJobExecutionsPackageCheckoutSession",
                    (GuardedOperation) guardedFacade -> guardedFacade.createJobExecutionsPackageCheckoutSession(1)),
                Named.of(
                    "reactivateSubscription", (GuardedOperation) BillingSubscriptionFacade::reactivateSubscription),
                Named.of(
                    "updateSubscription",
                    (GuardedOperation) guardedFacade -> guardedFacade.updateSubscription("GROWTH")));
        }

        private static void authenticate(String authority) {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "alice", "credentials", List.of(new SimpleGrantedAuthority(authority))));

            SecurityContextHolder.setContext(securityContext);
        }

        private BillingSubscriptionFacade secure(BillingSubscriptionFacade billingSubscriptionFacade) {
            DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();

            expressionHandler.setRoleHierarchy(
                RoleHierarchyImpl.withDefaultRolePrefix()
                    .role("ADMIN")
                    .implies("USER")
                    .build());

            PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager =
                new PreAuthorizeAuthorizationManager();

            preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

            ProxyFactory proxyFactory = new ProxyFactory(billingSubscriptionFacade);

            proxyFactory.addAdvisor(
                AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

            return (BillingSubscriptionFacade) proxyFactory.getProxy();
        }

        @FunctionalInterface
        interface GuardedOperation {

            void invoke(BillingSubscriptionFacade guardedFacade);
        }
    }
}
