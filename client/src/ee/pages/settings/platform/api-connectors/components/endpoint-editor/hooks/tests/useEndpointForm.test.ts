import {HttpMethod} from '@/shared/middleware/graphql';
import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {parse as yamlParse} from 'yaml';

import {EndpointDefinitionI} from '../../../../types/api-connector-wizard.types';
import useEndpointForm from '../useEndpointForm';

const hoisted = vi.hoisted(() => ({
    toastError: vi.fn(),
}));

vi.mock('sonner', () => ({
    toast: {error: hoisted.toastError},
}));

const endpoint: EndpointDefinitionI = {
    description: 'Find a pet by id',
    httpMethod: HttpMethod.Get,
    id: 'endpoint-1',
    operationId: 'getPet',
    parameters: [
        {
            description: 'Pet id',
            example: '42',
            id: 'parameter-1',
            in: 'path',
            name: 'petId',
            required: true,
            type: 'integer',
        },
    ],
    path: '/pets/{petId}',
    requestBody: {
        contentType: 'application/json',
        description: 'Pet filter',
        required: true,
        schema: '{"type":"object"}',
    },
    responses: [
        {
            contentType: 'application/json',
            description: 'The pet',
            schema: '{"type":"object","properties":{"id":{"type":"integer"}}}',
            statusCode: '200',
        },
        {description: 'Not found', statusCode: '404'},
    ],
    summary: 'Get pet',
};

const OPERATION_YAML = `paths:
  /orders/{orderId}:
    put:
      operationId: updateOrder
      summary: Update order
      description: Updates an order
      parameters:
        - name: orderId
          in: path
          required: true
          description: Order id
          example: abc
          schema:
            type: string
        - name: X-Trace
          in: cookie
          schema:
            type: uuid
        - in: query
      requestBody:
        description: The order
        required: true
        content:
          application/xml:
            schema:
              type: object
      responses:
        "200":
          description: Updated
          content:
            application/json:
              schema:
                type: object
        "204": {}
`;

const renderEndpointForm = (props: {endpoint?: EndpointDefinitionI; open?: boolean} = {}) => {
    const onClose = vi.fn();
    const onSave = vi.fn();

    const hook = renderHook(
        ({endpoint, open}: {endpoint?: EndpointDefinitionI; open: boolean}) =>
            useEndpointForm({endpoint, onClose, onSave, open}),
        {initialProps: {endpoint: props.endpoint, open: props.open ?? true}}
    );

    return {...hook, onClose, onSave};
};

