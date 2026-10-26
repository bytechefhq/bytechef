/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.contextstore;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.tool;
import static com.bytechef.component.definition.datastream.ItemWriter.DESTINATION;
import static com.bytechef.ee.component.contextstore.destination.ContextStoreItemWriter.MODE;
import static com.bytechef.ee.component.contextstore.destination.ContextStoreItemWriter.MODE_FULL_REPLACE;
import static com.bytechef.ee.component.contextstore.destination.ContextStoreItemWriter.MODE_PARTIAL;
import static com.bytechef.ee.component.contextstore.destination.ContextStoreItemWriter.SOURCE_ID;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.ee.component.contextstore.action.ContextStoreGetAction;
import com.bytechef.ee.component.contextstore.action.ContextStoreSearchAction;
import com.bytechef.ee.component.contextstore.action.ContextStoreSearchByStoreAction;
import com.bytechef.ee.component.contextstore.destination.ContextStoreItemWriter;
import com.bytechef.ee.platform.contextstore.service.ContextStoreNameLookupService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreRecordIndexService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreRecordService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Component("contextStore_v1_ComponentHandler")
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class ContextStoreComponentHandler implements ComponentHandler {

    public static final String CONTEXT_STORE = "contextStore";

    private final ComponentDefinition componentDefinition;

    @SuppressFBWarnings("EI2")
    public ContextStoreComponentHandler(
        ContextStoreSourceService contextStoreSourceService,
        ContextStoreRecordService contextStoreRecordService,
        ContextStoreRecordIndexService contextStoreRecordIndexService,
        ContextStoreQueryService contextStoreQueryService,
        ObjectProvider<ContextStoreNameLookupService> contextStoreNameLookupServiceProvider) {

        ModifiableClusterElementDefinition<ContextStoreItemWriter> writeToReplicaClusterElement =
            ComponentDsl.<ContextStoreItemWriter>clusterElement("writeToReplica")
                .title("Write to Context Store")
                .description("Upsert records into the Context Store replica for a configured "
                    + "ContextStoreSource. Tombstones records not seen in the current sync run.")
                .type(DESTINATION)
                .object(() -> new ContextStoreItemWriter(
                    contextStoreSourceService, contextStoreRecordService, contextStoreRecordIndexService))
                .properties(
                    integer(SOURCE_ID)
                        .label("Context Source ID")
                        .description("ID of the ContextStoreSource that owns this sync.")
                        .required(true),
                    string(MODE)
                        .label("Sync mode")
                        .description(
                            "FULL_REPLACE = tombstone records not seen this run (default); "
                                + "PARTIAL = leave unseen records untouched (use for backfills / partial updates).")
                        .options(
                            option(MODE_FULL_REPLACE, MODE_FULL_REPLACE),
                            option(MODE_PARTIAL, MODE_PARTIAL))
                        .defaultValue(MODE_FULL_REPLACE)
                        .required(false));

        ActionDefinition searchAction = ContextStoreSearchAction.of(
            contextStoreQueryService, contextStoreSourceService);
        ActionDefinition searchByStoreAction = ContextStoreSearchByStoreAction.of(
            contextStoreNameLookupServiceProvider, contextStoreQueryService, contextStoreSourceService);
        ActionDefinition getAction = ContextStoreGetAction.of(contextStoreQueryService, contextStoreSourceService);

        this.componentDefinition = component(CONTEXT_STORE)
            .title("Context Store")
            .description("Search and fetch records from the workspace's replicated source data; "
                + "write records as a DataStream sync destination.")
            .icon("path:assets/context-store.svg")
            .categories(ComponentCategory.HELPERS)
            .actions(searchAction, searchByStoreAction, getAction)
            .clusterElements(
                writeToReplicaClusterElement, tool(searchAction), tool(searchByStoreAction), tool(getAction))
            .version(1);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
