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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.platform.billing.config.BillingProperties;
import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.dto.BillingSubscriptionDTO;
import com.bytechef.platform.billing.service.BillingSubscriptionService;
import com.bytechef.platform.billing.service.BillingUsageService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.stripe.model.Subscription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * @author Matija Petanjek
 */
@ExtendWith(MockitoExtension.class)
class BillingSubscriptionFacadeImplTest {

    private static final String PRODUCT_STARTER_ID = "prod_starter_test";
    private static final String PRODUCT_GROWTH_ID = "prod_growth_test";
    private static final String PRODUCT_USAGE_ID = "prod_usage_test";

    @Mock
    private BillingSubscriptionService billingSubscriptionService;

    @Mock
    private BillingUsageService billingUsageService;

    @Mock
    private StripeClient stripeClient;

    @Mock
    private Subscription mockStripeSubscription;

    private BillingSubscriptionFacadeImpl facade;

    @BeforeEach
    void setUp() {
        BillingProperties billingProperties = new BillingProperties(
            new BillingProperties.Stripe(
                "sk_test_abc", null, PRODUCT_STARTER_ID, PRODUCT_GROWTH_ID, PRODUCT_USAGE_ID, null, "whsec_test",
                null, null));

        facade = new BillingSubscriptionFacadeImpl(
            billingProperties, billingSubscriptionService, billingUsageService, stripeClient);
    }

    @Test
    void testFetchCurrentSubscriptionReturnsDtoWithJobsExecuted() {
        BillingSubscription subscription = starterSubscription();

        Instant periodStart = Instant.parse("2026-06-01T00:00:00Z");

        subscription.setCurrentPeriodStart(periodStart);

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(subscription));
        when(billingUsageService.countJobExecutionsSince(eq(periodStart), any(Instant.class))).thenReturn(42);
        when(stripeClient.fetchScheduledPlanName(any())).thenReturn(Optional.of("GROWTH"));

        Optional<BillingSubscriptionDTO> result = facade.fetchCurrentSubscription();

