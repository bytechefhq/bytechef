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

package com.bytechef.component.ai.agent.guardrails;

/**
 * Signals that a guardrail could not complete a check because an upstream dependency was unavailable or returned an
 * unusable response (network error, auth/quota failure, malformed response, schema drift). Thrown so the
 * {@code CheckForViolationsAdvisor}'s fail-closed catch block treats the outage as a blocking violation rather than
 * silently passing every check (fail-open) while the dependency is down.
 *
 * <p>
 * <b>Subclass contract.</b> This class is {@code non-sealed} so downstream code may introduce finer-grained upstream
 * failure types (timeout, quota, TLS, rate-limit). Subclasses MUST keep {@link #kind()} returning
 * {@link GuardrailExceptionKind#UPSTREAM_UNAVAILABLE} — the advisor's {@code resolveFailureKind} and external alerting
 * pipelines pattern-match on the stable tag, and a subclass that silently changed the kind would break both. If a new
 * failure mode is genuinely a different kind (e.g. operator configuration), add a new permit to
 * {@link GuardrailException} and a new {@link GuardrailExceptionKind} value instead of subclassing here.
 *
 * @author Ivica Cardic
 */
public non-sealed class GuardrailUnavailableException extends GuardrailException {

    private final String guardrailName;

    public GuardrailUnavailableException(String guardrailName, String reason) {
        super(guardrailName + " guardrail could not complete: " + reason);

        this.guardrailName = guardrailName;
    }

    public GuardrailUnavailableException(String guardrailName, String reason, Throwable cause) {
        super(guardrailName + " guardrail could not complete: " + reason, cause);

        this.guardrailName = guardrailName;
    }

    @Override
    public String guardrailName() {
        return guardrailName;
    }

    @Override
    public GuardrailExceptionKind kind() {
        return GuardrailExceptionKind.UPSTREAM_UNAVAILABLE;
    }
}
