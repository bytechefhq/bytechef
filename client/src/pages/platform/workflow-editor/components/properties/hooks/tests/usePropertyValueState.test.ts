import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

/**
 * Characterization tests for the value state useProperty owns: propertyParameterValue and the
 * display values derived from it (inputValue, mentionInput/mentionInputValue, selectValue,
 * multiSelectValue) plus the error pair.
 *
 * These drive the real hook rather than a re-implementation, so they stay meaningful when the
 * internals change.
 */

const hoisted = vi.hoisted(() => {
    const mockSaveProperty = vi.fn();

    const panelStoreState = {
        currentNode: {
            componentName: 'mailchimp',
            metadata: {ui: {}},
            name: 'mailchimp_1',
            operationName: 'addMemberToList',
            parameters: {} as Record<string, unknown>,
        } as Record<string, unknown>,
        setFocusedInput: vi.fn(),
        workflowNodeDetailsPanelOpen: true,
    };

    const dataStoreState = {
        workflow: {
            definition: JSON.stringify({tasks: []}),
            id: 'workflow-1',
            tasks: [] as Array<Record<string, unknown>>,
            triggers: [] as Array<Record<string, unknown>>,
        },
    };

    return {dataStoreState, mockSaveProperty, panelStoreState};
});

vi.mock('../../../../utils/saveProperty', () => ({default: hoisted.mockSaveProperty}));

vi.mock('../../../../utils/deleteProperty', () => ({default: vi.fn()}));

vi.mock('../../../../stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: Object.assign(
        (selector: (state: typeof hoisted.panelStoreState) => unknown) => selector(hoisted.panelStoreState),
        {getState: () => hoisted.panelStoreState}
    ),
}));

vi.mock('../../../../stores/useWorkflowDataStore', () => ({
    default: (selector: (state: typeof hoisted.dataStoreState) => unknown) => selector(hoisted.dataStoreState),
}));

vi.mock('../../../../stores/useDataPillPanelStore', () => ({
    default: (selector: (state: {setDataPillPanelOpen: () => void}) => unknown) =>
        selector({setDataPillPanelOpen: vi.fn()}),
}));

vi.mock('../../../../stores/useWorkflowEditorStore', () => ({
    default: (selector: (state: {rootClusterElementNodeData: undefined}) => unknown) =>
        selector({rootClusterElementNodeData: undefined}),
}));

vi.mock('../../../../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        deleteClusterElementParameterMutation: undefined,
        deleteWorkflowNodeParameterMutation: {mutateAsync: vi.fn()},
        updateClusterElementParameterMutation: undefined,
        updateWorkflowNodeParameterMutation: {mutateAsync: vi.fn()},
    }),
}));

/* eslint-disable @typescript-eslint/no-explicit-any */
const renderProperty = async (property: Record<string, unknown>, extraProps: Record<string, unknown> = {}) => {
    const {useProperty} = await import('../useProperty');

    return renderHook(() => useProperty({property: property as any, ...extraProps} as any));
};

