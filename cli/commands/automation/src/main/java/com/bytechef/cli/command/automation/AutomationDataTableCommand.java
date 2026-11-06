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

package com.bytechef.cli.command.automation;

import com.bytechef.cli.client.automationdatatable.ApiException;
import com.bytechef.cli.client.automationdatatable.model.ColumnTypeModel;
import com.bytechef.cli.client.automationdatatable.model.CreateColumnRequestModel;
import com.bytechef.cli.client.automationdatatable.model.CreateDataTableRequestModel;
import com.bytechef.cli.client.automationdatatable.model.DataTableColumnModel;
import com.bytechef.cli.client.automationdatatable.model.DataTableModel;
import com.bytechef.cli.client.automationdatatable.model.RenameColumnRequestModel;
import com.bytechef.cli.client.automationdatatable.model.UpdateDataTableRequestModel;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.config.Environment;
import com.bytechef.cli.core.config.Overrides;
import com.bytechef.cli.core.config.ProfileResolver;
import com.bytechef.cli.core.error.CliException;
import com.bytechef.cli.core.output.OutputRenderer;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

/**
 * @author Ivica Cardic
 */
@org.springframework.stereotype.Component
public class AutomationDataTableCommand {

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> environmentVariables = System.getenv();

    @Command(name = "automation data-table list", description = "List data tables of a workspace.")
    public void dataTableList(
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "tag") String tag,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = resolve(profile, host, token, environment, workspaceId);

        List<DataTableModel> tables;

        try {
            tables = AutomationClientFactory.dataTableApi(config)
                .listDataTables(config.workspaceId(), null, tag);
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }

        new OutputRenderer(System.out).render(tables, output);
    }

    @Command(name = "automation data-table get", description = "Get a data table.")
    public void dataTableGet(
        @Option(longName = "name", required = true) String name,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = resolve(profile, host, token, environment, null);

        DataTableModel table;

        try {
            table = AutomationClientFactory.dataTableApi(config)
                .getDataTable(name, null);
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }

        new OutputRenderer(System.out).render(table, output);
    }

    @Command(name = "automation data-table create", description = "Create a data table.")
    public void dataTableCreate(
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "name", required = true) String name,
        @Option(longName = "description") String description,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output,
        CommandContext commandContext) {

        List<String> columns = optionValues(commandContext, "column");

        if (columns.isEmpty()) {
            throw new CliException(1, "At least one --column name:TYPE is required.");
        }

        CliConfig config = resolve(profile, host, token, environment, workspaceId);

        CreateDataTableRequestModel request = new CreateDataTableRequestModel()
            .name(name)
            .description(description)
            .columns(columns.stream()
                .map(AutomationDataTableCommand::parseColumn)
                .toList())
            .tags(optionValues(commandContext, "tag"));

        DataTableModel table;

        try {
            table = AutomationClientFactory.dataTableApi(config)
                .createDataTable(config.workspaceId(), request, null);
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }

        new OutputRenderer(System.out).render(table, output);
    }

    @Command(name = "automation data-table update", description = "Update a data table's description or tags.")
    public void dataTableUpdate(
        @Option(longName = "name", required = true) String name,
        @Option(longName = "description") String description,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output,
        CommandContext commandContext) {

        List<String> tags = optionValues(commandContext, "tag");

        CliConfig config = resolve(profile, host, token, environment, null);

        UpdateDataTableRequestModel request = new UpdateDataTableRequestModel()
            .description(description)
            .tags(tags.isEmpty() ? null : tags);

        DataTableModel table;

        try {
            table = AutomationClientFactory.dataTableApi(config)
                .updateDataTable(name, request, null);
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }

        new OutputRenderer(System.out).render(table, output);
    }

    @Command(name = "automation data-table delete", description = "Delete a data table.")
    public void dataTableDelete(
        @Option(longName = "name", required = true) String name,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment, null);

        try {
            AutomationClientFactory.dataTableApi(config)
                .deleteDataTable(name, null);
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }

        System.out.println("Data table deleted.");
    }

    @Command(name = "automation data-table column add", description = "Add a column to a data table.")
    public void dataTableColumnAdd(
        @Option(longName = "name", required = true) String name,
        @Option(longName = "column", required = true) String column,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        DataTableColumnModel parsedColumn = parseColumn(column);

        CliConfig config = resolve(profile, host, token, environment, null);

        CreateColumnRequestModel request = new CreateColumnRequestModel()
            .name(parsedColumn.getName())
            .type(parsedColumn.getType());

        DataTableModel table;

        try {
            table = AutomationClientFactory.dataTableApi(config)
                .createColumn(name, request, null);
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }

        new OutputRenderer(System.out).render(table, output);
    }

    @Command(name = "automation data-table column remove", description = "Remove a column from a data table.")
    public void dataTableColumnRemove(
        @Option(longName = "name", required = true) String name,
        @Option(longName = "column", required = true) String column,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment, null);

        try {
            AutomationClientFactory.dataTableApi(config)
                .deleteColumn(name, column, null);
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }

        System.out.println("Column deleted.");
    }

    @Command(name = "automation data-table column rename", description = "Rename a column of a data table.")
    public void dataTableColumnRename(
        @Option(longName = "name", required = true) String name,
        @Option(longName = "column", required = true) String column,
        @Option(longName = "new-name", required = true) String newName,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = resolve(profile, host, token, environment, null);

        RenameColumnRequestModel request = new RenameColumnRequestModel().newName(newName);

        DataTableModel table;

        try {
            table = AutomationClientFactory.dataTableApi(config)
                .renameColumn(name, column, request, null);
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }

        new OutputRenderer(System.out).render(table, output);
    }

    void setConfigPath(Path configPath) {
        this.configPath = configPath;
    }

    void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    private CliConfig resolve(String profile, String host, String token, String environment, Long workspaceId) {
        return new ProfileResolver(configPath, environmentVariables).resolve(
            new Overrides(
                host, token, environment == null ? null : Environment.valueOf(environment), workspaceId, profile));
    }

    private static List<String> optionValues(CommandContext commandContext, String longName) {
        return commandContext.parsedInput()
            .options()
            .stream()
            .filter(option -> longName.equals(option.longName()))
            .map(CommandOption::value)
            .toList();
    }

    static DataTableColumnModel parseColumn(String spec) {
        int separator = spec.indexOf(':');

        if (separator <= 0 || separator == spec.length() - 1) {
            throw new CliException(1, "A column is name:TYPE, got '" + spec + "'");
        }

        try {
            return new DataTableColumnModel()
                .name(spec.substring(0, separator))
                .type(ColumnTypeModel.fromValue(spec.substring(separator + 1)
                    .toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            throw new CliException(1, "Unknown column type in '" + spec + "'");
        }
    }
}
