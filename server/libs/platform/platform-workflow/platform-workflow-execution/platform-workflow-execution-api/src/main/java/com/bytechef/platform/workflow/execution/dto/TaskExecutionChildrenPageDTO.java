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

package com.bytechef.platform.workflow.execution.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record TaskExecutionChildrenPageDTO(
    List<TaskExecutionDTO> children, Map<String, String> icons, List<List<TaskExecutionDTO>> iterations, int number,
    int size, int numberOfElements, long totalElements, int totalPages) {

    public TaskExecutionChildrenPageDTO {
        Validate.isTrue(children.isEmpty() || iterations.isEmpty(), "children and iterations must not both be set");
    }

    public TaskExecutionChildrenPageDTO(
        List<TaskExecutionDTO> children, List<List<TaskExecutionDTO>> iterations, int number, int size,
        int numberOfElements, long totalElements, int totalPages) {

        this(
            withoutIcons(children), collectIcons(children, iterations), withoutIterationIcons(iterations), number,
            size, numberOfElements, totalElements, totalPages);
    }

    private static Map<String, String> collectIcons(
        List<TaskExecutionDTO> children, List<List<TaskExecutionDTO>> iterations) {

        Stream<TaskExecutionDTO> iterationTaskExecutionDTOs = iterations.stream()
            .flatMap(List::stream);

        Map<String, String> icons = new HashMap<>();

        Stream.concat(children.stream(), iterationTaskExecutionDTOs)
            .filter(taskExecutionDTO -> taskExecutionDTO.type() != null && taskExecutionDTO.icon() != null)
            .forEach(taskExecutionDTO -> icons.putIfAbsent(taskExecutionDTO.type(), taskExecutionDTO.icon()));

        return icons;
    }

    private static List<TaskExecutionDTO> withoutIcons(List<TaskExecutionDTO> taskExecutionDTOs) {
        return taskExecutionDTOs.stream()
            .map(TaskExecutionDTO::withoutIcon)
            .toList();
    }

    private static List<List<TaskExecutionDTO>> withoutIterationIcons(List<List<TaskExecutionDTO>> iterations) {
        return iterations.stream()
            .map(TaskExecutionChildrenPageDTO::withoutIcons)
            .toList();
    }

    public TaskExecutionChildrenPageDTO withoutDetails() {
        return new TaskExecutionChildrenPageDTO(
            children.stream()
                .map(TaskExecutionDTO::withoutDetails)
                .toList(),
            icons,
            iterations.stream()
                .map(iteration -> iteration.stream()
                    .map(TaskExecutionDTO::withoutDetails)
                    .toList())
                .toList(),
            number, size, numberOfElements, totalElements, totalPages);
    }
}
