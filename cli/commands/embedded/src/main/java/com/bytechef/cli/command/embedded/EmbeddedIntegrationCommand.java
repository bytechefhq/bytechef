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

package com.bytechef.cli.command.embedded;

import com.bytechef.cli.client.embeddedconfiguration.ApiException;
import com.bytechef.cli.client.embeddedconfiguration.model.ComponentInputOptionsRequestModel;
import com.bytechef.cli.client.embeddedconfiguration.model.CreateFrontendIntegrationInstanceRequestModel;
import com.bytechef.cli.client.embeddedconfiguration.model.UpdateFrontendIntegrationInstanceWorkflowRequestModel;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.output.OutputRenderer;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

/**
 * Commands for embedded integrations and integration instances on behalf of a connected user.
 *
 * @author Ivica Cardic
 */
@org.springframework.stereotype.Component
public class EmbeddedIntegrationCommand {

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> environmentVariables = System.getenv();

    @Command(name = "embedded integration list", description = "List integrations for a connected user.")
    public void integrationList(
        @Option(longName = "external-user-id", required = true) String externalUserId,
        @Option(longName = "output", defaultValue = "json") String output,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment);

        try {
            new OutputRenderer(System.out).render(
                EmbeddedConfigurationClientFactory.integrationApi(config)
                    .getIntegrations(externalUserId, null),
                output);
        } catch (ApiException e) {
            throw EmbeddedConfigurationClientFactory.toCliException(e);
        }
    }

    @Command(name = "embedded integration get", description = "Get an integration for a connected user.")
    public void integrationGet(
        @Option(longName = "external-user-id", required = true) String externalUserId,
        @Option(longName = "id", required = true) Long id,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment);

        try {
            new OutputRenderer(System.out).renderJson(
                EmbeddedConfigurationClientFactory.integrationApi(config)
                    .getIntegration(externalUserId, id, null));
        } catch (ApiException e) {
            throw EmbeddedConfigurationClientFactory.toCliException(e);
        }
    }

    @Command(
        name = "embedded integration-instance create",
        description = "Create an integration instance from a JSON body.")
    public void integrationInstanceCreate(
        @Option(longName = "external-user-id", required = true) String externalUserId,
        @Option(longName = "id", required = true) Long id,
        @Option(longName = "data", required = true) String data,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment);

        CreateFrontendIntegrationInstanceRequestModel body = EmbeddedSupport.readBody(
            data, CreateFrontendIntegrationInstanceRequestModel.class);

        try {
            Long instanceId = EmbeddedConfigurationClientFactory.integrationInstanceApi(config)
                .createIntegrationInstance(externalUserId, id, body, null);

            new OutputRenderer(System.out).message("Created integration instance " + instanceId + ".");
        } catch (ApiException e) {
            throw EmbeddedConfigurationClientFactory.toCliException(e);
        }
    }

    @Command(name = "embedded integration-instance delete", description = "Delete an integration instance.")
    public void integrationInstanceDelete(
        @Option(longName = "external-user-id", required = true) String externalUserId,
        @Option(longName = "id", required = true) Long id,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment);

        try {
            EmbeddedConfigurationClientFactory.integrationInstanceApi(config)
                .deleteIntegrationInstance(externalUserId, id);

            new OutputRenderer(System.out).message("Deleted integration instance " + id + ".");
        } catch (ApiException e) {
            throw EmbeddedConfigurationClientFactory.toCliException(e);
        }
    }

    @Command(
        name = "embedded integration-instance workflow-enable",
        description = "Enable a workflow on an integration instance.")
    public void integrationInstanceWorkflowEnable(
        @Option(longName = "external-user-id", required = true) String externalUserId,
        @Option(longName = "id", required = true) Long id,
        @Option(longName = "workflow-uuid", required = true) String workflowUuid,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment);

        try {
            EmbeddedConfigurationClientFactory.integrationInstanceWorkflowApi(config)
                .enableIntegrationInstanceWorkflow(externalUserId, id, workflowUuid);

            new OutputRenderer(System.out).message("Enabled workflow " + workflowUuid + ".");
        } catch (ApiException e) {
            throw EmbeddedConfigurationClientFactory.toCliException(e);
        }
    }

    @Command(
        name = "embedded integration-instance workflow-disable",
        description = "Disable a workflow on an integration instance.")
    public void integrationInstanceWorkflowDisable(
        @Option(longName = "external-user-id", required = true) String externalUserId,
        @Option(longName = "id", required = true) Long id,
        @Option(longName = "workflow-uuid", required = true) String workflowUuid,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment);

        try {
            EmbeddedConfigurationClientFactory.integrationInstanceWorkflowApi(config)
                .disableIntegrationInstanceWorkflow(externalUserId, id, workflowUuid);

            new OutputRenderer(System.out).message("Disabled workflow " + workflowUuid + ".");
        } catch (ApiException e) {
            throw EmbeddedConfigurationClientFactory.toCliException(e);
        }
    }

    @Command(
        name = "embedded integration-instance workflow-update",
        description = "Update a workflow's configuration on an integration instance from a JSON body.")
    public void integrationInstanceWorkflowUpdate(
        @Option(longName = "external-user-id", required = true) String externalUserId,
        @Option(longName = "id", required = true) Long id,
        @Option(longName = "workflow-uuid", required = true) String workflowUuid,
        @Option(longName = "data", required = true) String data,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment);

        UpdateFrontendIntegrationInstanceWorkflowRequestModel body = EmbeddedSupport.readBody(
            data, UpdateFrontendIntegrationInstanceWorkflowRequestModel.class);

        try {
            EmbeddedConfigurationClientFactory.integrationInstanceWorkflowApi(config)
                .updateIntegrationInstanceWorkflow(externalUserId, id, workflowUuid, body);

            new OutputRenderer(System.out).message("Updated workflow " + workflowUuid + " configuration.");
        } catch (ApiException e) {
            throw EmbeddedConfigurationClientFactory.toCliException(e);
        }
    }

    @Command(
        name = "embedded integration-instance input-options",
        description = "Fetch dynamic component input options for an integration instance from a JSON body.")
    public void integrationInstanceInputOptions(
        @Option(longName = "external-user-id", required = true) String externalUserId,
        @Option(longName = "id", required = true) Long id,
        @Option(longName = "data", required = true) String data,
        @Option(longName = "output", defaultValue = "json") String output,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment);

        ComponentInputOptionsRequestModel body = EmbeddedSupport.readBody(
            data, ComponentInputOptionsRequestModel.class);

        try {
            new OutputRenderer(System.out).render(
                EmbeddedConfigurationClientFactory.integrationInstanceWorkflowApi(config)
                    .getComponentInputOptions(externalUserId, id, body),
                output);
        } catch (ApiException e) {
            throw EmbeddedConfigurationClientFactory.toCliException(e);
        }
    }

    void setConfigPath(Path configPath) {
        this.configPath = configPath;
    }

    void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    private CliConfig resolve(String profile, String host, String token, String environment) {
        return EmbeddedSupport.resolve(configPath, environmentVariables, profile, host, token, environment);
    }
}
