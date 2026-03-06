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

package com.bytechef.component.definition;

import com.bytechef.component.definition.ActionContext.Approval.Links;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author Ivica Cardic
 */
public interface ActionContext extends Context {

    /**
     * @param approvalFunction
     */
    Links approval(ContextFunction<Approval, Links> approvalFunction);

    /**
     * @param dataFunction
     * @param <R>
     * @return
     */
    <R> R data(ContextFunction<Data, R> dataFunction);

    /**
     * @param eventConsumer
     */
    void event(Consumer<Event> eventConsumer);

    /**
     * Suspends the current action execution. When called during a perform method, the framework will detect the
     * suspension after perform completes and handle it accordingly, regardless of what perform returns.
     *
     * @param suspend the suspension details including continuation parameters and expiration time
     */
    void suspend(Suspend suspend);

    /**
     *
     */
    @Deprecated
    interface Approval {
        /**
         */
        Links generateLinks();

        record Links(String approvalLink, String disapprovalLink) {
        }
    }

    /**
     *
     */
    interface Event {
        /**
         * @param progress
         */
        void publishActionProgressEvent(int progress);
    }

    /**
     *
     */
    interface Data {

        enum Scope {
            CURRENT_EXECUTION("Current Execution"),
            WORKFLOW("Workflow"),
            PRINCIPAL("Principal"), // ProjectDeployment or IntegrationInstance
            ACCOUNT("Account");

            private final String label;

            Scope(String label) {
                this.label = label;
            }

            public String getLabel() {
                return label;
            }
        }

        /**
         * @param <T>
         * @param scope
         * @param key
         * @return
         */
        <T> Optional<T> fetch(Data.Scope scope, String key);

        /**
         * @param <T>
         * @param scope
         * @param key
         * @return
         */
        <T> T get(Data.Scope scope, String key);

        /**
         * @param <T>
         * @param scope
         * @return
         */
        <T> Map<String, T> getAll(Data.Scope scope);

        /**
         * @param scope
         * @param key
         * @param data
         */
        Void put(Data.Scope scope, String key, Object data);

        /**
         * @param scope
         * @param key
         */
        Void remove(Data.Scope scope, String key);
    }

    /**
     * Represents a suspend state as part of an action execution. It indicates a temporary suspension with accompanying
     * parameters and expiration details.
     *
     * @param continueParameters a map containing parameters required to continue the suspended action
     * @param expiresAt          the timestamp indicating when the suspension expires
     */
    @SuppressFBWarnings("EI")
    record Suspend(Map<String, ?> continueParameters, Instant expiresAt) {
    }
}
