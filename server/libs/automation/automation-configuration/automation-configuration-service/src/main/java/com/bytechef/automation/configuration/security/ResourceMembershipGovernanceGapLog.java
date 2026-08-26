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

package com.bytechef.automation.configuration.security;

import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.security.web.authentication.PrincipalEnvironment;
import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records an api-key principal reaching a resource-scoped check that {@link ResourceMembershipResolver} declined to
 * govern — i.e. the membership seam did not recognise a caller it was built for.
 *
 * <p>
 * This was written to answer one question — whether {@code EmbeddedAutomationAuthorizationSkipFilter} could be deleted
 * — because that filter's last remaining job was to keep granting resource-scoped checks in exactly this case: the
 * caller IS an embedded api-key principal, but {@code governsCurrentPrincipal()} answers false. The filter is now gone
 * (ticket 1051 Stage 4), which does not retire this log, it promotes it: every line here used to be a silent grant and
 * is now a 403 the caller received. It is the alarm for the membership seam failing to recognise a caller it was built
 * for, and in a healthy deployment it never fires.
 *
 * <p>
 * Distinct from {@link ResourceMembershipDenialLog}, which records a governed principal being DENIED — a working seam
 * saying no. This records the seam not answering at all.
 *
 * <p>
 * <b>An alarm that fires routinely is an alarm that gets filtered out</b>, and this is the only instrument the branch
 * has for the surface it deliberately left open — so the predicate is the precondition the seam actually needs, not the
 * widest plausible one.
 *
 * <p>
 * That precondition is <b>the principal carrying an environment</b>. {@link ResourceMembershipResolver} cannot govern a
 * principal without one — see {@code ConnectedUserResourceMembershipResolver.fetchCurrentPrincipalEnvironment} — so for
 * such a caller {@code NOT_GOVERNED} is the designed answer and there is nothing to report. A session principal, a
 * background thread and Community Edition are all silent for the same reason.
 *
 * <p>
 * An earlier version gated on "is this an api-key principal", and argued here that narrowing would hide an api-key
 * token whose provider forgot to carry the environment. That argument was wrong: it assumed every api-key family is one
 * this seam should govern. {@code AbstractApiKeyAuthenticationToken} has eight subclasses and the seam was built for
 * two. {@code AutomationApiKeyAuthenticationToken}, {@code PlatformApiKeyAuthenticationToken},
 * {@code ApiPlatformApiKeyAuthenticationToken}, {@code AiGatewayApiKeyAuthenticationToken} and
 * {@code EmbeddedPlatformUserApiKeyAuthenticationToken} are all built from the {@code User}-only super constructor,
 * carry no environment <em>by design</em>, are ungoverned <em>by design</em>, and authorize correctly through ordinary
 * workspace scopes. Under the old predicate every public-API call they made logged a WARN announcing a 403 that never
 * happened — and a {@code 'Workspace'}-keyed check, which this seam is designed never to govern, tripped it too. If a
 * forgot-the-environment detector is ever wanted it belongs in a separate check keyed by token type, not in this alarm.
 *
 * <p>
 * Silent, too, whenever {@link AutomationAuthorizationContext} skip mode is armed: the check was granted rather than
 * denied, so there is no 403 to announce. The copilot worker threads used to be the interesting exception — they armed
 * a narrower skip mode for an embedded connected user the resolver could not recognise, because {@code TenantContext}
 * did not reach them. Both the missing tenant and that skip mode are gone: those threads now bind the caller's tenant,
 * so a connected user is governed there like anywhere else and a WARN from them means what it means everywhere.
 *
 * <p>
 * <b>Deduplicated by PRINCIPAL, not by (principal, resourceType, id, scope).</b> The question is which callers the seam
 * fails to recognise, never which ids they touched, so the id adds nothing to the answer — and keying on it would make
 * the tracker's growth a function of caller-supplied input. {@link ResourceMembershipDenialLog} keys on the full tuple
 * because a denial genuinely is about the id, and it needs the saturation machinery below precisely because of that.
 * Here the key is bounded by the number of distinct principals, which an id-walking caller cannot inflate.
 *
 * <p>
 * The bound still exists, because the non-JWT embedded api-key mode derives {@code externalUserId} from the request
 * path, so a caller can mint distinct principals. Past {@link #MAX_TRACKED_PRINCIPALS} the dedup set stops growing and
 * with it stops being able to answer "have we already reported this one". At that point per-occurrence logging must go
 * to DEBUG and STAY there — it must never resume per-request WARNs on the theory that the set has room again, because
 * that is exactly the state a caller can drive, and the log would become the amplifier for the probing it exists to
 * report. Only a periodic aggregate stays at WARN. {@link ResourceMembershipDenialLog} had to learn this; it is
 * repeated here rather than inherited so that neither log can quietly lose it.
 *
 * @author Ivica Cardic
 */
public final class ResourceMembershipGovernanceGapLog {

    private static final Logger log = LoggerFactory.getLogger(ResourceMembershipGovernanceGapLog.class);

    private static final int MAX_TRACKED_PRINCIPALS = 1_000;

    private static final long SUPPRESSED_REPORT_INTERVAL = 1_000;

    private static final String GAP_MESSAGE =
        "Api-key principal not governed by the resource-membership seam: principal=[{}] resourceType=[{}] id=[{}] "
            + "scope=[{}] -- this check fell through to the ordinary RBAC path, which for a caller with no platform "
            + "workspace membership denies it. Expect the caller to have received a 403.";

    private static final Set<String> LOGGED_PRINCIPALS = ConcurrentHashMap.newKeySet();

    private static final AtomicLong SUPPRESSED_GAPS = new AtomicLong();

    private ResourceMembershipGovernanceGapLog() {
    }

    /**
     * Drops the JVM-lifetime dedup state. Package-private and test-only: the state is static, so without a reset every
     * test in a JVM would have to pick globally unique principals, and the past-the-bound behaviour could not be
     * exercised at all without permanently saturating the set for whatever ran next.
     */
    static void clearDedupState() {
        LOGGED_PRINCIPALS.clear();

        SUPPRESSED_GAPS.set(0);
    }

    /**
     * Logs at WARN the first time each environment-bearing principal is seen falling through an ungoverned
     * resource-scoped check. Everything else is ignored entirely — see the class Javadoc for why the predicate is this
     * narrow. Past {@link #MAX_TRACKED_PRINCIPALS} distinct principals: DEBUG per occurrence, WARN per
     * {@link #SUPPRESSED_REPORT_INTERVAL} of them.
     */
    public static void logIfGovernanceExpected(Serializable id, String resourceType, String scope) {
        if (PrincipalEnvironment.fetchCurrentPrincipalEnvironmentId()
            .isEmpty()) {

            return;
        }

        if (AutomationAuthorizationContext.isSkipChecks()) {
            return;
        }

        String externalUserId = SecurityUtils.fetchCurrentUserLogin()
            .orElse("<unknown>");

        if (LOGGED_PRINCIPALS.size() < MAX_TRACKED_PRINCIPALS) {
            if (LOGGED_PRINCIPALS.add(externalUserId)) {
                log.warn(GAP_MESSAGE, externalUserId, resourceType, id, scope);
            }

            return;
        }

        log.debug(GAP_MESSAGE, externalUserId, resourceType, id, scope);

        long suppressedGaps = SUPPRESSED_GAPS.incrementAndGet();

        if (suppressedGaps % SUPPRESSED_REPORT_INTERVAL == 0) {
            log.warn(
                "Api-key principals ungoverned by the resource-membership seam past the {}-principal dedup bound: {} "
                    + "further occurrences logged at DEBUG only. Most recent was principal=[{}]; enable DEBUG on this "
                    + "logger for the full list.",
                MAX_TRACKED_PRINCIPALS, suppressedGaps, externalUserId);
        }
    }
}
