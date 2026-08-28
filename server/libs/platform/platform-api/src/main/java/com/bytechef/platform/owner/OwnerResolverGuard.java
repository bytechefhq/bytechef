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

package com.bytechef.platform.owner;

import org.springframework.beans.factory.ObjectProvider;

/**
 * Refuses to start an Enterprise app that can serve owner-scoped data but cannot resolve an owner.
 *
 * <p>
 * {@code OwnerResolution} answers "no owner" when the resolver bean is absent, and every caller reads that as
 * unrestricted. In Community that is correct and universal -- there are no connected users. In an Enterprise deployment
 * carrying connected users it means every account sees every account's rows, with no exception and no log line: the
 * most dangerous shape a default can take, because it looks exactly like working software.
 *
 * <p>
 * This fails loudly instead. It is deliberately a hard failure rather than a warning: a warning at boot is read once,
 * by nobody, and the condition it reports is a silent data leak for as long as the app runs.
 *
 * @author Ivica Cardic
 */
public final class OwnerResolverGuard {

    private OwnerResolverGuard() {
    }

    /**
     * @param ownerResolverProvider the provider to probe
     * @param store                 what this app would serve unscoped, named in the failure so the operator knows which
     *                              module pulled the guard in
     */
    public static void check(ObjectProvider<OwnerResolver> ownerResolverProvider, String store) {
        if (ownerResolverProvider.getIfAvailable() != null) {
            return;
        }

        throw new IllegalStateException(
            "No OwnerResolver bean, but this Enterprise app serves " + store + ". Every connected user would read " +
                "every other account's data, silently. Add embedded-configuration-service to this app's classpath, " +
                "or run it as Community if it has no connected users.");
    }
}