describe('useProperty value state', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.panelStoreState.currentNode = {
            componentName: 'mailchimp',
            metadata: {ui: {}},
            name: 'mailchimp_1',
            operationName: 'addMemberToList',
            parameters: {},
        };

        hoisted.dataStoreState.workflow = {
            definition: JSON.stringify({tasks: []}),
            id: 'workflow-1',
            tasks: [],
            triggers: [],
        };
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    describe('initial value resolution', () => {
        // TEXT is mention-capable, so the plain inputValue stays empty and the value would be
        // carried by mentionInputValue instead.
        it('should leave inputValue empty for a mention-capable control', async () => {
            const {result} = await renderProperty({
                controlType: 'TEXT',
                defaultValue: 'seed',
                name: 'field',
                type: 'STRING',
            });

            expect(result.current.inputValue).toBe('');
            expect(result.current.mentionInput).toBe(true);
        });

        it('should seed inputValue from defaultValue when the path is present in the workflow definition', async () => {
            hoisted.dataStoreState.workflow = {
                definition: JSON.stringify({tasks: [{name: 'mailchimp_1'}]}),
                id: 'workflow-1',
                tasks: [{name: 'mailchimp_1', parameters: {field: '7'}}],
                triggers: [],
            };

            const {result} = await renderProperty({
                controlType: 'INTEGER',
                defaultValue: '7',
                name: 'field',
                type: 'INTEGER',
            });

            expect(result.current.inputValue).toBe('7');
        });

        // Current behaviour, pinned deliberately: the workflow-definition sync resolves undefined
        // for a path absent from the definition, and the value-sync effect then blanks the display
        // states — discarding the seeded defaultValue. See the note in the summary.
        it('should blank inputValue when the path is absent from the workflow definition', async () => {
            const {result} = await renderProperty({
                controlType: 'INTEGER',
                defaultValue: '7',
                name: 'field',
                type: 'INTEGER',
            });

            expect(result.current.inputValue).toBe('');
        });

        it('should prefer an explicit parameterValue over defaultValue', async () => {
            const {result} = await renderProperty(
                {controlType: 'TEXT', defaultValue: 'seed', name: 'field', type: 'STRING'},
                {parameterValue: 'explicit'}
            );

            expect(result.current.propertyParameterValue).toBe('explicit');
        });

        it('should start in mention input mode for mention-capable control types', async () => {
            const {result} = await renderProperty({controlType: 'TEXT', name: 'field', type: 'STRING'});

            expect(result.current.mentionInput).toBe(true);
        });

        it('should not start in mention input mode for a select control', async () => {
            const {result} = await renderProperty({controlType: 'SELECT', name: 'field', type: 'STRING'});

            expect(result.current.mentionInput).toBe(false);
        });

        // Current behaviour: on mount the workflow-definition sync resolves undefined for a path
        // that is absent from the definition, and the value-sync effect then blanks the display
        // states, discarding the seeded defaultValue. Pinned as-is; see the note in the summary.
        it('should blank selectValue when the path is absent from the workflow definition', async () => {
            const {result} = await renderProperty({
                controlType: 'SELECT',
                defaultValue: 'chosen',
                name: 'field',
                type: 'STRING',
            });

            expect(result.current.selectValue).toBe('');
        });

        it('should keep selectValue when the path is present in the workflow definition', async () => {
            hoisted.dataStoreState.workflow = {
                definition: JSON.stringify({tasks: [{name: 'mailchimp_1'}]}),
                id: 'workflow-1',
                tasks: [{name: 'mailchimp_1', parameters: {field: 'chosen'}}],
                triggers: [],
            };

            const {result} = await renderProperty({
                controlType: 'SELECT',
                defaultValue: 'chosen',
                name: 'field',
                type: 'STRING',
            });

            expect(result.current.selectValue).toBe('chosen');
        });

        it('should seed selectValue as the string form of a boolean parameterValue', async () => {
            const {result} = await renderProperty(
                {controlType: 'SELECT', name: 'field', type: 'BOOLEAN'},
                {parameterValue: true}
            );

            expect(result.current.selectValue).toBe('true');
        });
    });

    describe('expression values', () => {
        it('should switch to mention input and strip the leading = for an expression parameterValue', async () => {
            const {result} = await renderProperty(
                {controlType: 'INTEGER', name: 'field', type: 'INTEGER'},
                {parameterValue: '=1 + 2'}
            );

            expect(result.current.mentionInput).toBe(true);
            expect(result.current.mentionInputValue).toBe('1 + 2');
        });

        it('should treat a data pill value as mention input', async () => {
            const {result} = await renderProperty(
                {controlType: 'INTEGER', name: 'field', type: 'INTEGER'},
                {parameterValue: '${trigger_1.id}'}
            );

            expect(result.current.mentionInput).toBe(true);
        });
    });

    describe('parameter sync from the workflow definition', () => {
        it('should adopt the saved value for its path when the definition changes', async () => {
            const {rerender, result} = await renderProperty({
                controlType: 'TEXT',
                name: 'field',
                type: 'STRING',
            });

            act(() => {
                hoisted.dataStoreState.workflow = {
                    definition: JSON.stringify({tasks: [{name: 'mailchimp_1'}]}),
                    id: 'workflow-1',
                    tasks: [{name: 'mailchimp_1', parameters: {field: 'from-definition'}}],
                    triggers: [],
                };
            });

            rerender();

            expect(result.current.propertyParameterValue).toBe('from-definition');
        });
    });

    describe('input editing', () => {
        it('should update inputValue and clear the error for a valid change', async () => {
            const {result} = await renderProperty({
                controlType: 'INTEGER',
                maxValue: 100,
                minValue: 0,
                name: 'field',
                type: 'INTEGER',
            });

            act(() => {
                result.current.handleInputChange({
                    target: {value: '42'},
                } as never);
            });

            expect(result.current.inputValue).toBe('42');
            expect(result.current.hasError).toBe(false);
        });

        it('should flag an error when a numeric value exceeds maxValue', async () => {
            const {result} = await renderProperty({
                controlType: 'INTEGER',
                maxValue: 10,
                minValue: 0,
                name: 'field',
                type: 'INTEGER',
            });

            act(() => {
                result.current.handleInputChange({target: {value: '999'}} as never);
            });

            expect(result.current.hasError).toBe(true);
            expect(result.current.errorMessage).toBeTruthy();
        });

        it('should clear inputValue and error state via handleInputClear', async () => {
            const {result} = await renderProperty({controlType: 'TIME', name: 'field', type: 'TIME'});

            act(() => {
                result.current.handleInputChange({target: {value: '10:30'}} as never);
            });

            act(() => {
                result.current.handleInputClear();
            });

            expect(result.current.inputValue).toBe('');
            expect(result.current.hasError).toBe(false);
        });
    });

    describe('mention input editing', () => {
        it('should update mentionInputValue on change', async () => {
            const {result} = await renderProperty({controlType: 'TEXT', name: 'field', type: 'STRING'});

            act(() => {
                result.current.handleMentionInputValueChange('hello');
            });

            expect(result.current.mentionInputValue).toBe('hello');
        });

        it('should not flag an error for an expression value regardless of length rules', async () => {
            const {result} = await renderProperty({
                controlType: 'TEXT',
                minLength: 50,
                name: 'field',
                type: 'STRING',
            });

            act(() => {
                result.current.handleMentionInputValueChange('=short');
            });

            expect(result.current.hasError).toBe(false);
        });
    });

    describe('input type switching', () => {
        it('should toggle mentionInput and reset the value states', async () => {
            const {result} = await renderProperty({
                controlType: 'SELECT',
                expressionEnabled: true,
                name: 'field',
                type: 'STRING',
            });

            const initialMentionInput = result.current.mentionInput;

            act(() => {
                result.current.handleInputTypeSwitchButtonClick();
            });

            expect(result.current.mentionInput).toBe(!initialMentionInput);
            expect(result.current.mentionInputValue).toBe('');
        });
    });

    describe('multi select', () => {
        it('should update multiSelectValue and persist through handleMultiSelectChange', async () => {
            hoisted.panelStoreState.currentNode.parameters = {field: []};

            const {result} = await renderProperty({
                controlType: 'MULTI_SELECT',
                name: 'field',
                type: 'ARRAY',
            });

            act(() => {
                result.current.handleMultiSelectChange(['a', 'b']);
            });

            expect(result.current.multiSelectValue).toEqual(['a', 'b']);
            expect(hoisted.mockSaveProperty).toHaveBeenCalled();
        });
    });

    describe('select changes', () => {
        it('should update selectValue and persist through handleSelectChange', async () => {
            const {result} = await renderProperty({controlType: 'SELECT', name: 'field', type: 'STRING'});

            act(() => {
                result.current.handleSelectChange('picked', 'field');
            });

            expect(result.current.selectValue).toBe('picked');
            expect(hoisted.mockSaveProperty).toHaveBeenCalled();
        });

        it('should ignore a select change that matches the current value', async () => {
            const {result} = await renderProperty(
                {controlType: 'SELECT', name: 'field', type: 'STRING'},
                {parameterValue: 'same'}
            );

            act(() => {
                result.current.handleSelectChange('same', 'field');
            });

            expect(hoisted.mockSaveProperty).not.toHaveBeenCalled();
        });
    });

    describe('operation change reset', () => {
        it('should reset value states when the operation name changes', async () => {
            const {rerender, result} = await renderProperty({
                controlType: 'TEXT',
                defaultValue: 'initial',
                name: 'field',
                type: 'STRING',
            });

            act(() => {
                result.current.handleMentionInputValueChange('user typed');
            });

            expect(result.current.mentionInputValue).toBe('user typed');

            act(() => {
                hoisted.panelStoreState.currentNode = {
                    ...hoisted.panelStoreState.currentNode,
                    operationName: 'differentOperation',
                };
            });

            rerender();

            expect(result.current.mentionInputValue).toBe('initial');
            expect(result.current.propertyParameterValue).toBe('initial');
        });
    });

    // Guards the render amplification this hook is prone to: a single resolved parameter change
    // used to fan out through chained effects, each one costing another render pass.
    describe('render amplification', () => {
        it('should settle a workflow definition change in a small number of render passes', async () => {
            const {useProperty} = await import('../useProperty');

            let renderCount = 0;

            const {rerender} = renderHook(() => {
                renderCount += 1;

                /* eslint-disable @typescript-eslint/no-explicit-any */
                return useProperty({
                    property: {controlType: 'TEXT', name: 'field', type: 'STRING'} as any,
                } as any);
            });

            const rendersAfterMount = renderCount;

            act(() => {
                hoisted.dataStoreState.workflow = {
                    definition: JSON.stringify({tasks: [{name: 'mailchimp_1'}]}),
                    id: 'workflow-1',
                    tasks: [{name: 'mailchimp_1', parameters: {field: 'synced'}}],
                    triggers: [],
                };
            });

            rerender();

            const rendersForOneChange = renderCount - rendersAfterMount;

            console.log('RENDERS_FOR_ONE_CHANGE:', rendersForOneChange, 'MOUNT:', rendersAfterMount);
            expect(rendersAfterMount).toBeLessThanOrEqual(2);
            expect(rendersForOneChange).toBeLessThanOrEqual(3);
        });
    });

    describe('debounced save', () => {
        it('should save the latest input value once after the debounce window', async () => {
            vi.useFakeTimers();

            const {result} = await renderProperty({controlType: 'TEXT', name: 'field', type: 'STRING'});

            act(() => {
                result.current.handleInputChange({target: {value: 'a'}} as never);
            });

            act(() => {
                result.current.handleInputChange({target: {value: 'ab'}} as never);
            });

            await act(async () => {
                vi.advanceTimersByTime(700);
            });

            expect(hoisted.mockSaveProperty).toHaveBeenCalledTimes(1);
            expect(hoisted.mockSaveProperty).toHaveBeenCalledWith(expect.objectContaining({value: 'ab'}));
        });
    });
});
