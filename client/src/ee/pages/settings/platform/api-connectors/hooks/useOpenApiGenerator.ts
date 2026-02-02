import {
    EndpointDefinitionInput,
    ParameterDefinitionInput,
    ParameterLocation,
    ParameterType,
    RequestBodyDefinitionInput,
    ResponseDefinitionInput,
    useGenerateSpecificationMutation,
} from '@/shared/middleware/graphql';
import {useCallback} from 'react';

import {
    EndpointDefinitionI,
    ParameterDefinitionI,
    ParameterLocationType,
    ParameterTypeType,
    RequestBodyDefinitionI,
    ResponseDefinitionI,
} from '../types/api-connector-wizard.types';

interface UseOpenApiGeneratorProps {
    onError?: (error: Error) => void;
    onSuccess?: (specification: string) => void;
}

interface UseOpenApiGeneratorResultI {
    generateSpecification: (name: string, baseUrl: string | undefined, endpoints: EndpointDefinitionI[]) => void;
    isGenerating: boolean;
}

function mapParameterLocation(location: ParameterLocationType): ParameterLocation {
    const mapping: Record<ParameterLocationType, ParameterLocation> = {
        header: ParameterLocation.Header,
        path: ParameterLocation.Path,
        query: ParameterLocation.Query,
    };

    return mapping[location];
}

function mapParameterType(type: ParameterTypeType): ParameterType {
    const mapping: Record<ParameterTypeType, ParameterType> = {
        array: ParameterType.Array,
        boolean: ParameterType.Boolean,
        integer: ParameterType.Integer,
        number: ParameterType.Number,
        object: ParameterType.Object,
        string: ParameterType.String,
    };

    return mapping[type];
}

function convertParameter(parameter: ParameterDefinitionI): ParameterDefinitionInput {
    return {
        description: parameter.description,
        example: parameter.example,
        location: mapParameterLocation(parameter.in),
        name: parameter.name,
        required: parameter.required,
        type: mapParameterType(parameter.type),
    };
}

function convertRequestBody(requestBody: RequestBodyDefinitionI): RequestBodyDefinitionInput {
    return {
        contentType: requestBody.contentType,
        description: requestBody.description,
        required: requestBody.required,
        schema: requestBody.schema,
    };
}

function convertResponse(response: ResponseDefinitionI): ResponseDefinitionInput {
    return {
        contentType: response.contentType,
        description: response.description,
        schema: response.schema,
        statusCode: response.statusCode,
    };
}

function convertEndpoint(endpoint: EndpointDefinitionI): EndpointDefinitionInput {
    return {
        description: endpoint.description,
        httpMethod: endpoint.httpMethod,
        operationId: endpoint.operationId,
        parameters: endpoint.parameters.map(convertParameter),
        path: endpoint.path,
        requestBody: endpoint.requestBody ? convertRequestBody(endpoint.requestBody) : undefined,
        responses: endpoint.responses.map(convertResponse),
        summary: endpoint.summary,
    };
}

export function useOpenApiGenerator({onError, onSuccess}: UseOpenApiGeneratorProps = {}): UseOpenApiGeneratorResultI {
    const mutation = useGenerateSpecificationMutation({
        onError: (error) => {
            onError?.(error as Error);
        },
        onSuccess: (result) => {
            if (result.generateSpecification.specification) {
                onSuccess?.(result.generateSpecification.specification);
            }
        },
    });

    const generateSpecification = useCallback(
        (name: string, baseUrl: string | undefined, endpoints: EndpointDefinitionI[]) => {
            mutation.mutate({
                input: {
                    baseUrl,
                    endpoints: endpoints.map(convertEndpoint),
                    name,
                },
            });
        },
        [mutation]
    );

    return {
        generateSpecification,
        isGenerating: mutation.isPending,
    };
}
