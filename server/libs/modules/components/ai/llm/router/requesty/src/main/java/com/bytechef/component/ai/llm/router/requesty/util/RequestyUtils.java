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

package com.bytechef.component.ai.llm.router.requesty.util;

import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.TypeReference;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Thibault Jaigu
 */
public class RequestyUtils {

    public static OptionsFunction<String> getRequestyChatModels() {
        return getRequestyModels(model -> true);
    }

    public static OptionsFunction<String> getRequestyImageModels() {
        return getRequestyModels(model -> Boolean.TRUE.equals(model.supports_image_generation()));
    }

    private static OptionsFunction<String> getRequestyModels(Predicate<RequestyModel> filter) {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            ModelsResponse response = context.http(http -> http.get("/models"))
                .configuration(Context.Http.responseType(Context.Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            return response.data()
                .stream()
                .filter(filter)
                .sorted(Comparator.comparingDouble(RequestyModel::totalCost)
                    .thenComparing(RequestyModel::id))
                .map(RequestyUtils::toOption)
                .collect(Collectors.toList());
        };
    }

    private static Option<String> toOption(RequestyModel model) {
        String label = model.isPriced() ? model.id() + " - $" + formatCost(model.totalCost()) : model.id();

        return option(label, model.id(), model.description());
    }

    private static String formatCost(double cost) {
        return new BigDecimal(String.format("%.10f", cost))
            .stripTrailingZeros()
            .toPlainString();
    }

    private record RequestyModel(
        String id, String description, Double input_price, Double output_price, Boolean supports_image_generation) {

        boolean isPriced() {
            return input_price != null || output_price != null;
        }

        double totalCost() {
            return isPriced() ? valueOrZero(input_price) + valueOrZero(output_price) : Double.MAX_VALUE;
        }

        private static double valueOrZero(Double value) {
            return value == null ? 0 : value;
        }
    }

    private record ModelsResponse(List<RequestyModel> data) {
    }
}
