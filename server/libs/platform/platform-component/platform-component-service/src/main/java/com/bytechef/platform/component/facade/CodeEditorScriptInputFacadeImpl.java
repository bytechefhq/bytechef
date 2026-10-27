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

package com.bytechef.platform.component.facade;

import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.script.CodeEditorScriptInputProvider;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class CodeEditorScriptInputFacadeImpl implements CodeEditorScriptInputFacade {

    private final List<CodeEditorScriptInputProvider> codeEditorScriptInputProviders;
    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI")
    public CodeEditorScriptInputFacadeImpl(
        List<CodeEditorScriptInputProvider> codeEditorScriptInputProviders, ConnectionService connectionService) {

        this.codeEditorScriptInputProviders = codeEditorScriptInputProviders;
        this.connectionService = connectionService;
    }

    @Override
    public Map<String, Object> getScriptInputParameters(
        String rootComponentName, int rootComponentVersion, String sourceComponentName, int sourceComponentVersion,
        String sourceClusterElementName, Map<String, ?> sourceInputParameters, @Nullable Long sourceConnectionId) {

        CodeEditorScriptInputProvider codeEditorScriptInputProvider = codeEditorScriptInputProviders.stream()
            .filter(inputProvider -> Objects.equals(inputProvider.getRootComponentName(), rootComponentName))
            .findFirst()
            .orElse(null);

        if (codeEditorScriptInputProvider == null) {
            return Map.of();
        }

        return codeEditorScriptInputProvider.getScriptInputParameters(
            rootComponentVersion, sourceComponentName, sourceComponentVersion, sourceClusterElementName,
            sourceInputParameters, getComponentConnection(sourceConnectionId));
    }

    private @Nullable ComponentConnection getComponentConnection(@Nullable Long connectionId) {
        ComponentConnection componentConnection = null;

        if (connectionId != null) {
            Connection connection = connectionService.getConnection(connectionId);

            componentConnection = new ComponentConnection(
                connection.getComponentName(), connection.getConnectionVersion(), connectionId,
                connection.getParameters(), connection.getAuthorizationType());
        }

        return componentConnection;
    }
}
