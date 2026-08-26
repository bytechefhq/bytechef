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

import com.bytechef.automation.configuration.security.ResourceMembershipResolver.Decision;
import com.bytechef.platform.security.util.SecurityUtils;
import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records what {@link ResourceMembershipResolver} answered for a governed principal. Shared by both consumption points
 * ({@code AutomationPermissionEvaluator} and the EE {@code PermissionServiceImpl}) so the two cannot drift on format or
 * volume control.
 *
 * <p>
 * The answer is authoritative — see {@link ResourceMembershipDecider} — so every line here is a denial that actually
 * happened, and this log is how an operator diagnoses one in production.
 *
 * <p>
 * Only {@link Decision#DENIED} and {@link Decision#NOT_APPLICABLE} are logged — {@link Decision#GRANTED} is the
 * expected, safe answer for a governed principal and would only add noise. Deduplicated per distinct (principal,
 * resourceType, id, scope, decision) tuple for the lifetime of the JVM: a builder session that repeats the same check
 * thousands of times (a parameter panel re-checking the same workflow id on every keystroke, say) must not drown the
 * handful of lines that are actually worth reading.
 *
 * <p>
 * Past {@link #MAX_TRACKED_TUPLES} distinct tuples the dedup set stops growing, and with it stops being able to answer
 * "have we already reported this one". Individual denials then drop to DEBUG and only a periodic aggregate stays at
 * WARN. That asymmetry is deliberate: enforcement makes denial the EXPECTED outcome of any id probing by an
 * authenticated connected user, so a single caller walking ids could otherwise drive unbounded WARN volume — the log
 * would become the attack's amplifier. Raising the bound would not fix that; it would only raise the price of
 * admission. The aggregate is what keeps the situation visible without being drivable.
 *
 * @author Ivica Cardic
 */
public final class ResourceMembershipDenialLog {

    private static final Logger log = LoggerFactory.getLogger(ResourceMembershipDenialLog.class);

    private static final int MAX_TRACKED_TUPLES = 10_000;

    private static final long SUPPRESSED_REPORT_INTERVAL = 1_000;

    private static final String DENIAL_MESSAGE =
        "Connected-user resource-membership check denied: principal=[{}] resourceType=[{}] id=[{}] scope=[{}] "
            + "decision={} -- this answer was enforced";

    private static final Set<String> LOGGED_TUPLES = ConcurrentHashMap.newKeySet();

    private static final AtomicLong SUPPRESSED_DENIALS = new AtomicLong();

    private ResourceMembershipDenialLog() {
    }

    /**
     * Drops the JVM-lifetime dedup state. Package-private and test-only: the state is static, so without a reset every
     * test in a JVM would have to pick globally unique ids, and the past-the-bound behaviour could not be exercised at
     * all without permanently saturating the set for whatever ran next.
     */
    static void clearDedupState() {
        LOGGED_TUPLES.clear();

        SUPPRESSED_DENIALS.set(0);
    }

    /**
     * Logs {@code decision} at WARN when it is noteworthy ({@link Decision#DENIED} or {@link Decision#NOT_APPLICABLE})
     * and this exact tuple has not already been logged this JVM lifetime. {@link Decision#GRANTED} is never logged.
     * Both noteworthy decisions deny the check, so a line here is a 403 the caller received. Past
     * {@link #MAX_TRACKED_TUPLES} distinct tuples, see the class Javadoc: DEBUG per denial, WARN per
     * {@link #SUPPRESSED_REPORT_INTERVAL} of them.
     */
    public static void logIfNoteworthy(Decision decision, Serializable id, String resourceType, String scope) {
        if (decision != Decision.DENIED && decision != Decision.NOT_APPLICABLE) {
            return;
        }

        String externalUserId = SecurityUtils.fetchCurrentUserLogin()
            .orElse("<unknown>");

        if (LOGGED_TUPLES.size() < MAX_TRACKED_TUPLES) {
            String tuple = externalUserId + '|' + resourceType + '|' + id + '|' + scope + '|' + decision;

            if (LOGGED_TUPLES.add(tuple)) {
                log.warn(DENIAL_MESSAGE, externalUserId, resourceType, id, scope, decision);
            }

            return;
        }

        log.debug(DENIAL_MESSAGE, externalUserId, resourceType, id, scope, decision);

        long suppressedDenials = SUPPRESSED_DENIALS.incrementAndGet();

        if (suppressedDenials % SUPPRESSED_REPORT_INTERVAL == 0) {
            // Names the principal of the denial that tripped this line. It is a lead, not a verdict -- the counter is
            // global, so a busy multi-tenant JVM may attribute the line to whoever happened to land on the boundary --
            // but a WARN saying only "something is probing" leaves an operator with nowhere to start, and the
            // per-denial detail is at DEBUG by then.
            log.warn(
                "Connected-user resource-membership denials past the {}-tuple dedup bound: {} further denials logged "
                    + "at DEBUG only. Most recent was principal=[{}] resourceType=[{}] id=[{}] scope=[{}]; enable "
                    + "DEBUG on this logger for the full list.",
                MAX_TRACKED_TUPLES, suppressedDenials, externalUserId, resourceType, id, scope);
        }
    }
}
