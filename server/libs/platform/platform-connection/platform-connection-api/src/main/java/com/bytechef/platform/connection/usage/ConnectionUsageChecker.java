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

package com.bytechef.platform.connection.usage;

import java.util.List;

/**
 * Lets a module outside platform-connection declare that a connection is in use, so the connection cannot be deleted
 * from under it. Each description names the consumer, e.g. {@code "app 'crm-dashboard'"}.
 *
 * @author Ivica Cardic
 */
public interface ConnectionUsageChecker {

    /**
     * Returns one human-readable description per consumer that currently uses the connection; empty when the connection
     * is unused from this checker's point of view.
     */
    List<String> describeUsages(long connectionId);
}
