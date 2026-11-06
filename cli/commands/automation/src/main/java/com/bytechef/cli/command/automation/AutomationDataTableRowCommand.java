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

import com.bytechef.cli.client.automationdatatable.ApiClient;
import com.bytechef.cli.client.automationdatatable.ApiException;
import com.bytechef.cli.client.automationdatatable.model.BatchRowModel;
import com.bytechef.cli.client.automationdatatable.model.BatchRowsRequestModel;
import com.bytechef.cli.client.automationdatatable.model.BatchRowsResponseModel;
import com.bytechef.cli.client.automationdatatable.model.ClearRowsResponseModel;
import com.bytechef.cli.client.automationdatatable.model.CreateRowRequestModel;
import com.bytechef.cli.client.automationdatatable.model.CreateStrategyModel;
import com.bytechef.cli.client.automationdatatable.model.DataTableRowModel;
import com.bytechef.cli.client.automationdatatable.model.DeleteRowsResponseModel;
import com.bytechef.cli.client.automationdatatable.model.ImportRowsResponseModel;
import com.bytechef.cli.client.automationdatatable.model.PageModel;
import com.bytechef.cli.client.automationdatatable.model.UpdateRowRequestModel;
import com.bytechef.cli.client.automationdatatable.model.UpsertRowRequestModel;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.config.Environment;
import com.bytechef.cli.core.config.Overrides;
import com.bytechef.cli.core.config.ProfileResolver;
import com.bytechef.cli.core.error.CliException;
import com.bytechef.cli.core.input.CliArgs;
import com.bytechef.cli.core.output.OutputRenderer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

/**
 * @author Ivica Cardic
 */
@org.springframework.stereotype.Component
public class AutomationDataTableRowCommand {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> environmentVariables = System.getenv();

    @Command(name = "automation data-table row list", description = "Query rows of a data table.")
    public void rowList(
        @Option(longName = "name", required = true) String name,
        @Option(longName = "page", defaultValue = "0") Integer page,
        @Option(longName = "page-size") Integer pageSize,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output,
        CommandContext commandContext) {

        List<String> filters = optionValues(commandContext, "filter");
        List<String> sorts = optionValues(commandContext, "sort");

        CliConfig config = resolve(profile, host, token, environment, null);

        PageModel pageModel;

        try {
            pageModel = AutomationClientFactory.dataTableRowApi(config)
                .listRows(name, null, filters, sorts, page, pageSize);
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }

        new OutputRenderer(System.out).render(pageModel, output);
    }

    @Command(name = "automation data-table row get", description = "Get a row by id or external id.")
    public void rowGet(
        @Option(longName = "name", required = true) String name,
        @Option(longName = "id") Long id,
        @Option(longName = "external-id") String externalId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        if ((id == null) == (externalId == null)) {
            throw new CliException(1, "Exactly one of --id or --external-id is required.");
        }

        CliConfig config = resolve(profile, host, token, environment, null);

        DataTableRowModel row;

        try {
            row = id != null
                ? AutomationClientFactory.dataTableRowApi(config)
                    .getRow(name, id, null)
                : AutomationClientFactory.dataTableRowApi(config)
                    .getRowByExternalId(name, externalId, null);
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }

        new OutputRenderer(System.out).render(row, output);
    }

    @Command(name = "automation data-table row insert", description = "Insert a row.")
    public void rowInsert(
        @Option(longName = "name", required = true) String name,
        @Option(longName = "values", required = true) String values,
        @Option(longName = "external-id") String externalId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        CreateRowRequestModel request = new CreateRowRequestModel()
            .values(parseValues(values))
            .externalId(externalId);

        CliConfig config = resolve(profile, host, token, environment, null);

        DataTableRowModel row;

        try {
            row = AutomationClientFactory.dataTableRowApi(config)
                .createRow(name, request, null);
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }

        new OutputRenderer(System.out).render(row, output);
    }

    @Command(name = "automation data-table row update", description = "Update a row by id.")
    public void rowUpdate(
        @Option(longName = "name", required = true) String name,
        @Option(longName = "id", required = true) Long id,
        @Option(longName = "values") String values,
        @Option(longName = "external-id") String externalId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        UpdateRowRequestModel request = new UpdateRowRequestModel()
            .values(values == null ? null : parseValues(values))
            .externalId(externalId);

        CliConfig config = resolve(profile, host, token, environment, null);

        DataTableRowModel row;

        try {
            row = AutomationClientFactory.dataTableRowApi(config)
                .updateRow(name, id, request, null);
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }

        new OutputRenderer(System.out).render(row, output);
    }

    @Command(name = "automation data-table row upsert", description = "Insert or update a row by external id.")
    public void rowUpsert(
        @Option(longName = "name", required = true) String name,
        @Option(longName = "external-id", required = true) String externalId,
        @Option(longName = "values", required = true) String values,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        UpsertRowRequestModel request = new UpsertRowRequestModel().values(parseValues(values));

        CliConfig config = resolve(profile, host, token, environment, null);

        DataTableRowModel row;

        try {
            row = AutomationClientFactory.dataTableRowApi(config)
                .upsertRowByExternalId(name, externalId, request, null);
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }

        new OutputRenderer(System.out).render(row, output);
    }

