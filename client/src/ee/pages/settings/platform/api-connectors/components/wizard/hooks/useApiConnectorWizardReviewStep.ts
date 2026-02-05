import {toast} from '@/hooks/use-toast';
import {HttpMethod} from '@/shared/middleware/graphql';
import {useCallback, useEffect, useMemo, useRef} from 'react';
import {parse as yamlParse, stringify as yamlStringify} from 'yaml';

import {useApiConnectorWizardStore} from '../../../stores/useApiConnectorWizardStore';
import {EndpointDefinitionI, WizardModeType} from '../../../types/api-connector-wizard.types';
import {safeJsonParse} from '../../../utils/json-utils';

interface UseApiConnectorWizardReviewStepProps {
    mode: WizardModeType;
}

interface UseApiConnectorWizardReviewStepI {
    baseUrl: string | undefined;
    displayEndpoints: EndpointDefinitionI[];
    name: string;
    removeEndpoint: (id: string) => void;
    specification: string | undefined;
}

export default function useApiConnectorWizardReviewStep({
    mode,
}: UseApiConnectorWizardReviewStepProps): UseApiConnectorWizardReviewStepI {
    const {baseUrl, endpoints, name, removeEndpoint, setSpecification, specification} = useApiConnectorWizardStore();
    const specParseErrorShownRef = useRef(false);

    const generateOpenApiSpec = useCallback(() => {
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
                operation.parameters = endpoint.parameters.map((param) => ({
                    description: param.description,
                    in: param.in,
                    name: param.name,
                    required: param.required,
                    schema: {type: param.type},
                }));
            }

            if (endpoint.requestBody) {
                const {data: parsedSchema} = safeJsonParse(
                    endpoint.requestBody.schema,
                    `request body schema for ${endpoint.operationId}`
                );

                operation.requestBody = {
                    content: {
                        [endpoint.requestBody.contentType]: {
                            schema: parsedSchema,
                        },
                    },
                    required: endpoint.requestBody.required,
                };
            }

            endpoint.responses.forEach((response) => {
                const responseObj: Record<string, unknown> = {
                    description: response.description,
                };

                if (response.contentType && response.schema) {
                    const {data: parsedResponseSchema} = safeJsonParse(
                        response.schema,
                        `response schema for ${endpoint.operationId}`
                    );

                    responseObj.content = {
                        [response.contentType]: {
                            schema: parsedResponseSchema,
                        },
                    };
                }

                (operation.responses as Record<string, unknown>)[response.statusCode] = responseObj;
            });

            paths[endpoint.path][method] = operation;
        });

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
    }, [baseUrl, endpoints, name]);

    useEffect(() => {
        if (mode === 'manual' && endpoints.length > 0) {
            const generatedSpec = generateOpenApiSpec();

            setSpecification(generatedSpec);
        }
    }, [endpoints, generateOpenApiSpec, mode, setSpecification]);

    useEffect(() => {
        specParseErrorShownRef.current = false;
    }, [specification]);

    const parseEndpointsFromSpec = useCallback((): EndpointDefinitionI[] => {
        if (!specification || mode === 'manual') {
            return endpoints;
        }

        try {
            const parsed = yamlParse(specification);
            const parsedEndpoints: EndpointDefinitionI[] = [];

            if (parsed.paths) {
                Object.entries(parsed.paths).forEach(([path, methods]) => {
                    Object.entries(methods as Record<string, Record<string, unknown>>).forEach(
                        ([method, operation]) => {
                            const httpMethod = method.toUpperCase() as HttpMethod;

                            if (Object.values(HttpMethod).includes(httpMethod)) {
                                parsedEndpoints.push({
                                    description: (operation.description as string) || '',
                                    httpMethod,
                                    id: `${path}-${method}`,
                                    operationId:
                                        (operation.operationId as string) || `${method}${path.replace(/\//g, '_')}`,
                                    parameters: [],
                                    path,
                                    responses: [],
                                    summary: (operation.summary as string) || '',
                                });
                            }
                        }
                    );
                });
            }

            return parsedEndpoints;
        } catch (error) {
            console.error('Failed to parse API connector specification as YAML:', error);

            if (!specParseErrorShownRef.current) {
                specParseErrorShownRef.current = true;

                toast({
                    description:
                        'The specification could not be parsed. Please check that it is valid YAML/OpenAPI format.',
                    title: 'Failed to parse specification',
                    variant: 'destructive',
                });
            }

            return [];
        }
    }, [endpoints, mode, specification]);

    const displayEndpoints = useMemo(
        () => (mode === 'manual' ? endpoints : parseEndpointsFromSpec()),
        [endpoints, mode, parseEndpointsFromSpec]
    );

    return {
        baseUrl,
        displayEndpoints,
        name,
        removeEndpoint,
        specification,
    };
}
