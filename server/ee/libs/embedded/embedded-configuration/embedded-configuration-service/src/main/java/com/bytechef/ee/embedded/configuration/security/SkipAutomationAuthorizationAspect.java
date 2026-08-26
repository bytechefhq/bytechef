/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.ResourceMembershipResolver;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Activates {@link AutomationAuthorizationContext} skip mode for the full synchronous execution of any type or method
 * annotated with {@code com.bytechef.automation.configuration.security.SkipAutomationAuthorization}. Ordered at
 * {@link Ordered#HIGHEST_PRECEDENCE} so the skip flag is set before any nested method-security interceptor evaluates a
 * downstream automation {@code @PreAuthorize}.
 *
 * <p>
 * <b>Except for a principal {@link ResourceMembershipResolver} governs</b> — today, an embedded connected user — which
 * proceeds with no skip mode armed at all. Ticket 1051: such a principal is answered from its own membership at every
 * resource-scoped check, so it needs nothing skipped on its behalf; and the checks the membership seam does NOT answer
 * are exactly the ones it must never pass. Full skip grants tenant-admin status, workspace membership/role/scope,
 * current-user identity and resource ownership, none of which a connected user has.
 *
 * <p>
 * Until this check existed, that guarantee was held instead by {@code EmbeddedAutomationAuthorizationSkipFilter} arming
 * a narrower skip mode at the top of every embedded request, which a monotonic-narrowing rule in
 * {@code AutomationAuthorizationContext} then kept from being widened here. Both the filter and that rule are gone.
 * That floor was narrower than it looked. Every {@code SecurityConfigurerContributor} lands on the single
 * {@code apiFilterChain} ({@code /api/**}, {@code /graphql}) in
 * {@code com.bytechef.security.config.SecurityConfiguration}, so the filter does run for every API request — but it
 * arms nothing unless the principal is an {@code EmbeddedApiKeyAuthenticationToken} specifically. An embedded MCP
 * caller authenticates as {@code EmbeddedMcpServerApiKeyAuthenticationToken}, whose provider get-or-creates the same
 * {@code ConnectedUser} and carries the same environment — so it IS governed by the resolver, and has never had a floor
 * beneath this aspect.
 *
 * <p>
 * No call chain from an MCP request into an annotated facade has been demonstrated, so this is a latent hole rather
 * than an exploited one; the same is true of the filtered path, where no workspace-scoped gate is reachable from any of
 * the five annotated classes within three hops. The point of deciding from the principal is that neither statement is
 * something a future caller can be relied upon to preserve: both are properties of what happens to be called today, and
 * one new call edge changes either. This makes it a property of who the caller is.
 *
 * <p>
 * Behaviour-preserving on the filtered path, by construction rather than by measurement: a governed principal's
 * resource-scoped checks were already decided by {@code ResourceMembershipDecider} ahead of the skip check, so removing
 * the skip changes none of them; and its workspace-scoped checks were already denied by the narrower skip mode that
 * filter armed, so enforcing them changes none of them either.
 *
 * <p>
 * The cost is one {@link ResourceMembershipResolver#governsCurrentPrincipal()} lookup per invocation of an annotated
 * method, and it is deliberately not cached. Caching it per request would need a request-scoped holder or a
 * thread-local cleared by a filter, which is the shape this change exists to stop depending on. The lookup is the same
 * one the resolver already performs on every single permission check — of which one request makes far more than it does
 * annotated facade calls — so it is a rounding error against a cost Stage 2 already accepted. In Community Edition
 * there is no resolver bean and {@link ObjectProvider#getIfAvailable()} costs nothing.
 *
 * <p>
 * A governed principal proceeds unwrapped rather than having an outer skip actively cleared: no path arms skip mode and
 * then hands the stack to a connected user. The two copilot hand-offs that used to arm a narrower mode for one
 * ({@code WorkflowEditorSpringAIAgent}, {@code CopilotToolContextUtils}) now arm nothing at all, since the resolver
 * governs the connected user on those threads. Were such a path ever added, the outer skip would survive this aspect.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Aspect
@Component
@ConditionalOnEEVersion
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SkipAutomationAuthorizationAspect {

    private final ObjectProvider<ResourceMembershipResolver> resourceMembershipResolverProvider;

    @SuppressFBWarnings("EI")
    public SkipAutomationAuthorizationAspect(
        ObjectProvider<ResourceMembershipResolver> resourceMembershipResolverProvider) {

        this.resourceMembershipResolverProvider = resourceMembershipResolverProvider;
    }

    @Around("@within(com.bytechef.automation.configuration.security.SkipAutomationAuthorization) || " +
        "@annotation(com.bytechef.automation.configuration.security.SkipAutomationAuthorization)")
    public Object skipAutomationAuthorization(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        if (governsCurrentPrincipal()) {
            return proceedingJoinPoint.proceed();
        }

        return AutomationAuthorizationContext.callSkippingChecks(proceedingJoinPoint::proceed);
    }

    /**
     * Whether the membership seam answers for the current principal. An ABSENT resolver leaves the caller on the
     * pre-existing full-skip path — the same answer this aspect gave before the check existed — so Community Edition
     * and any deployment without the bean are untouched.
     *
     * <p>
     * A resolver that is present but whose lookup FAILS now propagates the exception rather than answering false; see
     * {@code ConnectedUserResourceMembershipResolver.governsCurrentPrincipal()}. That is deliberate here too. Answering
     * false on a failed lookup would arm full skip for a caller that may well be a connected user, which is the one
     * outcome this aspect exists to prevent — a robustness catch would trade a visible 500 for a silent grant of
     * tenant-admin status. The resolver reaches its query only for an api-key principal, so an ordinary platform
     * request cannot be turned into a 500 by this line.
     */
    private boolean governsCurrentPrincipal() {
        ResourceMembershipResolver resourceMembershipResolver = resourceMembershipResolverProvider.getIfAvailable();

        return resourceMembershipResolver != null && resourceMembershipResolver.governsCurrentPrincipal();
    }
}