describe('useEndpointForm', () => {
    beforeEach(() => {
        hoisted.toastError.mockReset();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    describe('initialization', () => {
        it('starts an added endpoint with default values and a default response', () => {
            const {result} = renderEndpointForm();

            expect(result.current.form.getValues()).toEqual({
                description: '',
                httpMethod: HttpMethod.Get,
                operationId: '',
                path: '',
                summary: '',
            });
            expect(result.current.parameters).toEqual([]);
            expect(result.current.requestBody).toBeUndefined();
            expect(result.current.responses).toEqual([{description: 'Successful response', statusCode: '200'}]);
            expect(result.current.editorMode).toBe('form');
        });

        it('loads an edited endpoint into the form', () => {
            const {result} = renderEndpointForm({endpoint});

            expect(result.current.form.getValues()).toEqual({
                description: 'Find a pet by id',
                httpMethod: HttpMethod.Get,
                operationId: 'getPet',
                path: '/pets/{petId}',
                summary: 'Get pet',
            });
            expect(result.current.parameters).toEqual(endpoint.parameters);
            expect(result.current.requestBody).toEqual(endpoint.requestBody);
            expect(result.current.responses).toEqual(endpoint.responses);
        });

        it('fills missing optional fields of an edited endpoint with empty values', () => {
            const {result} = renderEndpointForm({
                endpoint: {
                    httpMethod: HttpMethod.Post,
                    id: 'endpoint-2',
                    operationId: 'createPet',
                    parameters: undefined as unknown as EndpointDefinitionI['parameters'],
                    path: '/pets',
                    responses: undefined as unknown as EndpointDefinitionI['responses'],
                },
            });

            expect(result.current.form.getValues('description')).toBe('');
            expect(result.current.form.getValues('summary')).toBe('');
            expect(result.current.parameters).toEqual([]);
            expect(result.current.responses).toEqual([]);
        });

        it('does not initialize while the dialog is closed', () => {
            const {rerender, result} = renderEndpointForm({open: false});

            expect(result.current.responses).toEqual([]);

            rerender({endpoint: undefined, open: true});

            expect(result.current.responses).toEqual([{description: 'Successful response', statusCode: '200'}]);
        });

        it('returns to the form view when the dialog is reopened', () => {
            const {rerender, result} = renderEndpointForm({endpoint});

            act(() => result.current.handleModeChange('yaml'));

            expect(result.current.editorMode).toBe('yaml');

            rerender({endpoint, open: false});
            rerender({endpoint, open: true});

            expect(result.current.editorMode).toBe('form');
        });
    });

    describe('switching to YAML', () => {
        it('generates an OpenAPI path item from the form state', () => {
            const {result} = renderEndpointForm({endpoint});

            act(() => result.current.handleModeChange('yaml'));

            expect(result.current.editorMode).toBe('yaml');
            expect(yamlParse(result.current.yamlValue)).toEqual({
                paths: {
                    '/pets/{petId}': {
                        get: {
                            description: 'Find a pet by id',
                            operationId: 'getPet',
                            parameters: [
                                {
                                    description: 'Pet id',
                                    example: '42',
                                    in: 'path',
                                    name: 'petId',
                                    required: true,
                                    schema: {type: 'integer'},
                                },
                            ],
                            requestBody: {
                                content: {'application/json': {schema: {type: 'object'}}},
                                description: 'Pet filter',
                                required: true,
                            },
                            responses: {
                                200: {
                                    content: {
                                        'application/json': {
                                            schema: {properties: {id: {type: 'integer'}}, type: 'object'},
                                        },
                                    },
                                    description: 'The pet',
                                },
                                404: {description: 'Not found'},
                            },
                            summary: 'Get pet',
                        },
                    },
                },
            });
        });

        it('omits empty summary, description, parameters and request body', () => {
            const {result} = renderEndpointForm();

            act(() => {
                result.current.form.setValue('path', '/health');
                result.current.form.setValue('httpMethod', HttpMethod.Delete);
                result.current.form.setValue('operationId', 'deleteHealth');
            });

            act(() => result.current.handleModeChange('yaml'));

            expect(yamlParse(result.current.yamlValue)).toEqual({
                paths: {
                    '/health': {
                        delete: {
                            operationId: 'deleteHealth',
                            responses: {200: {description: 'Successful response'}},
                        },
                    },
                },
            });
        });

        it('replaces an invalid JSON schema with an empty object and warns', () => {
            vi.spyOn(console, 'error').mockImplementation(() => undefined);

            const {result} = renderEndpointForm({
                endpoint: {
                    ...endpoint,
                    requestBody: {contentType: 'application/json', required: false, schema: '{invalid'},
                },
            });

            act(() => result.current.handleModeChange('yaml'));

            expect(yamlParse(result.current.yamlValue).paths['/pets/{petId}'].get.requestBody.content).toEqual({
                'application/json': {schema: {}},
            });
            expect(hoisted.toastError).toHaveBeenCalledWith('Invalid JSON', expect.any(Object));
        });

        it('ignores a request for the mode that is already active', () => {
            const {result} = renderEndpointForm({endpoint});

            act(() => result.current.handleModeChange('form'));

            expect(result.current.editorMode).toBe('form');
            expect(result.current.yamlValue).toBe('');
        });
    });

    describe('switching back to the form', () => {
        it('applies the edited YAML to the form', () => {
            const {result} = renderEndpointForm();

            act(() => result.current.handleModeChange('yaml'));
            act(() => result.current.handleSetYamlValue(OPERATION_YAML));
            act(() => result.current.handleModeChange('form'));

            expect(result.current.editorMode).toBe('form');
            expect(result.current.form.getValues()).toEqual({
                description: 'Updates an order',
                httpMethod: HttpMethod.Put,
                operationId: 'updateOrder',
                path: '/orders/{orderId}',
                summary: 'Update order',
            });
            expect(result.current.parameters).toEqual([
                {
                    description: 'Order id',
                    example: 'abc',
                    id: expect.any(String),
                    in: 'path',
                    name: 'orderId',
                    required: true,
                    type: 'string',
                },
                {
                    description: '',
                    example: '',
                    id: expect.any(String),
                    in: 'query',
                    name: 'X-Trace',
                    required: false,
                    type: 'string',
                },
                {
                    description: '',
                    example: '',
                    id: expect.any(String),
                    in: 'query',
                    name: '',
                    required: false,
                    type: 'string',
                },
            ]);
            expect(result.current.requestBody).toEqual({
                contentType: 'application/xml',
                description: 'The order',
                required: true,
                schema: JSON.stringify({type: 'object'}, null, 2),
            });
            expect(result.current.responses).toEqual([
                {
                    contentType: 'application/json',
                    description: 'Updated',
                    schema: JSON.stringify({type: 'object'}, null, 2),
                    statusCode: '200',
                },
                {contentType: undefined, description: '', schema: undefined, statusCode: '204'},
            ]);
        });

        it('stays in YAML mode when the YAML is not parsable', () => {
            const {result} = renderEndpointForm();

            act(() => result.current.handleModeChange('yaml'));
            act(() => result.current.handleSetYamlValue('paths: [unclosed'));
            act(() => result.current.handleModeChange('form'));

            expect(result.current.editorMode).toBe('yaml');
            expect(hoisted.toastError).toHaveBeenCalledWith('Invalid YAML', expect.any(Object));
        });

        it('stays in YAML mode when the YAML has no paths section', () => {
            const {result} = renderEndpointForm();

            act(() => result.current.handleModeChange('yaml'));
            act(() => result.current.handleSetYamlValue('openapi: 3.0.0'));
            act(() => result.current.handleModeChange('form'));

            expect(result.current.editorMode).toBe('yaml');
            expect(hoisted.toastError).toHaveBeenCalledWith('Invalid OpenAPI structure', expect.any(Object));
        });

        it.each(['', '# only a comment'])('stays in YAML mode when the YAML document %j is empty', (yaml) => {
            const {result} = renderEndpointForm();

            act(() => result.current.handleModeChange('yaml'));
            act(() => result.current.handleSetYamlValue(yaml));

            expect(() => act(() => result.current.handleModeChange('form'))).not.toThrow();
            expect(result.current.editorMode).toBe('yaml');
            expect(hoisted.toastError).toHaveBeenCalledWith('Invalid OpenAPI structure', expect.any(Object));
        });

        it('switches the view but keeps the form when the paths section is empty', () => {
            const {result} = renderEndpointForm({endpoint});

            act(() => result.current.handleModeChange('yaml'));
            act(() => result.current.handleSetYamlValue('paths: {}'));
            act(() => result.current.handleModeChange('form'));

            expect(result.current.editorMode).toBe('form');
            expect(result.current.form.getValues('path')).toBe('/pets/{petId}');
            expect(hoisted.toastError).toHaveBeenCalledWith('No endpoints found', expect.any(Object));
        });
    });

    describe('handleSaveEndpoint', () => {
        it('saves the form state under the id of the edited endpoint', () => {
            const {onClose, onSave, result} = renderEndpointForm({endpoint});

            act(() => result.current.handleSaveEndpoint(result.current.form.getValues()));

            expect(onSave).toHaveBeenCalledWith({
                description: 'Find a pet by id',
                httpMethod: HttpMethod.Get,
                id: 'endpoint-1',
                operationId: 'getPet',
                parameters: endpoint.parameters,
                path: '/pets/{petId}',
                requestBody: endpoint.requestBody,
                responses: endpoint.responses,
                summary: 'Get pet',
            });
            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('assigns a new id to an added endpoint', () => {
            const {onSave, result} = renderEndpointForm();

            act(() => result.current.handleSaveEndpoint({...result.current.form.getValues(), path: '/new'}));

            expect(onSave).toHaveBeenCalledWith(
                expect.objectContaining({id: expect.stringMatching(/^[0-9a-f-]{36}$/), path: '/new'})
            );
        });

        it('saves the parsed YAML in YAML mode', () => {
            const {onClose, onSave, result} = renderEndpointForm({endpoint});

            act(() => result.current.handleModeChange('yaml'));
            act(() => result.current.handleSetYamlValue(OPERATION_YAML));
            act(() => result.current.handleSaveEndpoint(result.current.form.getValues()));

            expect(onSave).toHaveBeenCalledWith(
                expect.objectContaining({
                    description: 'Updates an order',
                    httpMethod: HttpMethod.Put,
                    id: 'endpoint-1',
                    operationId: 'updateOrder',
                    path: '/orders/{orderId}',
                    summary: 'Update order',
                })
            );
            expect(onSave.mock.calls[0][0].parameters).toHaveLength(3);
            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it.each([
            ['paths: [unclosed', 'Invalid YAML'],
            ['openapi: 3.0.0', 'Invalid OpenAPI structure'],
            ['paths: {}', 'No endpoints found'],
            ['paths:\n  /pets: {}', 'No methods found'],
        ])('does not save the YAML %j', (yaml, toastTitle) => {
            vi.spyOn(console, 'error').mockImplementation(() => undefined);

            const {onClose, onSave, result} = renderEndpointForm({endpoint});

            act(() => result.current.handleModeChange('yaml'));
            act(() => result.current.handleSetYamlValue(yaml));
            act(() => result.current.handleSaveEndpoint(result.current.form.getValues()));

            expect(onSave).not.toHaveBeenCalled();
            expect(onClose).not.toHaveBeenCalled();
            expect(hoisted.toastError).toHaveBeenCalledWith(toastTitle, expect.any(Object));
        });
    });

    it('exposes setters for the parameter, request body and response editors', () => {
        const {result} = renderEndpointForm();

        const parameters = endpoint.parameters;
        const responses = [{description: 'Created', statusCode: '201'}];

        act(() => {
            result.current.handleSetParameters(parameters);
            result.current.handleSetRequestBody(endpoint.requestBody);
            result.current.handleSetResponses(responses);
        });

        expect(result.current.parameters).toBe(parameters);
        expect(result.current.requestBody).toBe(endpoint.requestBody);
        expect(result.current.responses).toBe(responses);
    });
});
