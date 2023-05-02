
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.helios.project.connection;

import com.bytechef.helios.project.connection.service.ProjectInstanceConnectionService;
import com.bytechef.helios.project.constant.ProjectConstants;
import com.bytechef.hermes.connection.InstanceConnectionFetcher;
import com.bytechef.hermes.connection.domain.Connection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ProjectInstanceConnectionFetcher implements InstanceConnectionFetcher {

    private final ProjectInstanceConnectionService projectInstanceConnectionService;

    @SuppressFBWarnings("EI")
    public ProjectInstanceConnectionFetcher(ProjectInstanceConnectionService projectInstanceConnectionService) {
        this.projectInstanceConnectionService = projectInstanceConnectionService;
    }

    @Override
    public Connection getConnection(String key, String taskName) {
        return projectInstanceConnectionService.getConnection(key, taskName);
    }

    @Override
    public String getType() {
        return ProjectConstants.PROJECT;
    }
}
