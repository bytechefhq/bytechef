import {HttpMethod} from '@/shared/middleware/graphql';
import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {parse as yamlParse} from 'yaml';

import {useApiConnectorWizardStore} from '../../../../stores/useApiConnectorWizardStore';
import {EndpointDefinitionI} from '../../../../types/api-connector-wizard.types';
import useApiConnectorWizardReviewStep from '../useApiConnectorWizardReviewStep';

const hoisted = vi.hoisted(() => ({
    toastError: vi.fn(),
}));

vi.mock('sonner', () => ({
    toast: {error: hoisted.toastError},
}));

const listPetsEndpoint: EndpointDefinitionI = {
    httpMethod: HttpMethod.Get,
    id: 'endpoint-1',
    operationId: 'listPets',
    parameters: [
        {
            description: 'Page size',
            example: '10',
            id: 'parameter-1',
            in: 'query',
            name: 'limit',
            required: false,
            type: 'integer',
        },
    ],
    path: '/pets',
    responses: [
        {
            contentType: 'application/json',
            description: 'The pets',
            schema: '{"type":"array"}',
            statusCode: '200',
        },
        {contentType: 'application/json', description: 'Broken schema', schema: '{broken', statusCode: '500'},
        {description: 'No content', statusCode: '204'},
    ],
};

const createPetEndpoint: EndpointDefinitionI = {
    description: 'Creates a pet',
    httpMethod: HttpMethod.Post,
    id: 'endpoint-2',
    operationId: 'createPet',
    parameters: [],
    path: '/pets',
    requestBody: {contentType: 'application/json', required: true, schema: '{"type":"object"}'},
    responses: [],
    summary: 'Create pet',
};

const updatePetEndpoint: EndpointDefinitionI = {
    httpMethod: HttpMethod.Put,
    id: 'endpoint-3',
    operationId: 'updatePet',
    parameters: [],
    path: '/pets/{petId}',
    requestBody: {contentType: 'application/json', required: false, schema: 'not json'},
    responses: [],
};

const SPECIFICATION = `openapi: 3.0.0
paths:
  /pets:
    parameters:
      - name: shared
        in: query
    get:
      operationId: listPets
      summary: List pets
      description: All pets
    options:
      summary: Unsupported
  /pets/{petId}/photos:
    patch: {}
`;

