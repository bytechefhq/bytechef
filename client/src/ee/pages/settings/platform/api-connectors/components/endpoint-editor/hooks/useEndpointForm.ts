import {toast} from '@/hooks/use-toast';
import {HttpMethod} from '@/shared/middleware/graphql';
import {useCallback, useEffect, useState} from 'react';
import {UseFormReturn, useForm} from 'react-hook-form';
import {v4 as uuidv4} from 'uuid';
import {parse as yamlParse, stringify as yamlStringify} from 'yaml';

import {
    EndpointDefinitionI,
    ParameterDefinitionI,
    ParameterLocationType,
    ParameterTypeType,
    RequestBodyDefinitionI,
    ResponseDefinitionI,
} from '../../../types/api-connector-wizard.types';
import {safeJsonParse} from '../../../utils/json-utils';

interface EndpointFormDataI {
    description: string;
    httpMethod: HttpMethod;
    operationId: string;
    path: string;
    summary: string;
}

interface UseEndpointFormProps {
    endpoint?: EndpointDefinitionI;
    onClose: () => void;
    onSave: (endpoint: EndpointDefinitionI) => void;
    open: boolean;
}

interface UseEndpointFormI {
    control: UseFormReturn<EndpointFormDataI>['control'];
    editorMode: 'form' | 'yaml';
    handleModeChange: (mode: string) => void;
    handleSaveEndpoint: (data: EndpointFormDataI) => void;
    handleSetParameters: (parameters: ParameterDefinitionI[]) => void;
    handleSetRequestBody: (requestBody: RequestBodyDefinitionI | undefined) => void;
    handleSetResponses: (responses: ResponseDefinitionI[]) => void;
    handleSetYamlValue: (value: string) => void;
    handleSubmit: UseFormReturn<EndpointFormDataI>['handleSubmit'];
    parameters: ParameterDefinitionI[];
    requestBody: RequestBodyDefinitionI | undefined;
    responses: ResponseDefinitionI[];
    yamlValue: string;
}

interface ParsedYamlDataI {
    description: string;
    httpMethod: HttpMethod;
    operationId: string;
    parameters: ParameterDefinitionI[];
    path: string;
    requestBody: RequestBodyDefinitionI | undefined;
    responses: ResponseDefinitionI[];
    summary: string;
}

const DEFAULT_RESPONSE: ResponseDefinitionI = {
    description: 'Successful response',
    statusCode: '200',
};

const defaultEndpointValues: EndpointFormDataI = {
    description: '',
    httpMethod: HttpMethod.Get,
    operationId: '',
    path: '',
    summary: '',
};