    @Command(name = "automation data-table row delete", description = "Delete one or more rows.")
    public void rowDelete(
        @Option(longName = "name", required = true) String name,
        @Option(longName = "id") Long id,
        @Option(longName = "ids") String ids,
        @Option(longName = "external-id") String externalId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        List<Long> idList = CliArgs.splitCsvLong(ids);

        int selectorCount = (id != null ? 1 : 0) + (idList != null && !idList.isEmpty() ? 1 : 0)
            + (externalId != null ? 1 : 0);

        if (selectorCount != 1) {
            throw new CliException(1, "Exactly one of --id, --ids or --external-id is required.");
        }

        CliConfig config = resolve(profile, host, token, environment, null);

        try {
            if (id != null) {
                AutomationClientFactory.dataTableRowApi(config)
                    .deleteRow(name, id, null);

                System.out.println("Row deleted.");
            } else if (idList != null && !idList.isEmpty()) {
                DeleteRowsResponseModel response = AutomationClientFactory.dataTableRowApi(config)
                    .deleteRows(name, idList, null);

                new OutputRenderer(System.out).render(response, output);
            } else {
                AutomationClientFactory.dataTableRowApi(config)
                    .deleteRowByExternalId(name, externalId, null);

                System.out.println("Row deleted.");
            }
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }
    }

    @SuppressFBWarnings(
        value = "PATH_TRAVERSAL_IN", justification = "The batch file path is supplied by the CLI user on purpose.")
    @Command(name = "automation data-table row batch", description = "Insert or upsert many rows from a JSON file.")
    public void rowBatch(
        @Option(longName = "name", required = true) String name,
        @Option(longName = "file", required = true) String file,
        @Option(longName = "strategy") String strategy,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        List<BatchRowModel> rows = readBatchRows(file);

        BatchRowsRequestModel request = new BatchRowsRequestModel()
            .rows(rows)
            .createStrategy(strategy == null ? null : parseStrategy(strategy));

        CliConfig config = resolve(profile, host, token, environment, null);

        BatchRowsResponseModel response;

        try {
            response = AutomationClientFactory.dataTableRowApi(config)
                .batchRows(name, request, null);
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }

        new OutputRenderer(System.out).render(response, output);
    }

    @Command(name = "automation data-table row clear", description = "Delete every row of a data table.")
    public void rowClear(
        @Option(longName = "name", required = true) String name,
        @Option(longName = "yes") boolean yes,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        if (!yes) {
            throw new CliException(1, "Refusing to clear without --yes true.");
        }

        CliConfig config = resolve(profile, host, token, environment, null);

        ClearRowsResponseModel response;

        try {
            response = AutomationClientFactory.dataTableRowApi(config)
                .clearRows(name, null);
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }

        new OutputRenderer(System.out).render(response, output);
    }

    @SuppressFBWarnings(
        value = "PATH_TRAVERSAL_IN", justification = "The import file path is supplied by the CLI user on purpose.")
    @Command(name = "automation data-table row import", description = "Import rows from a CSV file.")
    public void rowImport(
        @Option(longName = "name", required = true) String name,
        @Option(longName = "file", required = true) String file,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        String csv = readFile(file);

        CliConfig config = resolve(profile, host, token, environment, null);

        ImportRowsResponseModel response;

        try {
            response = AutomationClientFactory.dataTableRowApi(config)
                .importRows(name, csv, null);
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }

        new OutputRenderer(System.out).render(response, output);
    }

    @SuppressFBWarnings(
        value = "PATH_TRAVERSAL_IN", justification = "The export file path is supplied by the CLI user on purpose.")
    @Command(name = "automation data-table row export", description = "Export rows as CSV.")
    public void rowExport(
        @Option(longName = "name", required = true) String name,
        @Option(longName = "file") String file,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment, null);

        String csv = fetchExportCsv(config, name);

        if (file == null) {
            System.out.print(csv);
        } else {
            try {
                Files.writeString(Path.of(file), csv);
            } catch (Exception e) {
                throw new CliException(1, "Cannot write to " + file + ": " + e.getMessage());
            }
        }
    }

    private static String fetchExportCsv(CliConfig config, String name) {
        ApiClient apiClient = AutomationClientFactory.dataTableApiClient(config);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(apiClient.getBaseUri() + "/data-tables/" + ApiClient.urlEncode(name) + "/rows/export"))
            .header("Accept", "text/csv, application/problem+json")
            .GET();

        Consumer<HttpRequest.Builder> requestInterceptor = apiClient.getRequestInterceptor();

        if (requestInterceptor != null) {
            requestInterceptor.accept(requestBuilder);
        }

        HttpResponse<String> response;

        try {
            response = apiClient.getHttpClient()
                .send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new CliException(1, "Request failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();

            throw new CliException(1, "Request interrupted: " + e.getMessage());
        }

        if (response.statusCode() / 100 != 2) {
            throw AutomationClientFactory.toCliException(response.statusCode());
        }

        return response.body();
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

    private static Map<String, Object> parseValues(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new CliException(1, "Invalid --values JSON: " + e.getMessage());
        }
    }

    private static CreateStrategyModel parseStrategy(String strategy) {
        try {
            return CreateStrategyModel.fromValue(strategy.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new CliException(1, "Unknown --strategy '" + strategy + "'");
        }
    }

    @SuppressFBWarnings(
        value = "PATH_TRAVERSAL_IN", justification = "The batch file path is supplied by the CLI user on purpose.")
    private static List<BatchRowModel> readBatchRows(String file) {
        try {
            return OBJECT_MAPPER.readValue(Files.readString(Path.of(file)),
                new TypeReference<List<BatchRowModel>>() {});
        } catch (Exception e) {
            throw new CliException(1, "Cannot read --file " + file + ": " + e.getMessage());
        }
    }

    @SuppressFBWarnings(
        value = "PATH_TRAVERSAL_IN", justification = "The file path is supplied by the CLI user on purpose.")
    private static String readFile(String file) {
        try {
            return Files.readString(Path.of(file));
        } catch (Exception e) {
            throw new CliException(1, "Cannot read --file " + file + ": " + e.getMessage());
        }
    }
}
