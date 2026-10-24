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

package com.bytechef.automation.datasync.exception;

import com.bytechef.automation.datasync.facade.DataSyncFacade;
import com.bytechef.exception.AbstractErrorType;

/**
 * Domain error codes raised by {@link DataSyncFacade}. Keys are numeric and MUST remain stable — downstream consumers
 * (clients, exception resolvers, test assertions) key off {@link #getErrorKey()}.
 *
 * @author Ivica Cardic
 */
public class DataSyncErrorType extends AbstractErrorType {

    /** Deleting a Data Sync whose generated workflow is enabled in a deployment of its project. */
    public static final DataSyncErrorType DATA_SYNC_HAS_DEPLOYMENTS = new DataSyncErrorType(100);

    /** Publishing without a {@code SOURCE} element. */
    public static final DataSyncErrorType SOURCE_MISSING = new DataSyncErrorType(101);

    /** Publishing without a {@code DESTINATION} element. */
    public static final DataSyncErrorType DESTINATION_MISSING = new DataSyncErrorType(102);

    /** Publishing while an element whose component requires a connection has none. */
    public static final DataSyncErrorType ELEMENT_CONNECTION_MISSING = new DataSyncErrorType(103);

    /** Publishing a {@code SCHEDULE} sync whose trigger parameters carry no cron expression. */
    public static final DataSyncErrorType SCHEDULE_EXPRESSION_MISSING = new DataSyncErrorType(104);

    /**
     * Run now on a disabled deployment, or on an enabled deployment whose {@code project_deployment_workflow} row for
     * the Data Sync's own workflow is disabled or absent — the two collapse to the same code because "no row" and
     * "disabled row" both fail the same {@code anyMatch(isEnabled)} check.
     */
    public static final DataSyncErrorType DEPLOYMENT_DISABLED = new DataSyncErrorType(105);

    /** A {@code PROCESSOR} element that is not {@code dataStreamProcessor/v1/fieldMapper}. */
    public static final DataSyncErrorType PROCESSOR_NOT_FIELD_MAPPER = new DataSyncErrorType(106);

    /** A deployment id that does not belong to the Data Sync's own project. */
    public static final DataSyncErrorType DEPLOYMENT_NOT_OWNED = new DataSyncErrorType(107);

    private DataSyncErrorType(int errorKey) {
        super(DataSyncFacade.class, errorKey);
    }
}
