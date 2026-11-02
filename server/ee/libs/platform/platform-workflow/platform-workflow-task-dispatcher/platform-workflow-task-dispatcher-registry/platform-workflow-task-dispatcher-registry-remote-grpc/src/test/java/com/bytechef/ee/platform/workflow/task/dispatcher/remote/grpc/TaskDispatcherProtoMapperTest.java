/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.dynamicProperties;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.fileEntry;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.taskDispatcher;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.task.dispatcher.branch.BranchTaskDispatcherDefinitionFactory;
import com.bytechef.task.dispatcher.condition.ConditionTaskDispatcherDefinitionFactory;
import com.bytechef.task.dispatcher.each.EachTaskDispatcherDefinitionFactory;
import com.bytechef.task.dispatcher.loop.LoopBreakTaskDispatcherDefinitionFactory;
import com.bytechef.task.dispatcher.loop.LoopTaskDispatcherDefinitionFactory;
import com.bytechef.task.dispatcher.map.MapTaskDispatcherDefinitionFactory;
import com.bytechef.task.dispatcher.parallel.ParallelTaskDispatcherDefinitionFactory;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class TaskDispatcherProtoMapperTest {

    private static final String FREE_FORM_FIELDS = ".*(sampleOutput|placeholder|defaultValue|exampleValue|value)";

    private final JsonMapper jsonMapper = JsonMapper.builder()
        .build();
    private final TaskDispatcherProtoMapper taskDispatcherProtoMapper = new TaskDispatcherProtoMapper(jsonMapper);

    @Test
    void testTaskDispatcherDefinitionsRoundTrip() {
        List<TaskDispatcherDefinitionFactory> taskDispatcherDefinitionFactories = List.of(
            new BranchTaskDispatcherDefinitionFactory(Optional.empty()),
            new ConditionTaskDispatcherDefinitionFactory(Optional.empty()),
            new EachTaskDispatcherDefinitionFactory(), new LoopBreakTaskDispatcherDefinitionFactory(),
            new LoopTaskDispatcherDefinitionFactory(), new MapTaskDispatcherDefinitionFactory(Optional.empty()),
            new ParallelTaskDispatcherDefinitionFactory());

        for (TaskDispatcherDefinitionFactory taskDispatcherDefinitionFactory : taskDispatcherDefinitionFactories) {
            TaskDispatcherDefinition taskDispatcherDefinition = new TaskDispatcherDefinition(
                taskDispatcherDefinitionFactory.getDefinition());

            TaskDispatcherDefinition roundTripTaskDispatcherDefinition =
                taskDispatcherProtoMapper.toTaskDispatcherDefinition(
                    taskDispatcherProtoMapper.toProto(taskDispatcherDefinition));

            assertThat(roundTripTaskDispatcherDefinition)
                .as(taskDispatcherDefinition.getName())
                .usingRecursiveComparison()
                .withEqualsForFieldsMatchingRegexes(this::jsonEquals, FREE_FORM_FIELDS)
                .isEqualTo(taskDispatcherDefinition);
        }
    }

    @Test
    void testFileEntryAndDynamicPropertiesRoundTrip() {
        TaskDispatcherDefinition taskDispatcherDefinition = new TaskDispatcherDefinition(
            taskDispatcher("probe")
                .title("Probe")
                .description("Probe dispatcher")
                .properties(
                    fileEntry("file"),
                    dynamicProperties("fields", inputParameters -> List.of())
                        .propertiesLookupDependsOn("file")));

        assertThat(
            taskDispatcherProtoMapper
                .toTaskDispatcherDefinition(taskDispatcherProtoMapper.toProto(taskDispatcherDefinition)))
                    .usingRecursiveComparison()
                    .withEqualsForFieldsMatchingRegexes(this::jsonEquals, FREE_FORM_FIELDS)
                    .isEqualTo(taskDispatcherDefinition);
    }

    @Test
    void testOutputResponseRoundTrip() {
        TaskDispatcherDefinition taskDispatcherDefinition = new TaskDispatcherDefinition(
            new LoopTaskDispatcherDefinitionFactory().getDefinition());

        OutputResponse outputResponse = new OutputResponse(
            taskDispatcherDefinition.getProperties()
                .getFirst(),
            Map.of("item", "x", "index", 1), null);

        assertThat(taskDispatcherProtoMapper
            .toOutputResponse(taskDispatcherProtoMapper.toOutputResponseResult(outputResponse)))
                .usingRecursiveComparison()
                .isEqualTo(outputResponse);
        assertThat(taskDispatcherProtoMapper.toOutputResponse(taskDispatcherProtoMapper.toOutputResponseResult(null)))
            .isNull();
    }

    private boolean jsonEquals(Object actual, Object expected) {
        return Objects.equals(jsonMapper.writeValueAsString(actual), jsonMapper.writeValueAsString(expected));
    }
}
