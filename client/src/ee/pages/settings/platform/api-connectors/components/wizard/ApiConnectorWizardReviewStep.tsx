import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {toast} from '@/hooks/use-toast';
import {HttpMethod} from '@/shared/middleware/graphql';
import {Trash2Icon} from 'lucide-react';
import {useCallback, useEffect, useRef} from 'react';
import {twMerge} from 'tailwind-merge';
import {parse as yamlParse, stringify as yamlStringify} from 'yaml';

import {useApiConnectorWizardStore} from '../../stores/useApiConnectorWizardStore';
import {EndpointDefinitionI, WizardModeType} from '../../types/api-connector-wizard.types';
import {getHttpMethodBadgeColor} from '../../utils/httpMethodUtils';
import {safeJsonParse} from '../../utils/jsonUtils';

interface ApiConnectorWizardReviewStepProps {
    mode: WizardModeType;
}

const ApiConnectorWizardReviewStep = ({mode}: ApiConnectorWizardReviewStepProps) => {
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

    const parseEndpointsFromSpec = (): EndpointDefinitionI[] => {
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
    };

    const displayEndpoints = mode === 'manual' ? endpoints : parseEndpointsFromSpec();

    return (
        <div className="flex flex-col gap-4 pb-4">
            <div>
                <h3 className="text-sm font-medium">Review API Connector</h3>

                <p className="text-sm text-muted-foreground">Review the endpoints before creating the API connector.</p>
            </div>

            <div className="rounded-md border p-3">
                <p className="text-sm">
                    <span className="font-medium">Name:</span> {name}
                </p>

                {baseUrl && (
                    <p className="text-sm">
                        <span className="font-medium">Base URL:</span> {baseUrl}
                    </p>
                )}

                <p className="text-sm">
                    <span className="font-medium">Endpoints:</span> {displayEndpoints.length}
                </p>
            </div>

            <div>
                <h4 className="mb-2 text-sm font-medium">Endpoints</h4>

                {displayEndpoints.length === 0 ? (
                    <div className="rounded-md border border-dashed p-4 text-center">
                        <p className="text-sm text-muted-foreground">No endpoints found.</p>
                    </div>
                ) : (
                    <ul className="max-h-64 divide-y overflow-y-auto rounded-md border">
                        {displayEndpoints.map((endpoint) => (
                            <li className="flex items-center justify-between p-3" key={endpoint.id}>
                                <div className="flex items-center gap-3">
                                    <Badge
                                        className={twMerge('w-20', getHttpMethodBadgeColor(endpoint.httpMethod))}
                                        label={endpoint.httpMethod}
                                        styleType="outline-outline"
                                        weight="semibold"
                                    />

                                    <div>
                                        <p className="text-sm font-medium">{endpoint.operationId}</p>

                                        <p className="text-xs text-gray-500">{endpoint.path}</p>

                                        {endpoint.summary && (
                                            <p className="text-xs text-gray-400">{endpoint.summary}</p>
                                        )}
                                    </div>
                                </div>

                                {mode === 'manual' && (
                                    <Button
                                        icon={<Trash2Icon className="size-4" />}
                                        onClick={() => removeEndpoint(endpoint.id)}
                                        size="icon"
                                        variant="ghost"
                                    />
                                )}
                            </li>
                        ))}
                    </ul>
                )}
            </div>

            {specification && (
                <div>
                    <h4 className="mb-2 text-sm font-medium">Generated OpenAPI Specification</h4>

                    <pre className="max-h-48 overflow-auto rounded-md bg-gray-100 p-3 text-xs">{specification}</pre>
                </div>
            )}
        </div>
    );
};

export default ApiConnectorWizardReviewStep;
