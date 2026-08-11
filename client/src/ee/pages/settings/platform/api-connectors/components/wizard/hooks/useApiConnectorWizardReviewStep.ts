import {useCallback, useEffect} from 'react';
import {parse as yamlParse, stringify as yamlStringify} from 'yaml';

import {useApiConnectorWizardStore} from '../../../stores/useApiConnectorWizardStore';
import {EndpointDefinitionI} from '../../../types/api-connector-wizard.types';
import {safeJsonParse} from '../../../utils/json-utils';
import {buildParameterSchema} from '../../../utils/specification-utils';

/* Operation-level OpenAPI keys the wizard does not model; carried over from the base specification when an endpoint
 * keeps its path and method, so editing does not silently strip them. */
const PRESERVED_OPERATION_KEYS = ['callbacks', 'deprecated', 'externalDocs', 'security', 'servers', 'tags'];

interface UseApiConnectorWizardReviewStepI {
    baseUrl: string | undefined;
    endpoints: EndpointDefinitionI[];
    name: string;
    removeEndpoint: (id: string) => void;
    specification: string | undefined;
}

export default function useApiConnectorWizardReviewStep(): UseApiConnectorWizardReviewStepI {
    const {baseSpecification, baseUrl, endpoints, name, removeEndpoint, setSpecification, specification} =
        useApiConnectorWizardStore();

    const generateOpenApiSpec = useCallback(() => {
        let baseSpec: Record<string, unknown> | undefined;

        if (baseSpecification) {
            try {
                baseSpec = yamlParse(baseSpecification) as Record<string, unknown>;
            } catch (error) {
                console.error('Failed to parse the base specification, regenerating from scratch:', error);
            }
        }

        const basePaths = baseSpec?.paths as Record<string, Record<string, unknown>> | undefined;

        const paths: Record<string, Record<string, unknown>> = {};

        endpoints.forEach((endpoint) => {
            const method = endpoint.httpMethod.toLowerCase();

            if (!paths[endpoint.path]) {
                paths[endpoint.path] = {};
            }

            const operation: Record<string, unknown> = {
                operationId: endpoint.operationId,
                responses: {},
            };

            if (endpoint.summary) {
                operation.summary = endpoint.summary;
            }

            if (endpoint.description) {
                operation.description = endpoint.description;
            }

            if (endpoint.parameters && endpoint.parameters.length > 0) {
                operation.parameters = endpoint.parameters.map((param) => {
                    const parameterObject: Record<string, unknown> = {
                        description: param.description,
                        in: param.in,
                        name: param.name,
                        required: param.required,
                        schema: buildParameterSchema(param),
                    };

                    if (param.example) {
                        parameterObject.example = param.example;
                    }

                    return parameterObject;
                });
            }

            if (endpoint.requestBody) {
                const {data: parsedSchema, success: schemaParseSuccess} = safeJsonParse(
                    endpoint.requestBody.schema,
                    `request body schema for ${endpoint.operationId}`
                );

                if (schemaParseSuccess) {
                    operation.requestBody = {
                        content: {
                            [endpoint.requestBody.contentType]: {
                                schema: parsedSchema,
                            },
                        },
                        required: endpoint.requestBody.required,
                    };
                }
            }

            endpoint.responses.forEach((response) => {
                const responseObj: Record<string, unknown> = {
                    description: response.description,
                };

                if (response.contentType && response.schema) {
                    const {data: parsedResponseSchema, success: responseSchemaParseSuccess} = safeJsonParse(
                        response.schema,
                        `response schema for ${endpoint.operationId}`
                    );

                    if (responseSchemaParseSuccess) {
                        responseObj.content = {
                            [response.contentType]: {
                                schema: parsedResponseSchema,
                            },
                        };
                    }
                }

                (operation.responses as Record<string, unknown>)[response.statusCode] = responseObj;
            });

            const baseOperation = basePaths?.[endpoint.path]?.[method];

            if (baseOperation && typeof baseOperation === 'object') {
                Object.entries(baseOperation as Record<string, unknown>).forEach(([key, value]) => {
                    if (PRESERVED_OPERATION_KEYS.includes(key) || key.startsWith('x-')) {
                        operation[key] = value;
                    }
                });
            }

            paths[endpoint.path][method] = operation;
        });

        // In edit mode the original specification is the base, so top-level sections the wizard does not model
        // ($ref targets in components, securitySchemes, ...) survive regeneration.
        if (baseSpec) {
            return yamlStringify({
                ...baseSpec,
                paths,
                servers: baseUrl ? [{url: baseUrl}] : (baseSpec.servers ?? []),
            });
        }

        const openApiSpec = {
            info: {
                title: name,
                version: '1.0.0',
            },
            openapi: '3.0.0',
            paths,
            servers: baseUrl ? [{url: baseUrl}] : [],
        };

        return yamlStringify(openApiSpec);
    }, [baseSpecification, baseUrl, endpoints, name]);

    useEffect(() => {
        if (endpoints.length > 0) {
            setSpecification(generateOpenApiSpec());
        } else {
            setSpecification(undefined);
        }
    }, [endpoints, generateOpenApiSpec, setSpecification]);

    return {
        baseUrl,
        endpoints,
        name,
        removeEndpoint,
        specification,
    };
}
