import {NodeDataType, PropertyAllType} from '@/shared/types';
import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import useClusterElementTestPropertiesPopover from '../useClusterElementTestPropertiesPopover';

function makeNode(parameters: Record<string, unknown> = {}): NodeDataType {
    return {
        componentName: 'openai',
        name: 'openai',
        parameters,
        workflowNodeName: 'openai_1',
    } as NodeDataType;
}

function makeProperty(name: string, overrides: Record<string, unknown> = {}): PropertyAllType {
    return {
        name,
        type: 'STRING',
        ...overrides,
    } as PropertyAllType;
}

describe('useClusterElementTestPropertiesPopover', () => {
    const mockOnSubmit = vi.fn();

    beforeEach(() => {
        vi.clearAllMocks();
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('filteredDefaultValues', () => {
        it('should filter out parameters with = prefix expressions', () => {
            const currentNode = makeNode({
                model: 'gpt-4',
                prompt: '=fromAi(description)',
                temperature: 0.7,
                tools: '=fromAi(tools, [tool1])',
            });

            const properties = [makeProperty('model'), makeProperty('prompt'), makeProperty('temperature')];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            const defaultValues = result.current.form.getValues();

            expect(defaultValues).toEqual({
                model: 'gpt-4',
                temperature: 0.7,
            });
            expect(defaultValues).not.toHaveProperty('prompt');
            expect(defaultValues).not.toHaveProperty('tools');
        });

        it('should keep non-string parameter values regardless of content', () => {
            const currentNode = makeNode({
                count: 5,
                enabled: true,
                tags: ['a', 'b'],
            });

            const properties = [makeProperty('count'), makeProperty('enabled'), makeProperty('tags')];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            const defaultValues = result.current.form.getValues();

            expect(defaultValues).toEqual({
                count: 5,
                enabled: true,
                tags: ['a', 'b'],
            });
        });

        it('should handle empty parameters', () => {
            const currentNode = makeNode({});
            const properties = [makeProperty('model')];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            expect(result.current.form.getValues()).toEqual({});
        });

        it('should handle undefined parameters', () => {
            const currentNode = makeNode();

            delete currentNode.parameters;

            const properties = [makeProperty('model')];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            expect(result.current.form.getValues()).toEqual({});
        });

        it('should keep plain strings and filter expressions', () => {
            const currentNode = makeNode({
                description: 'This uses fromAi internally',
                note: 'fromAi(test)',
                prefix: '=notFromAi(value)',
                ref: '${accelo_1.response.id}',
            });

            const properties = [
                makeProperty('description'),
                makeProperty('note'),
                makeProperty('prefix'),
                makeProperty('ref'),
            ];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            const defaultValues = result.current.form.getValues();

            expect(defaultValues).toEqual({
                description: 'This uses fromAi internally',
                note: 'fromAi(test)',
            });
            expect(defaultValues).not.toHaveProperty('prefix');
            expect(defaultValues).not.toHaveProperty('ref');
        });

        it('should filter expressions inside array values', () => {
            const currentNode = makeNode({
                labels: ['hello', '=fromAi(label)', 'world', '${trigger_1.id}'],
                model: 'gpt-4',
            });

            const properties = [makeProperty('labels', {type: 'ARRAY'}), makeProperty('model')];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            expect(result.current.form.getValues()).toEqual({
                labels: ['hello', 'world'],
                model: 'gpt-4',
            });
        });

        it('should filter expressions inside objects nested in arrays', () => {
            const currentNode = makeNode({
                items: [
                    {name: 'keep-me', value: 'plain'},
                    {name: 'drop-expr', value: '=fromAi(v)'},
                ],
            });

            const properties = [makeProperty('items', {type: 'ARRAY'})];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            expect(result.current.form.getValues()).toEqual({
                items: [{name: 'keep-me', value: 'plain'}, {name: 'drop-expr'}],
            });
        });

        it('should recursively filter expressions inside nested objects', () => {
            const currentNode = makeNode({
                fields: {
                    contactId: '${accelo_1.response.id}',
                    name: 'Test Contact',
                    status: '=fromAi(status)',
                },
                model: 'gpt-4',
            });

            const properties = [makeProperty('model'), makeProperty('fields', {type: 'DYNAMIC_PROPERTIES'})];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            const defaultValues = result.current.form.getValues();

            expect(defaultValues).toEqual({
                fields: {
                    name: 'Test Contact',
                },
                model: 'gpt-4',
            });
            expect(defaultValues.fields).not.toHaveProperty('contactId');
            expect(defaultValues.fields).not.toHaveProperty('status');
        });
    });

    describe('propertiesWithDefaults', () => {
        it('should set defaultValue on properties that have matching parameters', () => {
            const currentNode = makeNode({
                model: 'gpt-4',
                temperature: 0.7,
            });

            const properties = [makeProperty('model'), makeProperty('temperature'), makeProperty('unmatched')];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            expect(result.current.propertiesWithDefaults[0].defaultValue).toBe('gpt-4');
            expect(result.current.propertiesWithDefaults[1].defaultValue).toBe(0.7);
            expect(result.current.propertiesWithDefaults[2]).toEqual(properties[2]);
        });

        it('should not set defaultValue from expression parameters', () => {
            const currentNode = makeNode({
                prompt: '=fromAi(description)',
            });

            const properties = [makeProperty('prompt', {defaultValue: 'original'})];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            expect(result.current.propertiesWithDefaults[0].defaultValue).toBe('original');
        });

        it('should skip properties without a name', () => {
            const currentNode = makeNode({model: 'gpt-4'});
            const namelessProperty = makeProperty('', {name: undefined});

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties: [namelessProperty],
                })
            );

            expect(result.current.propertiesWithDefaults[0]).toEqual(namelessProperty);
        });
    });

    describe('handleFormSubmit', () => {
        it('should convert empty strings to null', () => {
            const currentNode = makeNode({});
            const properties: PropertyAllType[] = [];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            act(() => {
                result.current.handleFormSubmit({
                    description: '',
                    model: 'gpt-4',
                    notes: '',
                    temperature: 0.7,
                });
            });

            expect(mockOnSubmit).toHaveBeenCalledWith({
                description: null,
                model: 'gpt-4',
                notes: null,
                temperature: 0.7,
            });
        });

        it('should preserve non-empty string values', () => {
            const currentNode = makeNode({});
            const properties: PropertyAllType[] = [];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            act(() => {
                result.current.handleFormSubmit({
                    model: 'gpt-4',
                    prompt: 'Hello world',
                });
            });

            expect(mockOnSubmit).toHaveBeenCalledWith({
                model: 'gpt-4',
                prompt: 'Hello world',
            });
        });

        it('should preserve null, undefined, and non-string falsy values', () => {
            const currentNode = makeNode({});
            const properties: PropertyAllType[] = [];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            act(() => {
                result.current.handleFormSubmit({
                    count: 0,
                    enabled: false,
                    missing: null,
                    nothing: undefined,
                });
            });

            expect(mockOnSubmit).toHaveBeenCalledWith({
                count: 0,
                enabled: false,
                missing: null,
                nothing: undefined,
            });
        });

        it('should handle empty form values', () => {
            const currentNode = makeNode({});
            const properties: PropertyAllType[] = [];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            act(() => {
                result.current.handleFormSubmit({});
            });

            expect(mockOnSubmit).toHaveBeenCalledWith({});
        });

        it('should convert string "42" to integer 42 for INTEGER properties', () => {
            const currentNode = makeNode({});
            const properties = [makeProperty('count', {type: 'INTEGER'})];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            act(() => {
                result.current.handleFormSubmit({count: '42'});
            });

            expect(mockOnSubmit).toHaveBeenCalledWith({count: 42});
        });

        it('should convert string "3.14" to float 3.14 for NUMBER properties', () => {
            const currentNode = makeNode({});
            const properties = [makeProperty('rate', {type: 'NUMBER'})];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            act(() => {
                result.current.handleFormSubmit({rate: '3.14'});
            });

            expect(mockOnSubmit).toHaveBeenCalledWith({rate: 3.14});
        });

        it('should pass through non-numeric strings for INTEGER properties', () => {
            const currentNode = makeNode({});
            const properties = [makeProperty('count', {type: 'INTEGER'})];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            act(() => {
                result.current.handleFormSubmit({count: 'abc'});
            });

            expect(mockOnSubmit).toHaveBeenCalledWith({count: 'abc'});
        });

        it('should not convert STRING property values that look numeric', () => {
            const currentNode = makeNode({});
            const properties = [makeProperty('label', {type: 'STRING'})];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            act(() => {
                result.current.handleFormSubmit({label: '42'});
            });

            expect(mockOnSubmit).toHaveBeenCalledWith({label: '42'});
        });

        it('should pass through already-numeric values without conversion', () => {
            const currentNode = makeNode({});
            const properties = [makeProperty('count', {type: 'INTEGER'}), makeProperty('rate', {type: 'NUMBER'})];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            act(() => {
                result.current.handleFormSubmit({count: 7, rate: 2.718});
            });

            expect(mockOnSubmit).toHaveBeenCalledWith({count: 7, rate: 2.718});
        });

        it('should truncate decimal for INTEGER using parseInt', () => {
            const currentNode = makeNode({});
            const properties = [makeProperty('count', {type: 'INTEGER'})];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            act(() => {
                result.current.handleFormSubmit({count: '10.7'});
            });

            expect(mockOnSubmit).toHaveBeenCalledWith({count: 10});
        });

        it('should handle mixed property types in one submission', () => {
            const currentNode = makeNode({});
            const properties = [
                makeProperty('count', {type: 'INTEGER'}),
                makeProperty('label', {type: 'STRING'}),
                makeProperty('rate', {type: 'NUMBER'}),
            ];

            const {result} = renderHook(() =>
                useClusterElementTestPropertiesPopover({
                    currentNode,
                    onSubmit: mockOnSubmit,
                    properties,
                })
            );

            act(() => {
                result.current.handleFormSubmit({count: '5', label: 'hello', rate: '9.99'});
            });

            expect(mockOnSubmit).toHaveBeenCalledWith({count: 5, label: 'hello', rate: 9.99});
        });
    });
});