export default function useEndpointForm({endpoint, onClose, onSave, open}: UseEndpointFormProps): UseEndpointFormI {
    const [editorMode, setEditorMode] = useState<'form' | 'yaml'>('form');
    const [parameters, setParameters] = useState<ParameterDefinitionI[]>([]);
    const [requestBody, setRequestBody] = useState<RequestBodyDefinitionI | undefined>(undefined);
    const [responses, setResponses] = useState<ResponseDefinitionI[]>([]);
    const [yamlValue, setYamlValue] = useState('');

    const form = useForm<EndpointFormDataI>({
        defaultValues: defaultEndpointValues,
    });

    const {control, getValues, handleSubmit, reset, setValue} = form;

    const generateYamlFromForm = useCallback(() => {
        const formData = getValues();
        const method = formData.httpMethod.toLowerCase();

        const operation: Record<string, unknown> = {
            operationId: formData.operationId,
            responses: {},
        };

        if (formData.summary) {
            operation.summary = formData.summary;
        }

        if (formData.description) {
            operation.description = formData.description;
        }

        if (parameters.length > 0) {
            operation.parameters = parameters.map((param) => ({
                description: param.description,
                example: param.example,
                in: param.in,
                name: param.name,
                required: param.required,
                schema: {type: param.type},
            }));
        }

        if (requestBody) {
            const {data: parsedSchema} = safeJsonParse(requestBody.schema, 'request body schema');

            operation.requestBody = {
                content: {
                    [requestBody.contentType]: {
                        schema: parsedSchema,
                    },
                },
                description: requestBody.description,
                required: requestBody.required,
            };
        }

        responses.forEach((response) => {
            const responseObj: Record<string, unknown> = {
                description: response.description,
            };

            if (response.contentType && response.schema) {
                const {data: parsedResponseSchema} = safeJsonParse(
                    response.schema,
                    `response schema for status ${response.statusCode}`
                );

                responseObj.content = {
                    [response.contentType]: {
                        schema: parsedResponseSchema,
                    },
                };
            }

            (operation.responses as Record<string, unknown>)[response.statusCode] = responseObj;
        });

        const spec = {
            paths: {
                [formData.path]: {
                    [method]: operation,
                },
            },
        };

        return yamlStringify(spec);
    }, [getValues, parameters, requestBody, responses]);

    const parseYamlData = useCallback((yaml: string): ParsedYamlDataI | null => {
        try {
            const parsed = yamlParse(yaml);

            if (!parsed.paths) {
                toast({
                    description: 'The YAML must contain a "paths" section with endpoint definitions.',
                    title: 'Invalid OpenAPI structure',
                    variant: 'destructive',
                });

                return null;
            }

            const pathEntries = Object.entries(parsed.paths);

            if (pathEntries.length === 0) {
                toast({
                    description: 'The "paths" section is empty. Add at least one endpoint path.',
                    title: 'No endpoints found',
                    variant: 'destructive',
                });

                return null;
            }

            const [path, methods] = pathEntries[0];
            const methodEntries = Object.entries(methods as Record<string, Record<string, unknown>>);

            if (methodEntries.length === 0) {
                toast({
                    description: `The path "${path}" has no HTTP methods defined. Add at least one method (get, post, etc.).`,
                    title: 'No methods found',
                    variant: 'destructive',
                });

                return null;
            }

            const [method, operation] = methodEntries[0];

            let parsedParams: ParameterDefinitionI[] = [];

            if (Array.isArray(operation.parameters)) {
                const validLocations: ParameterLocationType[] = ['path', 'query', 'header'];
                const validTypes: ParameterTypeType[] = ['string', 'number', 'integer', 'boolean', 'array', 'object'];

                parsedParams = operation.parameters.map((param: Record<string, unknown>) => {
                    const rawLocation = param.in as string;
                    const location: ParameterLocationType = validLocations.includes(
                        rawLocation as ParameterLocationType
                    )
                        ? (rawLocation as ParameterLocationType)
                        : 'query';

                    const rawType = ((param.schema as Record<string, unknown>)?.type as string) || 'string';
                    const type: ParameterTypeType = validTypes.includes(rawType as ParameterTypeType)
                        ? (rawType as ParameterTypeType)
                        : 'string';

                    return {
                        description: (param.description as string) || '',
                        example: (param.example as string) || '',
                        id: uuidv4(),
                        in: location,
                        name: (param.name as string) || '',
                        required: !!param.required,
                        type,
                    };
                });
            }

            let parsedRequestBody: RequestBodyDefinitionI | undefined;

            if (operation.requestBody) {
                const reqBody = operation.requestBody as Record<string, unknown>;
                const content = reqBody.content as Record<string, Record<string, unknown>>;

                if (content) {
                    const contentType = Object.keys(content)[0];

                    parsedRequestBody = {
                        contentType,
                        description: (reqBody.description as string) || '',
                        required: !!reqBody.required,
                        schema: JSON.stringify(content[contentType]?.schema || {}, null, 2),
                    };
                }
            }

            let parsedResponses: ResponseDefinitionI[] = [];

            if (operation.responses) {
                parsedResponses = Object.entries(operation.responses as Record<string, Record<string, unknown>>).map(
                    ([statusCode, responseData]) => {
                        const content = responseData.content as Record<string, Record<string, unknown>> | undefined;
                        const contentType = content ? Object.keys(content)[0] : undefined;

                        return {
                            contentType,
                            description: (responseData.description as string) || '',
                            schema:
                                contentType && content
                                    ? JSON.stringify(content[contentType]?.schema, null, 2)
                                    : undefined,
                            statusCode,
                        };
                    }
                );
            }

            return {
                description: (operation.description as string) || '',
                httpMethod: method.toUpperCase() as HttpMethod,
                operationId: (operation.operationId as string) || '',
                parameters: parsedParams,
                path,
                requestBody: parsedRequestBody,
                responses: parsedResponses,
                summary: (operation.summary as string) || '',
            };
        } catch (error) {
            console.error('Failed to parse YAML in EndpointForm.parseYamlData:', error);

            toast({
                description: 'Unable to parse the YAML. Please check that your YAML syntax is valid.',
                title: 'Invalid YAML',
                variant: 'destructive',
            });

            return null;
        }
    }, []);

    const parseYamlToForm = useCallback(
        (yaml: string) => {
            const parsedData = parseYamlData(yaml);

            if (!parsedData) {
                return;
            }

            setValue('path', parsedData.path);
            setValue('httpMethod', parsedData.httpMethod);
            setValue('operationId', parsedData.operationId);
            setValue('summary', parsedData.summary);
            setValue('description', parsedData.description);
            setParameters(parsedData.parameters);
            setRequestBody(parsedData.requestBody);
            setResponses(parsedData.responses);
        },
        [parseYamlData, setValue]
    );

    const handleModeChange = useCallback(
        (mode: string) => {
            if (mode === 'yaml' && editorMode === 'form') {
                setYamlValue(generateYamlFromForm());
                setEditorMode('yaml');
            } else if (mode === 'form' && editorMode === 'yaml') {
                let parsed;

                try {
                    parsed = yamlParse(yamlValue);
                } catch {
                    toast({
                        description:
                            'Fix the YAML syntax errors before switching to form view, or your changes may be lost.',
                        title: 'Invalid YAML',
                        variant: 'destructive',
                    });

                    return;
                }

                if (!parsed.paths) {
                    toast({
                        description: 'The YAML must contain a "paths" section to switch to form view.',
                        title: 'Invalid OpenAPI structure',
                        variant: 'destructive',
                    });

                    return;
                }

                parseYamlToForm(yamlValue);
                setEditorMode('form');
            }
        },
        [editorMode, generateYamlFromForm, parseYamlToForm, yamlValue]
    );

    const handleSaveEndpoint = useCallback(
        (data: EndpointFormDataI) => {
            if (editorMode === 'yaml') {
                const parsedData = parseYamlData(yamlValue);

                if (!parsedData) {
                    return;
                }

                const newEndpoint: EndpointDefinitionI = {
                    description: parsedData.description,
                    httpMethod: parsedData.httpMethod,
                    id: endpoint?.id || uuidv4(),
                    operationId: parsedData.operationId,
                    parameters: parsedData.parameters,
                    path: parsedData.path,
                    requestBody: parsedData.requestBody,
                    responses: parsedData.responses,
                    summary: parsedData.summary,
                };

                onSave(newEndpoint);
                onClose();

                return;
            }

            const newEndpoint: EndpointDefinitionI = {
                ...data,
                id: endpoint?.id || uuidv4(),
                parameters,
                requestBody,
                responses,
            };

            onSave(newEndpoint);
            onClose();
        },
        [editorMode, endpoint?.id, onClose, onSave, parameters, parseYamlData, requestBody, responses, yamlValue]
    );

    useEffect(() => {
        if (open) {
            if (endpoint) {
                reset({
                    description: endpoint.description || '',
                    httpMethod: endpoint.httpMethod,
                    operationId: endpoint.operationId,
                    path: endpoint.path,
                    summary: endpoint.summary || '',
                });

                setParameters(endpoint.parameters || []);
                setRequestBody(endpoint.requestBody);
                setResponses(endpoint.responses || []);
            } else {
                reset(defaultEndpointValues);
                setParameters([]);
                setRequestBody(undefined);
                setResponses([DEFAULT_RESPONSE]);
            }

            setEditorMode('form');
        }
    }, [endpoint, open, reset]);

    return {
        control,
        editorMode,
        handleModeChange,
        handleSaveEndpoint,
        handleSetParameters: setParameters,
        handleSetRequestBody: setRequestBody,
        handleSetResponses: setResponses,
        handleSetYamlValue: setYamlValue,
        handleSubmit,
        parameters,
        requestBody,
        responses,
        yamlValue,
    };
}