        assertThat(result).isPresent();
        assertThat(result.get()
            .jobsExecuted()).isEqualTo(42);
        assertThat(result.get()
            .subscription()).isSameAs(subscription);
        assertThat(result.get()
            .scheduledPlanName()).isEqualTo("GROWTH");
    }

    @Test
    void testUpgradeSubscriptionCallsUpgradeNowForUpdatePath() {
        BillingSubscription currentSubscription = starterSubscription();

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(currentSubscription));
        when(stripeClient.retrieveSubscription("sub_starter")).thenReturn(mockStripeSubscription);
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_GROWTH_ID)).thenReturn("price_growth");

        facade.updateSubscription("GROWTH");

        verify(stripeClient).upgradeSubscriptionNow(
            eq("sub_starter"), eq("si_flat_starter"), eq("price_growth"), eq("GROWTH"), any());
        verify(stripeClient, never()).scheduleDowngrade(any(), any(), any(), any(), any(), any(), any(), anyLong());
    }

    @Test
    void testUpdateSubscriptionSchedulesDowngrade() {
        BillingSubscription currentSubscription = growthSubscription();

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(currentSubscription));
        when(stripeClient.retrieveSubscription("sub_growth")).thenReturn(mockStripeSubscription);
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_STARTER_ID)).thenReturn("price_starter");
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_USAGE_ID)).thenReturn("price_usage");

        facade.updateSubscription("STARTER");

        verify(stripeClient).scheduleDowngrade(
            eq("sub_growth"), eq("si_flat_growth"), eq("si_usage_growth"),
            eq("price_starter"), eq("price_usage"), eq("STARTER"), any(), anyLong());
        verify(stripeClient, never()).upgradeSubscriptionNow(any(), any(), any(), any(), any());
    }

    @Test
    void testUpgradeSubscriptionReleasesExistingScheduleBeforeUpgrading() {
        BillingSubscription currentSubscription = starterSubscription();

        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(currentSubscription));
        when(stripeClient.retrieveSubscription(any())).thenReturn(mockStripeSubscription);
        when(stripeClient.fetchProductDefaultPriceId(PRODUCT_GROWTH_ID)).thenReturn("price_growth");

        facade.updateSubscription("GROWTH");

        verify(stripeClient).releaseSubscriptionScheduleIfPresent(mockStripeSubscription);
        verify(stripeClient).upgradeSubscriptionNow(any(), any(), eq("price_growth"), eq("GROWTH"), any());
    }

    @Test
    void testCancelSubscriptionThrowsWhenNoActiveSubscription() {
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.cancelSubscription())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No active subscription found");
    }

    @Test
    void testReactivateSubscriptionThrowsWhenNoActiveSubscription() {
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.reactivateSubscription())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No active subscription found");
    }

    @Test
    void testUpdateSubscriptionThrowsForUnknownPlanName() {
        when(billingSubscriptionService.fetchCurrentSubscription()).thenReturn(Optional.of(starterSubscription()));
        when(stripeClient.retrieveSubscription(any())).thenReturn(mockStripeSubscription);

        assertThatThrownBy(() -> facade.updateSubscription("ENTERPRISE"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown plan: ENTERPRISE");
    }

    private BillingSubscription starterSubscription() {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setSubscriptionId("sub_starter");
        subscription.setProductId("si_flat_starter");
        subscription.setUsageProductId("si_usage_starter");
        subscription.setPlanName("STARTER");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.parse("2026-07-01T00:00:00Z"));

        return subscription;
    }

    private BillingSubscription growthSubscription() {
        BillingSubscription subscription = new BillingSubscription();

        subscription.setSubscriptionId("sub_growth");
        subscription.setProductId("si_flat_growth");
        subscription.setUsageProductId("si_usage_growth");
        subscription.setPlanName("GROWTH");
        subscription.setStatus(BillingSubscription.Status.ACTIVE);
        subscription.setCurrentPeriodEnd(Instant.parse("2026-07-01T00:00:00Z"));

        return subscription;
    }

    static Stream<Arguments> subscriptionWrites() {
        return Stream.of("cancelSubscription", "createCheckoutSession", "reactivateSubscription", "updateSubscription")
            .flatMap(methodName -> Stream.of(Arguments.of(methodName, false), Arguments.of(methodName, true)));
    }

    // Evaluates the real @PreAuthorize expressions on the subscription writes through the real
    // AutomationMethodSecurityExpressionHandler and AutomationPermissionEvaluator. Each write changes the tenant's
    // subscription, so it must decide on the caller's ROLE_ADMIN authority alone.
    @ParameterizedTest(name = "{0} admin={1}")
    @MethodSource("subscriptionWrites")
    void testSubscriptionWriteRequiresTheAdminAuthority(String methodName, boolean admin) {
        PermissionService permissionService = mock(PermissionService.class);

        assertThat(evaluateGuard(permissionService, findMethod(methodName), admin))
            .as("%s must %s a caller %s ROLE_ADMIN", methodName, admin ? "allow" : "deny",
                admin ? "holding" : "without")
            .isEqualTo(admin);

        verifyNoInteractions(permissionService);
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode, not attacker-influenced input.
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, Method method, boolean admin) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s must carry a @PreAuthorize guard", method.getName())
            .isNotNull();

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        List<GrantedAuthority> authorities = admin
            ? List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))
            : List.of(new SimpleGrantedAuthority(AuthorityConstants.USER));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", authorities);

        Object[] arguments = new Object[method.getParameterCount()];

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(new Object(), method, arguments);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static Method findMethod(String methodName) {
        List<Method> matches = Arrays.stream(BillingSubscriptionFacadeImpl.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> methodName.equals(method.getName()))
            .toList();

        assertThat(matches)
            .as("Expected exactly one non-synthetic '%s' method on BillingSubscriptionFacadeImpl", methodName)
            .hasSize(1);

        return matches.getFirst();
    }
}
