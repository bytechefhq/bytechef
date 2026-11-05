import {encodePath} from '@/pages/platform/workflow-editor/utils/encodingUtils';
import {VALUE_PROPERTY_CONTROL_TYPES} from '@/shared/constants';
import {PropertyAllType} from '@/shared/types';
import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {useObjectProperty} from '../useObjectProperty';

interface MockCurrentNodeI {
    metadata?: {ui?: {dynamicPropertyTypes?: Record<string, string>}};
    parameters?: Record<string, unknown>;
}

const hoisted = vi.hoisted(() => ({
    editorState: {
        updateClusterElementParameterMutation: undefined as object | undefined,
        updateWorkflowNodeParameterMutation: undefined as object | undefined,
    },
    mockSaveProperty: vi.fn(),
    nodeDetailsPanelState: {currentNode: null as MockCurrentNodeI | null},
    workflowState: {workflow: {id: 'workflow-1'} as {id?: string}},
}));

vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: hoisted.mockSaveProperty,
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: Object.assign(
        (selector: (state: typeof hoisted.nodeDetailsPanelState) => unknown) => selector(hoisted.nodeDetailsPanelState),
        {getState: () => hoisted.nodeDetailsPanelState}
    ),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: (selector: (state: typeof hoisted.workflowState) => unknown) => selector(hoisted.workflowState),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => hoisted.editorState,
}));

const workflowNodeParameterMutation = {mutateAsync: vi.fn()};
const clusterElementParameterMutation = {mutateAsync: vi.fn()};

const asProperty = (property: object) => property as unknown as PropertyAllType;

const getSubPropertySummaries = (subProperties: Array<PropertyAllType> | undefined) =>
    subProperties?.map((subProperty) => ({
        controlType: subProperty.controlType,
        name: subProperty.name,
        type: subProperty.type,
    }));

interface RenderPropsI {
    onDeleteClick?: (path: string) => void;
    path?: string;
    property: PropertyAllType;
}

const renderObjectProperty = (props: RenderPropsI) =>
    renderHook((hookProps: RenderPropsI) => useObjectProperty(hookProps), {initialProps: props});

