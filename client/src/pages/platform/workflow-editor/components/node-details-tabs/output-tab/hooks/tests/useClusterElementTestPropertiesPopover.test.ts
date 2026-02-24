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
        it('should filter out parameters with =fromAi() expressions', () => {
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

        it('should keep strings that do not match =fromAi( pattern', () => {
            const currentNode = makeNode({
                description: 'This uses fromAi internally',
                note: 'fromAi(test)',
                prefix: '=notFromAi(value)',
            });

            const properties = [makeProperty('description'), makeProperty('note'), makeProperty('prefix')];

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
                prefix: '=notFromAi(value)',
            });
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

        it('should not set defaultValue from =fromAi() parameters', () => {
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
    });
});
