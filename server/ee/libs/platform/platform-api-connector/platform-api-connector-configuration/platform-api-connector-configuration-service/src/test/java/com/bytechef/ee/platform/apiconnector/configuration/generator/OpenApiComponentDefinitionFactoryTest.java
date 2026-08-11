/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.generator;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Golden tests: the direct spec-to-definition mapper must produce the same definition JSON as the CLI's
 * generate-compile-classload pipeline for the same specification.
 *
 * <p>
 * Specs without a {@code components} section are asserted mapper-only because the old pipeline cannot process them at
 * all: javac was handed a Connection.java that was never generated — the exact shape the manual wizard emits.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class OpenApiComponentDefinitionFactoryTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * The shape the API-connector wizard emits — paths with parameters, request bodies, responses, and servers — plus a
     * minimal security scheme so the legacy generator can process it for the golden comparison.
     */
    private static final String WIZARD_STYLE_SPECIFICATION = """
        openapi: 3.0.0
        info:
          title: Sample Connector
          description: A sample API connector
          version: 1.0.0
        servers:
          - url: https://api.example.com/v1
        paths:
          /pets:
            get:
              operationId: listPets
              summary: List pets
              description: Lists all the pets
              parameters:
                - name: limit
                  in: query
                  required: false
                  description: Page size
                  schema:
                    type: integer
                - name: X-Request-Id
                  in: header
                  required: false
                  schema:
                    type: string
              responses:
                '200':
                  description: Successful response
                  content:
                    application/json:
                      schema:
                        type: array
                        items:
                          type: object
                          properties:
                            id:
                              type: integer
                            name:
                              type: string
            post:
              operationId: createPet
              summary: Create a pet
              requestBody:
                required: true
                content:
                  application/json:
                    schema:
                      type: object
                      required:
                        - name
                      properties:
                        name:
                          type: string
                        tag:
                          type: string
                          enum:
                            - dog
                            - cat
                        age:
                          type: integer
                          minimum: 0
                          maximum: 30
              responses:
                '201':
                  description: Created
          /pets/{petId}:
            delete:
              operationId: deletePet
              parameters:
                - name: petId
                  in: path
                  required: true
                  schema:
                    type: string
              responses:
                '204':
                  description: Deleted
        components:
          securitySchemes:
            api_key:
              type: apiKey
              name: X-Api-Key
              in: header
        """;

    /**
     * Exercises components: $ref schemas (as body root, named property, and array items in output), nested objects,
     * enums, defaults, examples, and the security scheme families the legacy generator can still compile.
     */
    private static final String COMPONENTS_SPECIFICATION = """
        openapi: 3.0.0
        info:
          title: Rich Connector
          version: 1.0.0
        servers:
          - url: https://rich.example.com
        paths:
          /items:
            get:
              operationId: listItems
              summary: List items
              responses:
                '200':
                  description: OK
                  content:
                    application/json:
                      schema:
                        type: array
                        items:
                          $ref: '#/components/schemas/Item'
            post:
              operationId: createItem
              requestBody:
                required: true
                content:
                  application/json:
                    schema:
                      $ref: '#/components/schemas/Item'
              responses:
                '201':
                  description: Created
                  content:
                    application/json:
                      schema:
                        $ref: '#/components/schemas/Item'
        components:
          schemas:
            Item:
              type: object
              required:
                - name
              properties:
                id:
                  type: integer
                  format: int64
                name:
                  type: string
                  example: widget
                price:
                  type: number
                  default: 9.99
                status:
                  type: string
                  enum:
                    - active
                    - retired
                category:
                  $ref: '#/components/schemas/Category'
            Category:
              type: object
              properties:
                id:
                  type: integer
                label:
                  type: string
          securitySchemes:
            api_key:
              type: apiKey
              name: api_key
              in: header
            basic_auth:
              type: http
              scheme: basic
            bearer_auth:
              type: http
              scheme: bearer
            oauth2:
              type: oauth2
              flows:
                authorizationCode:
                  authorizationUrl: https://rich.example.com/oauth/authorize
                  tokenUrl: https://rich.example.com/oauth/token
                  scopes: {}
        """;

    /**
     * A componentless spec — exactly what the manual wizard generates. The legacy pipeline crashes on it
     * (ClassNotFoundException after a failed javac invocation), so this asserts the mapper's own output shape.
     */
    private static final String COMPONENTLESS_SPECIFICATION = """
        openapi: 3.0.0
        info:
          title: Wizard Connector
          version: 1.0.0
        servers:
          - url: https://wizard.example.com
        paths:
          /things:
            get:
              operationId: listThings
              summary: List things
              responses:
                '200':
                  description: OK
        """;

    private static final String OAUTH2_SCOPES_SPECIFICATION = """
        openapi: 3.0.0
        info:
          title: Scoped Connector
          version: 1.0.0
        servers:
          - url: https://scoped.example.com
        paths:
          /records:
            get:
              operationId: listRecords
              security:
                - oauth2:
                    - read:records
              responses:
                '200':
                  description: OK
        components:
          securitySchemes:
            oauth2:
              type: oauth2
              flows:
                clientCredentials:
                  tokenUrl: https://scoped.example.com/oauth/token
                  scopes:
                    read:records: Read access
                    write:records: Write access
        """;

    @TempDir
    private Path tempDir;

    @Test
    void testWizardStyleSpecificationMatchesGeneratorOutput() throws Exception {
        assertMapperMatchesGenerator("samplepets", WIZARD_STYLE_SPECIFICATION);
    }

    @Test
    void testComponentsSpecificationMatchesGeneratorOutput() throws Exception {
        assertMapperMatchesGenerator("richconnector", COMPONENTS_SPECIFICATION);
    }

    @Test
    void testComponentlessSpecificationProducesDefinition() throws Exception {
        JsonNode definition = OBJECT_MAPPER.readTree(
            OpenApiComponentDefinitionFactory.createComponentDefinitionJson(
                "wizardconnector", parseSpecification("wizardconnector", COMPONENTLESS_SPECIFICATION)));

        assertThat(definition.get("name")
            .asText()).isEqualTo("wizardconnector");
        assertThat(definition.get("connection")
            .isNull()).isTrue();

        JsonNode action = definition.get("actions")
            .get(0);

        assertThat(action.get("name")
            .asText()).isEqualTo("listThings");
        assertThat(action.get("metadata")
            .get("method")
            .asText()).isEqualTo("GET");
        assertThat(action.get("metadata")
            .get("path")
            .asText()).isEqualTo("/things");
    }

    @Test
    void testOAuth2ScopesSpecificationMatchesGeneratorOutput() throws Exception {
        assertMapperMatchesGenerator("scopedconnector", OAUTH2_SCOPES_SPECIFICATION);
    }

    private void assertMapperMatchesGenerator(String componentName, String specification) throws Exception {
        OpenAPI openAPI = parseSpecification(componentName, specification);

        String mapperJson = OpenApiComponentDefinitionFactory.createComponentDefinitionJson(componentName, openAPI);

        Path generatorDefinitionPath = OpenApiGenerator.generate(
            componentName, tempDir.resolve(componentName + ".yaml"));

        JsonNode generatorNode = OBJECT_MAPPER.readTree(Files.readString(generatorDefinitionPath));
        JsonNode mapperNode = OBJECT_MAPPER.readTree(mapperJson);

        assertThat(mapperNode).isEqualTo(generatorNode);
    }

    private OpenAPI parseSpecification(String componentName, String specification) throws Exception {
        Path specificationPath = tempDir.resolve(componentName + ".yaml");

        Files.writeString(specificationPath, specification);

        return new OpenAPIParser()
            .readLocation(specificationPath.toString(), null, null)
            .getOpenAPI();
    }
}