describe('useObjectProperty', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.clearAllMocks();

        hoisted.editorState.updateClusterElementParameterMutation = undefined;
        hoisted.editorState.updateWorkflowNodeParameterMutation = workflowNodeParameterMutation;
        hoisted.nodeDetailsPanelState.currentNode = null;
        hoisted.workflowState.workflow = {id: 'workflow-1'};
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    describe('availablePropertyTypes', () => {
        it('offers every value type when the object has no definitions', () => {
            const {result} = renderObjectProperty({property: asProperty({name: 'settings', type: 'OBJECT'})});

            expect(result.current.availablePropertyTypes).toEqual(
                Object.keys(VALUE_PROPERTY_CONTROL_TYPES).map((type) => ({label: type, value: type}))
            );
            expect(result.current.defaultPropertyType).toBe('STRING');
        });

        it('offers only the typed additional properties', () => {
            const {result} = renderObjectProperty({
                property: asProperty({
                    additionalProperties: [{type: 'NUMBER'}, {}, {type: 'BOOLEAN'}],
                    name: 'settings',
                    type: 'OBJECT',
                }),
            });

            expect(result.current.availablePropertyTypes).toEqual([
                {label: 'NUMBER', value: 'NUMBER'},
                {label: 'BOOLEAN', value: 'BOOLEAN'},
            ]);
            expect(result.current.defaultPropertyType).toBe('NUMBER');
        });

        it('offers nothing when the predefined properties are all non-custom', () => {
            const {result} = renderObjectProperty({
                property: asProperty({
                    additionalProperties: [{type: 'NUMBER'}],
                    name: 'settings',
                    properties: [{name: 'host', type: 'STRING'}],
                    type: 'OBJECT',
                }),
            });

            expect(result.current.availablePropertyTypes).toEqual([]);
        });

        it('offers the additional property types when a predefined property is custom', () => {
            const {result} = renderObjectProperty({
                property: asProperty({
                    additionalProperties: [{type: 'INTEGER'}],
                    name: 'settings',
                    properties: [
                        {name: 'host', type: 'STRING'},
                        {custom: true, name: 'port', type: 'INTEGER'},
                    ],
                    type: 'OBJECT',
                }),
            });

            expect(result.current.availablePropertyTypes).toEqual([{label: 'INTEGER', value: 'INTEGER'}]);
        });
    });

    describe('calculatedPath', () => {
        it('falls back to the property name without a path', () => {
            const {result} = renderObjectProperty({
                property: asProperty({label: 'Settings', name: 'settings', placeholder: 'Add setting'}),
            });

            expect(result.current.calculatedPath).toBe('settings');
            expect(result.current.isContainerObject).toBe(false);
            expect(result.current.label).toBe('Settings');
            expect(result.current.name).toBe('settings');
            expect(result.current.placeholder).toBe('Add setting');
        });

        it('keeps an explicit path', () => {
            const {result} = renderObjectProperty({
                path: 'request.settings',
                property: asProperty({name: 'settings'}),
            });

            expect(result.current.calculatedPath).toBe('request.settings');
        });

        it('strips the __item segment of a container object', () => {
            const {result} = renderObjectProperty({
                path: 'items[0].__item',
                property: asProperty({name: '__item'}),
            });

            expect(result.current.isContainerObject).toBe(true);
            expect(result.current.calculatedPath).toBe('items[0]');
        });
    });

    describe('sub-properties built from the parameters', () => {
        it('stays empty without current node parameters', () => {
            const {result} = renderObjectProperty({property: asProperty({name: 'settings', type: 'OBJECT'})});

            expect(result.current.subProperties).toBeUndefined();
            expect(result.current.existingSubPropertyNames).toEqual([]);
        });

        it('stays empty when the value at the path is not an object', () => {
            hoisted.nodeDetailsPanelState.currentNode = {parameters: {settings: '${trigger_1.settings}'}};

            const {result} = renderObjectProperty({property: asProperty({name: 'settings', type: 'OBJECT'})});

            expect(result.current.subProperties).toBeUndefined();
        });

        it('infers the type and control type of every stored value', () => {
            hoisted.nodeDetailsPanelState.currentNode = {
                parameters: {
                    settings: {
                        count: 5,
                        day: '2026-01-02',
                        enabled: true,
                        nested: {inner: 1},
                        ratio: 1.5,
                        tags: ['a'],
                        timestamp: '2026-01-02T10:00:00Z',
                        title: 'Hello',
                    },
                },
            };

            const {result} = renderObjectProperty({property: asProperty({name: 'settings', type: 'OBJECT'})});

            expect(getSubPropertySummaries(result.current.subProperties)).toEqual([
                {controlType: 'INTEGER', name: 'count', type: 'INTEGER'},
                {controlType: 'DATE', name: 'day', type: 'DATE'},
                {controlType: 'SELECT', name: 'enabled', type: 'BOOLEAN'},
                {controlType: 'OBJECT_BUILDER', name: 'nested', type: 'OBJECT'},
                {controlType: 'NUMBER', name: 'ratio', type: 'NUMBER'},
                {controlType: 'ARRAY_BUILDER', name: 'tags', type: 'ARRAY'},
                {controlType: 'DATE_TIME', name: 'timestamp', type: 'DATE_TIME'},
                {controlType: 'TEXT', name: 'title', type: 'STRING'},
            ]);
            expect(result.current.existingSubPropertyNames).toEqual([
                'count',
                'day',
                'enabled',
                'nested',
                'ratio',
                'tags',
                'timestamp',
                'title',
            ]);
        });

        it('marks stored values as custom, expression-enabled entries with their value as default', () => {
            hoisted.nodeDetailsPanelState.currentNode = {parameters: {settings: {title: 'Hello'}}};

            const {result} = renderObjectProperty({
                property: asProperty({additionalProperties: [{type: 'STRING'}], name: 'settings', type: 'OBJECT'}),
            });

            expect(result.current.subProperties?.[0]).toEqual(
                expect.objectContaining({
                    additionalProperties: [{type: 'STRING'}],
                    custom: true,
                    defaultValue: 'Hello',
                    expressionEnabled: true,
                    label: 'title',
                    name: 'title',
                })
            );
        });

        it('decodes encoded keys for the label and re-encodes them for the name', () => {
            hoisted.nodeDetailsPanelState.currentNode = {parameters: {settings: {'first name': 'Ada'}}};

            const {result} = renderObjectProperty({property: asProperty({name: 'settings', type: 'OBJECT'})});

            expect(result.current.subProperties).toHaveLength(1);
            expect(result.current.subProperties?.[0].label).toBe('first name');
            expect(result.current.subProperties?.[0].name).toBe(encodePath('first name'));
            expect(result.current.subProperties?.[0].defaultValue).toBe('Ada');
        });

        it('reads the object at an explicit nested path', () => {
            hoisted.nodeDetailsPanelState.currentNode = {parameters: {request: {settings: {timeout: 30}}}};

            const {result} = renderObjectProperty({
                path: 'request.settings',
                property: asProperty({name: 'settings', type: 'OBJECT'}),
            });

            expect(getSubPropertySummaries(result.current.subProperties)).toEqual([
                {controlType: 'INTEGER', name: 'timeout', type: 'INTEGER'},
            ]);
        });

        it('reads the array item of a container object', () => {
            hoisted.nodeDetailsPanelState.currentNode = {parameters: {items: [{sku: 'A-1'}]}};

            const {result} = renderObjectProperty({
                path: 'items[0].__item',
                property: asProperty({name: '__item', type: 'OBJECT'}),
            });

            expect(getSubPropertySummaries(result.current.subProperties)).toEqual([
                {controlType: 'TEXT', name: 'sku', type: 'STRING'},
            ]);
        });

        it('prefers the dynamic property types and orders entries by them', () => {
            hoisted.nodeDetailsPanelState.currentNode = {
                metadata: {
                    ui: {
                        dynamicPropertyTypes: {
                            'other.zeta': 'STRING',
                            'settings.alpha': 'INTEGER',
                            'settings.alpha.deep': 'STRING',
                            'settings.beta': 'INTEGER',
                            'settings.gamma': 'STRING',
                            'settings.list[0]': 'STRING',
                        },
                    },
                },
                parameters: {
                    settings: Object.fromEntries([
                        ['omega', 'last'],
                        ['beta', '${trigger_1.count}'],
                        ['alpha', 2],
                    ]),
                },
            };

            const {result} = renderObjectProperty({property: asProperty({name: 'settings', type: 'OBJECT'})});

            expect(getSubPropertySummaries(result.current.subProperties)).toEqual([
                {controlType: 'INTEGER', name: 'alpha', type: 'INTEGER'},
                {controlType: 'INTEGER', name: 'beta', type: 'INTEGER'},
                {controlType: 'TEXT', name: 'omega', type: 'STRING'},
            ]);
        });

        it('rebuilds the entries when the parameters change', () => {
            hoisted.nodeDetailsPanelState.currentNode = {parameters: {settings: {first: 1}}};

            const settingsProperty = asProperty({name: 'settings', type: 'OBJECT'});

            const {rerender, result} = renderObjectProperty({property: settingsProperty});

            expect(result.current.existingSubPropertyNames).toEqual(['first']);

            hoisted.nodeDetailsPanelState.currentNode = {parameters: {settings: {first: 1, second: 'two'}}};

            rerender({property: settingsProperty});

            expect(result.current.existingSubPropertyNames).toEqual(['first', 'second']);
        });
    });

    describe('sub-properties of an object with predefined properties', () => {
        const predefinedProperties = [
            {controlType: 'TEXT', label: 'Street', name: 'street', type: 'STRING'},
            {name: 'zip', type: 'INTEGER'},
            {controlType: 'TEXT', displayCondition: 'kind == "a"', name: 'value', type: 'STRING'},
            {controlType: 'INTEGER', displayCondition: 'kind == "b"', name: 'value', type: 'INTEGER'},
        ];

        const addressProperty = asProperty({name: 'address', properties: predefinedProperties, type: 'OBJECT'});

        it('starts from the predefined properties', () => {
            hoisted.nodeDetailsPanelState.currentNode = {parameters: {address: {}}};

            const {result} = renderObjectProperty({property: addressProperty});

            expect(result.current.subProperties).toEqual(predefinedProperties);
            expect(result.current.existingSubPropertyNames).toEqual(['street', 'zip', 'value', 'value']);
        });

        it('merges stored values into the definitions and appends unknown keys as custom entries', () => {
            hoisted.nodeDetailsPanelState.currentNode = {parameters: {address: {}}};

            const {rerender, result} = renderObjectProperty({property: addressProperty});

            hoisted.nodeDetailsPanelState.currentNode = {
                parameters: {address: {extra: true, street: 'Main', value: 'v', zip: 10000}},
            };

            rerender({property: addressProperty});

            const subProperties = result.current.subProperties!;

            expect(getSubPropertySummaries(subProperties)).toEqual([
                {controlType: 'TEXT', name: 'street', type: 'STRING'},
                {controlType: 'INTEGER', name: 'zip', type: 'INTEGER'},
                {controlType: 'TEXT', name: 'value', type: 'STRING'},
                {controlType: 'INTEGER', name: 'value', type: 'INTEGER'},
                {controlType: 'SELECT', name: 'extra', type: 'BOOLEAN'},
            ]);

            expect(subProperties[0]).toEqual(
                expect.objectContaining({defaultValue: 'Main', expressionEnabled: false, label: 'Street'})
            );
            expect(subProperties[1]).toEqual(expect.objectContaining({defaultValue: 10000, expressionEnabled: true}));
            expect(subProperties[2]).toEqual(
                expect.objectContaining({defaultValue: 'v', displayCondition: 'kind == "a"'})
            );
            expect(subProperties[3]).toEqual(
                expect.objectContaining({defaultValue: 'v', displayCondition: 'kind == "b"'})
            );
            expect(subProperties[4]).toEqual(expect.objectContaining({custom: true, defaultValue: true}));
        });
    });

    describe('handleAddItemClick', () => {
        const settingsProperty = asProperty({
            additionalProperties: [{type: 'NUMBER'}, {type: 'STRING'}],
            name: 'settings',
            type: 'OBJECT',
        });

        beforeEach(() => {
            hoisted.nodeDetailsPanelState.currentNode = {parameters: {settings: {}}};
        });

        it('ignores an empty name', () => {
            const {result} = renderObjectProperty({property: settingsProperty});

            act(() => {
                result.current.handleAddItemClick({name: '', type: 'STRING'});
            });

            expect(result.current.subProperties).toBeUndefined();
            expect(hoisted.mockSaveProperty).not.toHaveBeenCalled();
        });

        it('adds an encoded custom entry and saves it in the workflow node', () => {
            const {result} = renderObjectProperty({property: settingsProperty});

            act(() => {
                result.current.handleAddItemClick({name: 'max retries', type: 'INTEGER'});
            });

            const encodedName = encodePath('max retries');

            expect(result.current.subProperties).toEqual([
                {
                    additionalProperties: [{type: 'NUMBER'}, {type: 'STRING'}],
                    controlType: 'INTEGER',
                    custom: true,
                    expressionEnabled: true,
                    label: 'max retries',
                    name: encodedName,
                    type: 'INTEGER',
                },
            ]);
            expect(hoisted.mockSaveProperty).toHaveBeenCalledTimes(1);
            expect(hoisted.mockSaveProperty).toHaveBeenCalledWith({
                includeInMetadata: true,
                path: `settings.${encodedName}`,
                type: 'INTEGER',
                updateClusterElementParameterMutation: undefined,
                updateWorkflowNodeParameterMutation: workflowNodeParameterMutation,
                workflowId: 'workflow-1',
            });
        });

        it('falls back to the first additional property type', () => {
            const {result} = renderObjectProperty({property: settingsProperty});

            act(() => {
                result.current.handleAddItemClick({name: 'rate', type: ''});
            });

            expect(getSubPropertySummaries(result.current.subProperties)).toEqual([
                {controlType: 'NUMBER', name: 'rate', type: 'NUMBER'},
            ]);
        });

        it('falls back to STRING without additional properties', () => {
            const {result} = renderObjectProperty({property: asProperty({name: 'settings', type: 'OBJECT'})});

            act(() => {
                result.current.handleAddItemClick({name: 'note', type: ''});
            });

            expect(getSubPropertySummaries(result.current.subProperties)).toEqual([
                {controlType: 'TEXT', name: 'note', type: 'STRING'},
            ]);
        });

        it('rejects a name that encodes to an existing entry', () => {
            const {result} = renderObjectProperty({property: settingsProperty});

            act(() => {
                result.current.handleAddItemClick({name: 'max retries', type: 'INTEGER'});
            });

            act(() => {
                result.current.handleAddItemClick({name: 'max retries', type: 'STRING'});
            });

            expect(result.current.subProperties).toHaveLength(1);
            expect(hoisted.mockSaveProperty).toHaveBeenCalledTimes(1);
        });

        it('appends after existing entries', () => {
            hoisted.nodeDetailsPanelState.currentNode = {parameters: {settings: {first: 'a'}}};

            const {result} = renderObjectProperty({property: settingsProperty});

            act(() => {
                result.current.handleAddItemClick({name: 'second', type: 'STRING'});
            });

            expect(result.current.existingSubPropertyNames).toEqual(['first', 'second']);
        });

        it('saves through the cluster element mutation when that is the only one available', () => {
            hoisted.editorState.updateWorkflowNodeParameterMutation = undefined;
            hoisted.editorState.updateClusterElementParameterMutation = clusterElementParameterMutation;

            const {result} = renderObjectProperty({property: settingsProperty});

            act(() => {
                result.current.handleAddItemClick({name: 'note', type: 'STRING'});
            });

            expect(hoisted.mockSaveProperty).toHaveBeenCalledWith(
                expect.objectContaining({
                    updateClusterElementParameterMutation: clusterElementParameterMutation,
                    updateWorkflowNodeParameterMutation: undefined,
                })
            );
        });

        it('only updates local state without any mutation', () => {
            hoisted.editorState.updateWorkflowNodeParameterMutation = undefined;

            const {result} = renderObjectProperty({property: settingsProperty});

            act(() => {
                result.current.handleAddItemClick({name: 'note', type: 'STRING'});
            });

            expect(result.current.subProperties).toHaveLength(1);
            expect(hoisted.mockSaveProperty).not.toHaveBeenCalled();
        });
    });

    describe('handleDeleteClick', () => {
        it('removes the entry and reports its full path', () => {
            hoisted.nodeDetailsPanelState.currentNode = {parameters: {settings: {first: 'a', second: 'b'}}};

            const onDeleteClick = vi.fn();

            const {result} = renderObjectProperty({
                onDeleteClick,
                property: asProperty({name: 'settings', type: 'OBJECT'}),
            });

            act(() => {
                result.current.handleDeleteClick(result.current.subProperties![0] as never);
            });

            expect(result.current.existingSubPropertyNames).toEqual(['second']);
            expect(onDeleteClick).toHaveBeenCalledWith('settings.first');
        });

        it('only removes the entry with the same display condition', () => {
            hoisted.nodeDetailsPanelState.currentNode = {parameters: {payload: {}}};

            const conditionalProperties = [
                {displayCondition: 'kind == "a"', name: 'value', type: 'STRING'},
                {displayCondition: 'kind == "b"', name: 'value', type: 'INTEGER'},
            ];

            const {result} = renderObjectProperty({
                property: asProperty({name: 'payload', properties: conditionalProperties, type: 'OBJECT'}),
            });

            act(() => {
                result.current.handleDeleteClick(conditionalProperties[1] as never);
            });

            expect(result.current.subProperties).toEqual([conditionalProperties[0]]);
        });

        it('still removes the entry without an onDeleteClick handler', () => {
            hoisted.nodeDetailsPanelState.currentNode = {parameters: {settings: {first: 'a'}}};

            const {result} = renderObjectProperty({property: asProperty({name: 'settings', type: 'OBJECT'})});

            act(() => {
                result.current.handleDeleteClick(result.current.subProperties![0] as never);
            });

            expect(result.current.subProperties).toEqual([]);
        });

        it('does nothing without a path', () => {
            const namelessProperties = [{name: 'first', type: 'STRING'}];

            const onDeleteClick = vi.fn();

            const {result} = renderObjectProperty({
                onDeleteClick,
                property: asProperty({properties: namelessProperties, type: 'OBJECT'}),
            });

            act(() => {
                result.current.handleDeleteClick(namelessProperties[0] as never);
            });

            expect(result.current.subProperties).toEqual(namelessProperties);
            expect(onDeleteClick).not.toHaveBeenCalled();
        });
    });

    describe('saving default values', () => {
        const configProperties = [
            {defaultValue: 'localhost', name: 'host', type: 'STRING'},
            {
                name: 'auth',
                properties: [
                    {defaultValue: 'admin', name: 'user', type: 'STRING'},
                    {defaultValue: 22, name: 'port', type: 'INTEGER'},
                ],
                type: 'OBJECT',
            },
            {defaultValue: 'ignored', type: 'STRING'},
        ];

        const configProperty = asProperty({name: 'config', properties: configProperties, type: 'OBJECT'});

        it('saves the nested default object after the debounce delay', () => {
            hoisted.nodeDetailsPanelState.currentNode = {parameters: {}};

            renderObjectProperty({property: configProperty});

            act(() => {
                vi.advanceTimersByTime(599);
            });

            expect(hoisted.mockSaveProperty).not.toHaveBeenCalled();

            act(() => {
                vi.advanceTimersByTime(1);
            });

            expect(hoisted.mockSaveProperty).toHaveBeenCalledTimes(1);
            expect(hoisted.mockSaveProperty).toHaveBeenCalledWith({
                path: 'config',
                type: 'OBJECT',
                updateClusterElementParameterMutation: undefined,
                updateWorkflowNodeParameterMutation: workflowNodeParameterMutation,
                value: {auth: {port: 22, user: 'admin'}, host: 'localhost'},
                workflowId: 'workflow-1',
            });
        });

        it('waits for the parameters before saving a nested object', () => {
            hoisted.nodeDetailsPanelState.currentNode = {parameters: {}};

            renderObjectProperty({path: 'request.config', property: configProperty});

            act(() => {
                vi.advanceTimersByTime(700);
            });

            expect(hoisted.mockSaveProperty).not.toHaveBeenCalled();
        });

        it('does not save without a current node', () => {
            renderObjectProperty({property: configProperty});

            act(() => {
                vi.advanceTimersByTime(700);
            });

            expect(hoisted.mockSaveProperty).not.toHaveBeenCalled();
        });

        it('does not save without a workflow id', () => {
            hoisted.nodeDetailsPanelState.currentNode = {parameters: {}};
            hoisted.workflowState.workflow = {};

            renderObjectProperty({property: configProperty});

            act(() => {
                vi.advanceTimersByTime(700);
            });

            expect(hoisted.mockSaveProperty).not.toHaveBeenCalled();
        });

        it('does not save without a parameter mutation', () => {
            hoisted.nodeDetailsPanelState.currentNode = {parameters: {}};
            hoisted.editorState.updateWorkflowNodeParameterMutation = undefined;

            renderObjectProperty({property: configProperty});

            act(() => {
                vi.advanceTimersByTime(700);
            });

            expect(hoisted.mockSaveProperty).not.toHaveBeenCalled();
        });

        it('cancels the pending save on unmount', () => {
            hoisted.nodeDetailsPanelState.currentNode = {parameters: {}};

            const {unmount} = renderObjectProperty({property: configProperty});

            act(() => {
                vi.advanceTimersByTime(300);
            });

            unmount();

            act(() => {
                vi.advanceTimersByTime(700);
            });

            expect(hoisted.mockSaveProperty).not.toHaveBeenCalled();
        });
    });
});
