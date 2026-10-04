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

package com.bytechef.platform.mcp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
@SuppressWarnings("unchecked")
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class McpServerServiceTest {

    private static final long MCP_SERVER_ID = 42L;

    private final McpServerRepository mcpServerRepository = mock(McpServerRepository.class);

    @Test
    void testUpdateEnabledTrueRejectsWhenValidatorThrows() {
        McpServerEnablementValidator failingValidator = mock(McpServerEnablementValidator.class);

        RuntimeException validationFailure = new IllegalStateException("workflow 'Unmapped Workflow' has no toolName");

        doThrow(validationFailure).when(failingValidator)
            .validateEnablement(MCP_SERVER_ID);

        McpServerServiceImpl mcpServerService = mcpServerService(failingValidator);

        stubExistingMcpServer(false);

        assertThatThrownBy(() -> mcpServerService.update(MCP_SERVER_ID, null, true))
            .isSameAs(validationFailure)
            .hasMessageContaining("Unmapped Workflow");

        verify(failingValidator).validateEnablement(MCP_SERVER_ID);
        verify(mcpServerRepository, never()).save(any());
    }

    @Test
    void testUpdateEnabledTrueSucceedsWhenValidatorPasses() {
        McpServerEnablementValidator passingValidator = mock(McpServerEnablementValidator.class);

        McpServerServiceImpl mcpServerService = mcpServerService(passingValidator);

        McpServer existingMcpServer = stubExistingMcpServer(false);

        when(mcpServerRepository.save(existingMcpServer)).thenReturn(existingMcpServer);

        McpServer updatedMcpServer = mcpServerService.update(MCP_SERVER_ID, null, true);

        assertThat(updatedMcpServer.isEnabled()).isTrue();

        verify(passingValidator).validateEnablement(MCP_SERVER_ID);
    }

    @Test
    void testUpdateEnabledFalseNeverValidates() {
        McpServerEnablementValidator validator = mock(McpServerEnablementValidator.class);

        McpServerServiceImpl mcpServerService = mcpServerService(validator);

        McpServer existingMcpServer = stubExistingMcpServer(true);

        when(mcpServerRepository.save(existingMcpServer)).thenReturn(existingMcpServer);

        McpServer updatedMcpServer = mcpServerService.update(MCP_SERVER_ID, null, false);

        assertThat(updatedMcpServer.isEnabled()).isFalse();

        verifyNoInteractions(validator);
    }

    @Test
    void testUpdateDoesNotChangeEnvironment() {
        McpServer current = new McpServer("s", PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        current.setId(1L);

        when(mcpServerRepository.findById(1L)).thenReturn(Optional.of(current));
        when(mcpServerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        McpServer incoming = new McpServer("renamed", PlatformType.AUTOMATION, Environment.PRODUCTION);

        incoming.setId(1L);

        McpServer updated = mcpServerService().update(incoming);

        assertThat(updated.getEnvironment()).isEqualTo(Environment.DEVELOPMENT);
        assertThat(updated.getName()).isEqualTo("renamed");
    }

    @Test
    void testUpdateDoesNotChangeSecretKey() {
        McpServer current = new McpServer("s", PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        String currentSecretKey = current.getSecretKey();

        current.setId(1L);

        when(mcpServerRepository.findById(1L)).thenReturn(Optional.of(current));
        when(mcpServerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        McpServer incoming = new McpServer("s", PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        incoming.setId(1L);
        incoming.setSecretKey("attacker-chosen-secret");

        McpServer updated = mcpServerService().update(incoming);

        assertThat(updated.getSecretKey()).isEqualTo(currentSecretKey);
    }

    @Test
    void testRotateSecretKeyReplacesSecretKey() {
        McpServer current = new McpServer("s", PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        String previousSecretKey = current.getSecretKey();

        current.setId(1L);

        when(mcpServerRepository.findById(1L)).thenReturn(Optional.of(current));
        when(mcpServerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        McpServer rotated = mcpServerService().rotateSecretKey(1L);

        assertThat(rotated.getSecretKey())
            .isNotBlank()
            .isNotEqualTo(previousSecretKey);

        verify(mcpServerRepository).save(current);
    }

    @Test
    void testRotateSecretKeyRejectsUnknownServer() {
        when(mcpServerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcpServerService().rotateSecretKey(1L))
            .isInstanceOf(IllegalArgumentException.class);

        verify(mcpServerRepository, never()).save(any());
    }

    private McpServerServiceImpl mcpServerService(McpServerEnablementValidator... mcpServerEnablementValidators) {
        ObjectProvider<McpServerEnablementValidator> mcpServerEnablementValidatorProvider =
            mock(ObjectProvider.class);

        when(mcpServerEnablementValidatorProvider.orderedStream())
            .thenReturn(List.of(mcpServerEnablementValidators)
                .stream());

        return new McpServerServiceImpl(mcpServerEnablementValidatorProvider, mcpServerRepository);
    }

    private McpServer stubExistingMcpServer(boolean initiallyEnabled) {
        McpServer existingMcpServer = new McpServer(
            "test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, initiallyEnabled);

        when(mcpServerRepository.findById(MCP_SERVER_ID)).thenReturn(Optional.of(existingMcpServer));

        return existingMcpServer;
    }
}