describe('useApiConnectorWizardReviewStep', () => {
    beforeEach(() => {
        useApiConnectorWizardStore.getState().reset();

        hoisted.toastError.mockReset();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    describe('manual mode', () => {
        it('generates the OpenAPI specification from the defined endpoints', () => {
            vi.spyOn(console, 'error').mockImplementation(() => undefined);

            useApiConnectorWizardStore.setState({
                baseUrl: 'https://api.example.com',
                endpoints: [listPetsEndpoint, createPetEndpoint, updatePetEndpoint],
                name: 'Pets',
            });

            const {result} = renderHook(() => useApiConnectorWizardReviewStep({mode: 'manual'}));

            const specification = useApiConnectorWizardStore.getState().specification;

            expect(result.current.specification).toBe(specification);
            expect(yamlParse(specification!)).toEqual({
                info: {title: 'Pets', version: '1.0.0'},
                openapi: '3.0.0',
                paths: {
                    '/pets': {
                        get: {
                            operationId: 'listPets',
                            parameters: [
                                {
                                    description: 'Page size',
                                    in: 'query',
                                    name: 'limit',
                                    required: false,
                                    schema: {type: 'integer'},
                                },
                            ],
                            responses: {
                                200: {
                                    content: {'application/json': {schema: {type: 'array'}}},
                                    description: 'The pets',
                                },
                                204: {description: 'No content'},
                                500: {description: 'Broken schema'},
                            },
                        },
                        post: {
                            description: 'Creates a pet',
                            operationId: 'createPet',
                            requestBody: {content: {'application/json': {schema: {type: 'object'}}}, required: true},
                            responses: {},
                            summary: 'Create pet',
                        },
                    },
                    '/pets/{petId}': {
                        put: {operationId: 'updatePet', responses: {}},
                    },
                },
                servers: [{url: 'https://api.example.com'}],
            });
            expect(hoisted.toastError).toHaveBeenCalledWith('Invalid JSON', expect.any(Object));
        });

        it('leaves the servers list empty without a base URL', () => {
            useApiConnectorWizardStore.setState({endpoints: [createPetEndpoint], name: 'Pets'});

            renderHook(() => useApiConnectorWizardReviewStep({mode: 'manual'}));

            expect(yamlParse(useApiConnectorWizardStore.getState().specification!).servers).toEqual([]);
        });

        it('clears the specification when there are no endpoints', () => {
            useApiConnectorWizardStore.setState({specification: 'stale: true'});

            renderHook(() => useApiConnectorWizardReviewStep({mode: 'manual'}));

            expect(useApiConnectorWizardStore.getState().specification).toBeUndefined();
        });

        it('regenerates the specification and display list when an endpoint is removed', () => {
            useApiConnectorWizardStore.setState({endpoints: [listPetsEndpoint, createPetEndpoint], name: 'Pets'});

            const {result} = renderHook(() => useApiConnectorWizardReviewStep({mode: 'manual'}));

            expect(result.current.displayEndpoints).toEqual([listPetsEndpoint, createPetEndpoint]);

            act(() => result.current.removeEndpoint('endpoint-1'));

            expect(result.current.displayEndpoints).toEqual([createPetEndpoint]);
            expect(Object.keys(yamlParse(result.current.specification!).paths['/pets'])).toEqual(['post']);
        });
    });

    describe('import and AI modes', () => {
        it('does not overwrite the imported specification', () => {
            useApiConnectorWizardStore.setState({endpoints: [listPetsEndpoint], specification: SPECIFICATION});

            renderHook(() => useApiConnectorWizardReviewStep({mode: 'import'}));

            expect(useApiConnectorWizardStore.getState().specification).toBe(SPECIFICATION);
        });

        it('lists the supported operations of the specification', () => {
            useApiConnectorWizardStore.setState({specification: SPECIFICATION});

            const {result} = renderHook(() => useApiConnectorWizardReviewStep({mode: 'ai'}));

            expect(result.current.displayEndpoints).toEqual([
                {
                    description: 'All pets',
                    httpMethod: HttpMethod.Get,
                    id: '/pets-get',
                    operationId: 'listPets',
                    parameters: [],
                    path: '/pets',
                    responses: [],
                    summary: 'List pets',
                },
                {
                    description: '',
                    httpMethod: HttpMethod.Patch,
                    id: '/pets/{petId}/photos-patch',
                    operationId: 'patch_pets_{petId}_photos',
                    parameters: [],
                    path: '/pets/{petId}/photos',
                    responses: [],
                    summary: '',
                },
            ]);
        });

        it('lists no endpoints for a specification without paths', () => {
            useApiConnectorWizardStore.setState({specification: 'openapi: 3.0.0'});

            const {result} = renderHook(() => useApiConnectorWizardReviewStep({mode: 'import'}));

            expect(result.current.displayEndpoints).toEqual([]);
        });

        it('falls back to the stored endpoints without a specification', () => {
            useApiConnectorWizardStore.setState({endpoints: [listPetsEndpoint]});

            const {result} = renderHook(() => useApiConnectorWizardReviewStep({mode: 'import'}));

            expect(result.current.displayEndpoints).toEqual([listPetsEndpoint]);
        });

        it('reports an unparsable specification once and lists no endpoints', () => {
            vi.spyOn(console, 'error').mockImplementation(() => undefined);

            useApiConnectorWizardStore.setState({name: 'Pets', specification: 'paths: [unclosed'});

            const {rerender, result} = renderHook(() => useApiConnectorWizardReviewStep({mode: 'import'}));

            rerender();

            expect(result.current.displayEndpoints).toEqual([]);
            expect(hoisted.toastError).toHaveBeenCalledTimes(1);
            expect(hoisted.toastError).toHaveBeenCalledWith('Failed to parse specification', expect.any(Object));
        });
    });

    it('exposes the name and base URL of the connector', () => {
        useApiConnectorWizardStore.setState({baseUrl: 'https://api.example.com', name: 'Pets'});

        const {result} = renderHook(() => useApiConnectorWizardReviewStep({mode: 'import'}));

        expect(result.current.name).toBe('Pets');
        expect(result.current.baseUrl).toBe('https://api.example.com');
    });
});
