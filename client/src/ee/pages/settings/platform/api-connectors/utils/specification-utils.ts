import {HttpMethod} from '@/shared/middleware/graphql';
import {v4 as uuidv4} from 'uuid';
import {parse as yamlParse} from 'yaml';

import {
    ApiConnectorWizardActionsI,
    EndpointDefinitionI,
    ParameterDefinitionI,
    ParameterLocationType,
    ParameterTypeType,
    RequestBodyDefinitionI,
    ResponseDefinitionI,
} from '../types/api-connector-wizard.types';

export interface ParsedSpecificationI {
    baseUrl?: string;
    endpoints: EndpointDefinitionI[];
}

const VALID_LOCATIONS: ParameterLocationType[] = ['path', 'query', 'header'];
const VALID_TYPES: ParameterTypeType[] = ['string', 'number', 'integer', 'boolean', 'array', 'object'];

function parseParameters(operation: Record<string, unknown>): ParameterDefinitionI[] {
    if (!Array.isArray(operation.parameters)) {
        return [];
    }

    return operation.parameters
        .filter((parameter): parameter is Record<string, unknown> => !!parameter && typeof parameter === 'object')
        .filter((parameter) => VALID_LOCATIONS.includes(parameter.in as ParameterLocationType))
        .map((parameter) => {
            const schema = parameter.schema as Record<string, unknown> | undefined;
            const rawType = (schema?.type as string) || 'string';

            return {
                description: (parameter.description as string) || '',
                example: parameter.example !== undefined ? String(parameter.example) : '',
                id: uuidv4(),
                in: parameter.in as ParameterLocationType,
                name: (parameter.name as string) || '',
                required: !!parameter.required,
                schema: schema && typeof schema === 'object' ? JSON.stringify(schema, null, 2) : undefined,
                type: VALID_TYPES.includes(rawType as ParameterTypeType) ? (rawType as ParameterTypeType) : 'string',
            };
        });
}

function parseRequestBody(operation: Record<string, unknown>): RequestBodyDefinitionI | undefined {
    if (!operation.requestBody || typeof operation.requestBody !== 'object') {
        return undefined;
    }

    const requestBody = operation.requestBody as Record<string, unknown>;
    const content = requestBody.content as Record<string, Record<string, unknown>> | undefined;

    if (!content) {
        return undefined;
    }

    const contentType = Object.keys(content)[0];

    if (!contentType) {
        return undefined;
    }

    return {
        contentType,
        description: (requestBody.description as string) || '',
        required: !!requestBody.required,
        schema: JSON.stringify(content[contentType]?.schema || {}, null, 2),
    };
}

function parseResponses(operation: Record<string, unknown>): ResponseDefinitionI[] {
    if (!operation.responses || typeof operation.responses !== 'object') {
        return [];
    }

    return Object.entries(operation.responses as Record<string, Record<string, unknown>>).map(
        ([statusCode, responseData]) => {
            const content = responseData?.content as Record<string, Record<string, unknown>> | undefined;
            const contentType = content ? Object.keys(content)[0] : undefined;

            return {
                contentType,
                description: (responseData?.description as string) || '',
                schema: contentType && content ? JSON.stringify(content[contentType]?.schema, null, 2) : undefined,
                statusCode,
            };
        }
    );
}

export function parseSpecificationForWizard(specification: string): ParsedSpecificationI | null {
    let parsed;

    try {
        parsed = yamlParse(specification);
    } catch (error) {
        console.error('Failed to parse API connector specification as YAML:', error);

        return null;
    }

    if (!parsed || typeof parsed !== 'object') {
        return null;
    }

    const servers = parsed.servers as Array<Record<string, unknown>> | undefined;
    const baseUrl = servers?.[0]?.url ? String(servers[0].url) : undefined;

    const endpoints: EndpointDefinitionI[] = [];

    if (parsed.paths && typeof parsed.paths === 'object') {
        Object.entries(parsed.paths as Record<string, Record<string, unknown>>).forEach(([path, methods]) => {
            if (!methods || typeof methods !== 'object') {
                return;
            }

            Object.entries(methods).forEach(([method, operation]) => {
                const httpMethod = method.toUpperCase() as HttpMethod;

                if (!Object.values(HttpMethod).includes(httpMethod) || !operation || typeof operation !== 'object') {
                    return;
                }

                const operationObject = operation as Record<string, unknown>;

                endpoints.push({
                    description: (operationObject.description as string) || '',
                    httpMethod,
                    id: uuidv4(),
                    operationId: (operationObject.operationId as string) || `${method}${path.replace(/\//g, '_')}`,
                    parameters: parseParameters(operationObject),
                    path,
                    requestBody: parseRequestBody(operationObject),
                    responses: parseResponses(operationObject),
                    summary: (operationObject.summary as string) || '',
                });
            });
        });
    }

    return {baseUrl, endpoints};
}

export function buildParameterSchema(parameter: ParameterDefinitionI): Record<string, unknown> {
    if (parameter.schema) {
        try {
            const parsedSchema = JSON.parse(parameter.schema);

            if (
                parsedSchema &&
                typeof parsedSchema === 'object' &&
                (parsedSchema.type ?? 'string') === parameter.type
            ) {
                return parsedSchema;
            }
        } catch (error) {
            console.error('Failed to parse the preserved parameter schema:', error);
        }
    }

    return {type: parameter.type};
}

type WizardHydrationActionsType = Pick<
    ApiConnectorWizardActionsI,
    'setBaseSpecification' | 'setBaseUrl' | 'setEndpoints'
>;

export function hydrateWizardEndpointsFromSpecification(
    specification: string,
    {setBaseSpecification, setBaseUrl, setEndpoints}: WizardHydrationActionsType
): boolean {
    const parsedSpecification = parseSpecificationForWizard(specification);

    if (!parsedSpecification) {
        return false;
    }

    setBaseSpecification(specification);
    setEndpoints(parsedSpecification.endpoints);

    if (parsedSpecification.baseUrl) {
        setBaseUrl(parsedSpecification.baseUrl);
    }

    return true;
}
