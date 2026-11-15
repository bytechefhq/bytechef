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

package com.bytechef.automation.configuration.facade;

import java.util.List;

/**
 * Lets an edition add to what {@link WorkspaceConnectionFacade} lists and removes, without replacing the facade. CE
 * registers none; EE lists the organization connections in every workspace and deletes a connection's grants with it.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceConnectionContributor {

    /**
     * The ids of connections outside the workspace that are listed in it as well.
     */
    List<Long> getSharedConnectionIds();

    /**
     * Runs in the delete's transaction, before the connection is removed.
     */
    void beforeDelete(long connectionId);
}
