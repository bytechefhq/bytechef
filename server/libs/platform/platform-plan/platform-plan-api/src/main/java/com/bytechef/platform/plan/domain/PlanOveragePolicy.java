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

package com.bytechef.platform.plan.domain;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * A tenant's on-demand overage terms, modeled on Sim's opt-in overage billing: by default runs stop when the period
 * spend reaches the plan's included monthly cost, but a tenant that opted into on-demand billing keeps running past the
 * ceiling until the <b>unbilled overage</b> (spend minus the included amount) reaches {@code unbilledLimitUsd} — the
 * point at which the billing integration would have auto-invoiced. A null {@code unbilledLimitUsd} means unbounded
 * overage.
 *
 * <p>
 * This is a placeholder seam: no default {@link com.bytechef.platform.plan.provider.PlanOveragePolicyProvider} bean is
 * registered, so overage stays {@link #DISABLED} everywhere until the billing integration contributes a provider.
 * </p>
 *
 * @param enabled          whether the tenant opted into on-demand overage billing
 * @param unbilledLimitUsd overage allowed beyond the included monthly cost before runs hard-stop; null = unbounded
 *
 * @author Ivica Cardic
 */
public record PlanOveragePolicy(boolean enabled, @Nullable BigDecimal unbilledLimitUsd) {

    public static final PlanOveragePolicy DISABLED = new PlanOveragePolicy(false, null);
}
