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

package com.bytechef.server.promotion;

import static com.bytechef.ee.automation.promotion.test.assertion.PromotionContributorAssertions.discoverEnvironmentPromotionContributorClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.promotion.contributor.EnvironmentPromotionContributor;
import graphql.language.EnumTypeDefinition;
import graphql.language.EnumTypeExtensionDefinition;
import graphql.language.EnumValueDefinition;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.objenesis.ObjenesisStd;

/**
 * @author Ivica Cardic
 */
class PromotionResourceTypeParityTest {

    private static final String PROMOTION_RESOURCE_TYPE = "PromotionResourceType";

    @Test
    void testGraphQlPromotionResourceTypeValuesEqualRegisteredContributorResourceTypes() throws IOException {
        assertThat(graphQlPromotionResourceTypeValues())
            .containsExactlyInAnyOrderElementsOf(contributorResourceTypes());
    }

    private static Set<String> contributorResourceTypes() {
        ObjenesisStd objenesis = new ObjenesisStd();
        Set<String> resourceTypes = new HashSet<>();

        for (Class<?> contributorClass : discoverEnvironmentPromotionContributorClasses("com.bytechef.ee")) {
            EnvironmentPromotionContributor environmentPromotionContributor =
                (EnvironmentPromotionContributor) objenesis.newInstance(contributorClass);

            resourceTypes.add(environmentPromotionContributor.getResourceType());
        }

        assertThat(resourceTypes).isNotEmpty();

        return resourceTypes;
    }

    private static List<String> graphQlPromotionResourceTypeValues() throws IOException {
        PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        SchemaParser schemaParser = new SchemaParser();

        List<String> enumValues = new ArrayList<>();
        boolean baseEnumFound = false;

        for (Resource resource : resourcePatternResolver.getResources("classpath*:graphql/**/*.graphqls")) {
            String schema = readSchema(resource);

            if (!schema.contains(PROMOTION_RESOURCE_TYPE)) {
                continue;
            }

            TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

            Optional<EnumTypeDefinition> enumTypeDefinitionOptional = typeDefinitionRegistry.getType(
                PROMOTION_RESOURCE_TYPE, EnumTypeDefinition.class);

            if (enumTypeDefinitionOptional.isPresent()) {
                EnumTypeDefinition enumTypeDefinition = enumTypeDefinitionOptional.get();

                baseEnumFound = true;

                addEnumValues(enumValues, enumTypeDefinition.getEnumValueDefinitions());
            }

            Map<String, List<EnumTypeExtensionDefinition>> enumTypeExtensions =
                typeDefinitionRegistry.enumTypeExtensions();

            for (EnumTypeExtensionDefinition enumTypeExtensionDefinition : enumTypeExtensions.getOrDefault(
                PROMOTION_RESOURCE_TYPE, List.of())) {

                addEnumValues(enumValues, enumTypeExtensionDefinition.getEnumValueDefinitions());
            }
        }

        assertThat(baseEnumFound).isTrue();

        return enumValues;
    }

    private static void addEnumValues(List<String> enumValues, List<EnumValueDefinition> enumValueDefinitions) {
        for (EnumValueDefinition enumValueDefinition : enumValueDefinitions) {
            enumValues.add(enumValueDefinition.getName());
        }
    }

    private static String readSchema(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();

            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
