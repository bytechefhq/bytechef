vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {PropertyAllType} from '@/shared/types';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {type Mock, afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {useProperty} from '../useProperty';

const wrapper = ({children}: {children: ReactNode}) => (
    <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>{children}</WorkflowEditorProvider>
);

const countProperty = {
    controlType: 'INTEGER',
    expressionEnabled: true,
    name: 'count',
    type: 'INTEGER',
} as PropertyAllType;

const renderCount = (parameterValue?: unknown) =>
    renderHook(() => useProperty({parameterValue, path: 'parameters.count', property: countProperty}), {wrapper});

describe('uncontrolled Text/Formula', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({
            workflow: {id: 'wf-formula-test', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'httpClient_1', parameters: {}, workflowNodeName: 'httpClient_1'},
            workflowNodeDetailsPanelOpen: true,
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);

        (saveProperty as unknown as Mock).mockReset();
    });

    it('renders a constant natively', () => {
        const {result} = renderCount(5);

        expect(result.current.inputMode).toMatchObject({mode: 'text', renderer: 'native'});
        expect(result.current.mentionInput).toBe(false);
    });

    it('renders a lone pill in the one-pill editor', () => {
        const {result} = renderCount('${trigger_1.count}');

        expect(result.current.inputMode).toMatchObject({renderer: 'mentions', singlePill: true});
    });

    it('converts a constant into a formula and saves it', () => {
        const {result} = renderCount(5);

        act(() => result.current.handleFormulaSwitch());

        expect(result.current.isFormulaMode).toBe(true);
        expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({includeInMetadata: true, value: '=5'}));
    });

    it('converts a formula literal back into a constant', () => {
        const {result} = renderCount('=7');

        act(() => result.current.handleFormulaSwitch());

        expect(result.current.isFormulaMode).toBe(false);
        expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: 7}));
    });

    it('shows 0 in the native input when a formula edited to 0 converts back to text', () => {
        const {result} = renderCount(5);

        act(() => result.current.handleFormulaSwitch());
        act(() => result.current.handleMentionInputValueChange('0'));
        act(() => result.current.handleFormulaSwitch());

        expect(result.current.isFormulaMode).toBe(false);
        expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: 0}));
        expect(String(result.current.inputValue)).toBe('0');
    });

    it('clears a formula that cannot convert back', () => {
        const {result} = renderCount('=concat(a, b)');

        act(() => result.current.handleFormulaSwitch());

        expect(result.current.isFormulaMode).toBe(false);
        expect(result.current.propertyParameterValue).toBe('');
        expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: null}));
    });

    it('replaces a constant with a pill and saves the pill', () => {
        const {result} = renderCount(5);

        act(() => result.current.insertPillValue('trigger_1.count'));

        expect(result.current.propertyParameterValue).toBe('${trigger_1.count}');
        expect(result.current.inputMode.singlePill).toBe(true);
        expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: '${trigger_1.count}'}));
    });

    it('goes back to the native control when the pill is deleted', () => {
        const {result} = renderCount('${trigger_1.count}');

        act(() => result.current.handleMentionInputValueChange(''));

        expect(result.current.inputMode.renderer).toBe('native');
        expect(result.current.propertyParameterValue).toBe('');
    });

    it('$ on an empty number field opens pill entry and requests editor focus with $', () => {
        const {result} = renderCount('');

        const preventDefault = vi.fn();

        act(() => result.current.handleNativeKeyDown({currentTarget: {value: ''}, key: '$', preventDefault} as never));

        expect(preventDefault).toHaveBeenCalled();
        expect(result.current.inputMode).toMatchObject({renderer: 'mentions', singlePill: true});
        expect(result.current.editorFocusRequest?.initialInput).toBe('$');
    });

    it('= on an empty number field enters formula mode', () => {
        const {result} = renderCount('');

        act(() =>
            result.current.handleNativeKeyDown({currentTarget: {value: ''}, key: '=', preventDefault: vi.fn()} as never)
        );

        expect(result.current.isFormulaMode).toBe(true);
        expect(result.current.editorFocusRequest).toBeDefined();
    });

    it('ignores $ and = when the field already holds a constant', () => {
        const {result} = renderCount(5);

        const preventDefault = vi.fn();

        act(() => result.current.handleNativeKeyDown({currentTarget: {value: '5'}, key: '$', preventDefault} as never));

        expect(preventDefault).not.toHaveBeenCalled();
        expect(result.current.inputMode.renderer).toBe('native');
    });

    it('$ right after the saved constant is deleted opens pill entry before the save echoes back', () => {
        const {result} = renderCount(5);

        const preventDefault = vi.fn();

        act(() => result.current.handleNativeKeyDown({currentTarget: {value: ''}, key: '$', preventDefault} as never));

        expect(result.current.propertyParameterValue).toBe(5);
        expect(preventDefault).toHaveBeenCalled();
        expect(result.current.inputMode).toMatchObject({renderer: 'mentions', singlePill: true});
        expect(result.current.editorFocusRequest?.initialInput).toBe('$');
    });

    it('abandoning pill entry returns to the native control without saving', () => {
        const {result} = renderCount('');

        act(() =>
            result.current.handleNativeKeyDown({currentTarget: {value: ''}, key: '$', preventDefault: vi.fn()} as never)
        );
        act(() => result.current.handleSinglePillAbandoned());

        expect(result.current.inputMode.renderer).toBe('native');
        expect(saveProperty).not.toHaveBeenCalled();
    });

    describe('$ on a select', () => {
        const methodProperty = {
            controlType: 'SELECT',
            expressionEnabled: true,
            name: 'method',
            options: [
                {label: 'GET', value: 'GET'},
                {label: 'POST', value: 'POST'},
            ],
            type: 'STRING',
        } as PropertyAllType;

        const enabledProperty = {
            controlType: 'SELECT',
            expressionEnabled: true,
            name: 'enabled',
            type: 'BOOLEAN',
        } as PropertyAllType;

        const renderSelect = (property: PropertyAllType, parameterValue: unknown) =>
            renderHook(() => useProperty({parameterValue, path: `parameters.${property.name}`, property}), {wrapper});

        const pressDollar = (result: ReturnType<typeof renderSelect>['result']) => {
            const preventDefault = vi.fn();

            act(() => result.current.handleSelectKeyDown({key: '$', preventDefault} as never));

            return preventDefault;
        };

        describe('of BOOLEAN type', () => {
            it('opens an empty one-pill editor with $ typed although an option is selected', () => {
                const {result} = renderSelect(enabledProperty, true);

                expect(result.current.inputMode.renderer).toBe('native');

                const preventDefault = pressDollar(result);

                expect(preventDefault).toHaveBeenCalled();
                expect(result.current.inputMode).toMatchObject({renderer: 'mentions', singlePill: true});
                expect(result.current.mentionInputValue).toBe('');
                expect(result.current.editorFocusRequest?.initialInput).toBe('$');
            });

            it('replaces the option with the chosen pill', () => {
                const {result} = renderSelect(enabledProperty, true);

                pressDollar(result);

                act(() => result.current.handleMentionInputValueChange('${trigger_1.enabled}'));

                expect(result.current.propertyParameterValue).toBe('${trigger_1.enabled}');
                expect(result.current.inputMode).toMatchObject({renderer: 'mentions', singlePill: true});
            });

            it('keeps the editor and the option when pill entry loses focus without a pill', () => {
                const {result} = renderSelect(enabledProperty, true);

                pressDollar(result);

                act(() => result.current.handleSinglePillAbandoned());

                expect(result.current.inputMode.renderer).toBe('mentions');
                expect(result.current.propertyParameterValue).toBe(true);
                expect(result.current.selectValue).toBe('true');
                expect(saveProperty).not.toHaveBeenCalled();
            });

            it('keeps the editor and the option, and drops the pending save, when the $ is erased', () => {
                const {result} = renderSelect(enabledProperty, true);

                pressDollar(result);

                const cancelPendingSave = vi.fn();

                result.current.editorPendingSaveCancelRef.current = cancelPendingSave;

                act(() => result.current.handleMentionInputValueChange(''));

                expect(cancelPendingSave).toHaveBeenCalled();
                expect(result.current.inputMode.renderer).toBe('mentions');
                expect(result.current.propertyParameterValue).toBe(true);
                expect(result.current.selectValue).toBe('true');
            });

            it('clears the field but keeps the editor when the chosen pill is deleted afterwards', () => {
                const {result} = renderSelect(enabledProperty, true);

                pressDollar(result);

                act(() => result.current.handleMentionInputValueChange('${trigger_1.enabled}'));
                act(() => result.current.handleMentionInputValueChange(''));

                expect(result.current.inputMode.renderer).toBe('mentions');
                expect(result.current.propertyParameterValue).toBe('');
            });
        });

        describe('of STRING type', () => {
            it('opens an empty editor for text and pills with $ typed although an option is selected', () => {
                const {result} = renderSelect(methodProperty, 'GET');

                expect(result.current.inputMode.renderer).toBe('native');

                const preventDefault = pressDollar(result);

                expect(preventDefault).toHaveBeenCalled();
                expect(result.current.inputMode).toMatchObject({renderer: 'mentions', singlePill: false});
                expect(result.current.mentionInputValue).toBe('');
                expect(result.current.editorFocusRequest?.initialInput).toBe('$');
            });

            it('renders a saved value with text and pills in the editor without the one-pill limit', () => {
                const {result} = renderSelect(methodProperty, 'Hi ${trigger_1.name} and ${trigger_1.email}');

                expect(result.current.inputMode).toMatchObject({
                    legacyMixed: false,
                    renderer: 'mentions',
                    singlePill: false,
                });
            });

            it('renders a saved lone pill in the editor without the one-pill limit', () => {
                const {result} = renderSelect(methodProperty, '${trigger_1.method}');

                expect(result.current.inputMode).toMatchObject({renderer: 'mentions', singlePill: false});
            });

            it('saves only values holding a pill or a formula from the editor', () => {
                const {result} = renderSelect(methodProperty, 'GET');

                expect(result.current.validateMentionInputValue('PATCH')).toBe(false);
                expect(result.current.validateMentionInputValue('Hi ${trigger_1.name}')).toBe(true);
                expect(result.current.validateMentionInputValue("='GET'")).toBe(true);
                expect(result.current.validatePropertyValue('GET')).toBe(true);
            });

            it('keeps the editor and the option when pill entry loses focus without a pill', () => {
                const {result} = renderSelect(methodProperty, 'GET');

                pressDollar(result);

                act(() => result.current.handleMentionInputValueChange('$GE'));
                act(() => result.current.handleSinglePillAbandoned());

                expect(result.current.inputMode.renderer).toBe('mentions');
                expect(result.current.propertyParameterValue).toBe('GET');
                expect(result.current.selectValue).toBe('GET');
                expect(saveProperty).not.toHaveBeenCalled();
            });

            it('stays in the editor when the $ is erased, without saving the empty value', () => {
                const {result} = renderSelect(methodProperty, 'GET');

                pressDollar(result);

                const cancelPendingSave = vi.fn();

                result.current.editorPendingSaveCancelRef.current = cancelPendingSave;

                act(() => result.current.handleMentionInputValueChange(''));

                expect(cancelPendingSave).toHaveBeenCalled();
                expect(result.current.inputMode.renderer).toBe('mentions');

                act(() => result.current.handleSinglePillAbandoned());

                expect(result.current.inputMode.renderer).toBe('mentions');

                act(() => result.current.handleDynamicSwitch());

                expect(result.current.inputMode.renderer).toBe('native');
                expect(result.current.selectValue).toBe('GET');
                expect(saveProperty).not.toHaveBeenCalled();
            });

            it('stays in the editor while every pill is deleted, and clears the field when it loses focus without one', () => {
                const {result} = renderSelect(methodProperty, 'GET');

                pressDollar(result);

                act(() => result.current.handleMentionInputValueChange('Hi ${trigger_1.name}'));
                act(() => result.current.handleMentionInputValueChange('Hi '));

                expect(result.current.inputMode.renderer).toBe('mentions');

                act(() => result.current.handleSinglePillAbandoned());

                expect(result.current.inputMode.renderer).toBe('mentions');
                expect(result.current.propertyParameterValue).toBe('');
                expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: null}));

                act(() => result.current.handleDynamicSwitch());

                expect(result.current.inputMode.renderer).toBe('native');
            });
        });

        describe('the Dynamic switch', () => {
            it.each([
                ['a BOOLEAN select', enabledProperty, true],
                ['a STRING select', methodProperty, 'GET'],
            ])('on %s opens an empty editor', (_, property, parameterValue) => {
                const {result} = renderSelect(property, parameterValue);

                act(() => result.current.handleDynamicSwitch());

                expect(result.current.inputMode.renderer).toBe('mentions');
                expect(result.current.mentionInputValue).toBe('');
                expect(result.current.editorFocusRequest).toBeDefined();
                expect(result.current.editorFocusRequest?.initialInput).toBeUndefined();
            });

            it('turned off before a pill is chosen keeps the option without saving', () => {
                const {result} = renderSelect(methodProperty, 'GET');

                act(() => result.current.handleDynamicSwitch());

                const cancelPendingSave = vi.fn();

                result.current.editorPendingSaveCancelRef.current = cancelPendingSave;

                act(() => result.current.handleDynamicSwitch());

                expect(cancelPendingSave).toHaveBeenCalled();
                expect(result.current.inputMode.renderer).toBe('native');
                expect(result.current.selectValue).toBe('GET');
                expect(saveProperty).not.toHaveBeenCalled();
            });

            it('turned off over a pill clears the field and saves the clear', () => {
                const {result} = renderSelect(enabledProperty, '${trigger_1.enabled}');

                expect(result.current.inputMode.renderer).toBe('mentions');

                act(() => result.current.handleDynamicSwitch());

                expect(result.current.inputMode.renderer).toBe('native');
                expect(result.current.propertyParameterValue).toBe('');
                expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: null}));
            });

            it('on an empty builder opens the one-pill editor, and turned off saves nothing', () => {
                const {result} = renderSelect(
                    {
                        controlType: 'ARRAY_BUILDER',
                        expressionEnabled: true,
                        name: 'tags',
                        type: 'ARRAY',
                    } as PropertyAllType,
                    undefined
                );

                expect(result.current.inputMode.renderer).toBe('native');

                act(() => result.current.handleDynamicSwitch());

                expect(result.current.inputMode).toMatchObject({renderer: 'mentions', singlePill: true});
                expect(result.current.editorFocusRequest?.initialInput).toBeUndefined();

                act(() => result.current.handleSinglePillAbandoned());

                expect(result.current.inputMode.renderer).toBe('mentions');

                act(() => result.current.handleDynamicSwitch());

                expect(result.current.inputMode.renderer).toBe('native');
                expect(saveProperty).not.toHaveBeenCalled();
            });
        });

        it('ignores other keys', () => {
            const {result} = renderSelect(methodProperty, 'GET');

            const preventDefault = vi.fn();

            act(() => result.current.handleSelectKeyDown({key: 'G', preventDefault} as never));

            expect(preventDefault).not.toHaveBeenCalled();
            expect(result.current.inputMode.renderer).toBe('native');
        });

        it('ignores $ when expressions are disabled', () => {
            const {result} = renderSelect({...methodProperty, expressionEnabled: false} as PropertyAllType, 'GET');

            const preventDefault = pressDollar(result);

            expect(preventDefault).not.toHaveBeenCalled();
            expect(result.current.inputMode.renderer).toBe('native');
        });
    });

    it('shows the Formula switch on a STRING property', () => {
        const {result} = renderHook(
            () =>
                useProperty({
                    path: 'parameters.uri',
                    property: {
                        controlType: 'TEXT',
                        expressionEnabled: true,
                        name: 'uri',
                        type: 'STRING',
                    } as PropertyAllType,
                }),
            {wrapper}
        );

        expect(result.current.showFormulaSwitch).toBe(true);
    });

    it('never treats a fromAi value as formula', () => {
        const {result} = renderCount("=fromAi('count', 'INTEGER', {'required': false})");

        expect(result.current.isFormulaMode).toBe(false);
        expect(result.current.showFormulaSwitch).toBe(false);
    });

    describe('a saved formula on a root property', () => {
        const renderRootCount = () =>
            renderHook(() => useProperty({property: countProperty}), {
                wrapper,
            });

        beforeEach(() => {
            useWorkflowNodeDetailsPanelStore.setState({
                currentNode: {name: 'math_1', parameters: {count: '=1 + 1'}, workflowNodeName: 'math_1'},
            } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);

            useWorkflowDataStore.setState({
                workflow: {
                    definition: JSON.stringify({tasks: [{name: 'math_1', parameters: {count: '=1 + 1'}}]}),
                    id: 'wf-formula-test',
                    nodeNames: [],
                    tasks: [{name: 'math_1', parameters: {count: '=1 + 1'}}],
                },
            } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);
        });

        it('stays in Formula after the formula is emptied and the save echoes back', () => {
            const {result} = renderRootCount();

            expect(result.current.propertyParameterValue).toBe('=1 + 1');
            expect(result.current.isFormulaMode).toBe(true);

            act(() => result.current.handleMentionInputValueChange(''));

            act(() => {
                useWorkflowDataStore.setState({
                    workflow: {
                        definition: JSON.stringify({tasks: [{name: 'math_1', parameters: {count: null}}]}),
                        id: 'wf-formula-test',
                        nodeNames: [],
                        tasks: [{name: 'math_1', parameters: {count: null}}],
                    },
                } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);
            });

            expect(result.current.propertyParameterValue).toBeNull();
            expect(result.current.isFormulaMode).toBe(true);
            expect(result.current.mentionInput).toBe(true);
        });

        it('leaves Formula on Backspace-exit although the stale formula is still the value', () => {
            const {result} = renderRootCount();

            act(() => result.current.handleMentionInputValueChange(''));

            expect(result.current.propertyParameterValue).toBe('=1 + 1');

            act(() => result.current.setIsFormulaMode(false));

            expect(result.current.isFormulaMode).toBe(false);
            expect(result.current.propertyParameterValue).toBe('');
            expect(result.current.inputMode.renderer).toBe('native');
        });
    });

    describe('a reused field whose path changes', () => {
        beforeEach(() => {
            useWorkflowNodeDetailsPanelStore.setState({
                currentNode: {
                    name: 'math_1',
                    parameters: {first: '=1 + 1', second: 5},
                    workflowNodeName: 'math_1',
                },
            } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
        });

        it('leaves Formula when the new path holds a plain constant', () => {
            const {rerender, result} = renderHook(
                ({path}: {path: string}) => useProperty({path, property: countProperty}),
                {initialProps: {path: 'first'}, wrapper}
            );

            expect(result.current.propertyParameterValue).toBe('=1 + 1');
            expect(result.current.isFormulaMode).toBe(true);

            rerender({path: 'second'});

            expect(result.current.propertyParameterValue).toBe(5);
            expect(result.current.isFormulaMode).toBe(false);
            expect(result.current.inputMode.renderer).toBe('native');
            expect(String(result.current.inputValue)).toBe('5');
        });

        it('leaves Formula when the new path holds no value', () => {
            const {rerender, result} = renderHook(
                ({path}: {path: string}) => useProperty({path, property: countProperty}),
                {initialProps: {path: 'first'}, wrapper}
            );

            expect(result.current.isFormulaMode).toBe(true);

            rerender({path: 'missing'});

            expect(result.current.isFormulaMode).toBe(false);
            expect(result.current.inputMode.renderer).toBe('native');
        });

        it('keeps a FORMULA_MODE control in Formula on a plain value', () => {
            const {rerender, result} = renderHook(
                ({path}: {path: string}) =>
                    useProperty({
                        path,
                        property: {...countProperty, controlType: 'FORMULA_MODE'} as PropertyAllType,
                    }),
                {initialProps: {path: 'first'}, wrapper}
            );

            rerender({path: 'second'});

            expect(result.current.isFormulaMode).toBe(true);
        });
    });

    describe('a switch inside the save debounce', () => {
        afterEach(() => {
            vi.useRealTimers();
        });

        it('keeps the converted value when a typed constant is still waiting to be saved', () => {
            vi.useFakeTimers();

            const {result} = renderCount('');

            act(() => result.current.handleInputChange({target: {value: '7'}} as never));

            act(() => result.current.handleFormulaSwitch());

            act(() => {
                vi.advanceTimersByTime(1000);
            });

            expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: '=7'}));
            expect(saveProperty).toHaveBeenCalledTimes(1);
        });
    });
});
