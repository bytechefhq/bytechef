vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {FormulaEnabledProvider} from '@/pages/platform/workflow-editor/components/properties/FormulaEnabledContext';
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

    it('ignores = on an empty number field when the surface disables formulas', () => {
        const {result} = renderHook(
            () => useProperty({parameterValue: '', path: 'parameters.count', property: countProperty}),
            {
                wrapper: ({children}: {children: ReactNode}) => (
                    <FormulaEnabledProvider value={false}>{wrapper({children})}</FormulaEnabledProvider>
                ),
            }
        );

        const preventDefault = vi.fn();

        act(() => result.current.handleNativeKeyDown({currentTarget: {value: ''}, key: '=', preventDefault} as never));

        expect(preventDefault).not.toHaveBeenCalled();
        expect(result.current.isFormulaMode).toBe(false);
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

    describe('text typed into the editor', () => {
        const renderUri = (parameterValue?: unknown) =>
            renderHook(
                () =>
                    useProperty({
                        parameterValue,
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

        it('quotes the decoded text when Formula turns on', () => {
            const {result} = renderUri();

            act(() => result.current.handleMentionInputValueChange('a &amp; b &lt;c&gt;'));

            act(() => result.current.handleFormulaSwitch());

            expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: "='a & b <c>'"}));
        });

        it('unquotes the decoded formula when Formula turns off', () => {
            const {result} = renderUri();

            act(() => result.current.handleFormulaSwitch());

            act(() => result.current.handleMentionInputValueChange("'a &amp; b'"));

            act(() => result.current.handleFormulaSwitch());

            expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: 'a & b'}));
        });

        it('keeps a saved value with markup as it is when Formula turns on', () => {
            const {result} = renderUri('<b>a &amp; b</b>');

            act(() => result.current.handleFormulaSwitch());

            expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: "='<b>a &amp; b</b>'"}));
        });
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

    describe('Formula off into a control that cannot show the value', () => {
        const renderTyped = (controlType: string, type: string, parameterValue: unknown) =>
            renderHook(
                () =>
                    useProperty({
                        parameterValue,
                        path: 'parameters.field',
                        property: {controlType, expressionEnabled: true, name: 'field', type} as PropertyAllType,
                    }),
                {wrapper}
            );

        it.each([
            ['JSON_SCHEMA_BUILDER', 'STRING', "='hello'"],
            ['JSON_SCHEMA_BUILDER', 'STRING', "='[1, 2]'"],
            ['OBJECT_BUILDER', 'OBJECT', "='abc'"],
            ['ARRAY_BUILDER', 'ARRAY', "='abc'"],
            ['MULTI_SELECT', 'ARRAY', "='abc'"],
            ['DATE', 'DATE', "='hello'"],
            ['DATE_TIME', 'DATE_TIME', "='hello'"],
            ['TIME', 'TIME', "='hello'"],
        ])('keeps Formula on for %s of type %s holding %s', (controlType, type, formula) => {
            const {result} = renderTyped(controlType, type, formula);

            act(() => result.current.handleFormulaSwitch());

            expect(result.current.isFormulaMode).toBe(true);
            expect(result.current.propertyParameterValue).toBe(formula);
            expect(saveProperty).not.toHaveBeenCalled();
        });

        it.each([
            ['JSON_SCHEMA_BUILDER', 'STRING', `='{"type":"object"}'`, '{"type":"object"}'],
            ['DATE', 'DATE', "='2024-01-31'", '2024-01-31'],
            ['DATE_TIME', 'DATE_TIME', "='2024-01-31T10:15'", '2024-01-31T10:15'],
            ['TIME', 'TIME', "='10:15:30'", '10:15:30'],
            ['OBJECT_BUILDER', 'OBJECT', '=${trigger_1.settings}', '${trigger_1.settings}'],
        ])('turns Formula off for %s of type %s holding %s', (controlType, type, formula, converted) => {
            const {result} = renderTyped(controlType, type, formula);

            act(() => result.current.handleFormulaSwitch());

            expect(result.current.isFormulaMode).toBe(false);
            expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: converted}));
        });
    });

    describe('Formula off into a select', () => {
        const methodSelectProperty = {
            controlType: 'SELECT',
            expressionEnabled: true,
            name: 'method',
            options: [
                {label: 'GET', value: 'GET'},
                {label: 'POST', value: 'POST'},
            ],
            type: 'STRING',
        } as PropertyAllType;

        const renderSelectProperty = (property: PropertyAllType, parameterValue: unknown) =>
            renderHook(() => useProperty({parameterValue, path: `parameters.${property.name}`, property}), {wrapper});

        it('keeps Formula on for a value that is not one of the options', () => {
            const {result} = renderSelectProperty(methodSelectProperty, "='PATCH'");

            act(() => result.current.handleFormulaSwitch());

            expect(result.current.isFormulaMode).toBe(true);
            expect(saveProperty).not.toHaveBeenCalled();
        });

        it('keeps Formula on for a number that is not one of the options', () => {
            const {result} = renderSelectProperty(
                {
                    ...methodSelectProperty,
                    name: 'priority',
                    options: [
                        {label: 'One', value: 1},
                        {label: 'Two', value: 2},
                    ],
                    type: 'INTEGER',
                } as PropertyAllType,
                '=3'
            );

            act(() => result.current.handleFormulaSwitch());

            expect(result.current.isFormulaMode).toBe(true);
            expect(saveProperty).not.toHaveBeenCalled();
        });

        it('turns Formula off for one of the options', () => {
            const {result} = renderSelectProperty(methodSelectProperty, "='POST'");

            act(() => result.current.handleFormulaSwitch());

            expect(result.current.isFormulaMode).toBe(false);
            expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: 'POST'}));
        });

        it('keeps Formula on for a value missing from the loaded options of a data source', () => {
            const {result} = renderSelectProperty(
                {...methodSelectProperty, options: undefined, optionsDataSource: {}} as PropertyAllType,
                "='PATCH'"
            );

            act(() => result.current.setLoadedSelectOptionValues(['GET', 'POST']));

            act(() => result.current.handleFormulaSwitch());

            expect(result.current.isFormulaMode).toBe(true);
            expect(saveProperty).not.toHaveBeenCalled();
        });

        it('turns Formula off when the options of a data source are not loaded yet', () => {
            const {result} = renderSelectProperty(
                {...methodSelectProperty, options: undefined, optionsDataSource: {}} as PropertyAllType,
                "='PATCH'"
            );

            act(() => result.current.handleFormulaSwitch());

            expect(result.current.isFormulaMode).toBe(false);
            expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: 'PATCH'}));
        });

        it('turns Formula off for a select that takes values outside its options', () => {
            const {result} = renderSelectProperty(
                {...methodSelectProperty, optionsDataSource: {}, optionsLoadedDynamically: true} as PropertyAllType,
                "='PATCH'"
            );

            act(() => result.current.setLoadedSelectOptionValues(['GET', 'POST']));

            act(() => result.current.handleFormulaSwitch());

            expect(result.current.isFormulaMode).toBe(false);
            expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: 'PATCH'}));
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

        it('opens an empty formula on = after a constant converted from a formula is cleared', () => {
            vi.useFakeTimers();

            const {result} = renderCount('=7');

            act(() => result.current.handleFormulaSwitch());

            act(() => result.current.handleInputChange({target: {value: ''}} as never));

            act(() =>
                result.current.handleNativeKeyDown({
                    currentTarget: {value: ''},
                    key: '=',
                    preventDefault: vi.fn(),
                } as never)
            );

            act(() => {
                vi.advanceTimersByTime(1000);
            });

            expect(result.current.isFormulaMode).toBe(true);
            expect(result.current.mentionInputValue).toBe('');
            expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: null}));
            expect(saveProperty).toHaveBeenCalledTimes(2);
        });

        it('turns a constant cleared but not yet saved into an empty formula', () => {
            vi.useFakeTimers();

            const {result} = renderCount(5);

            act(() => result.current.handleInputChange({target: {value: ''}} as never));

            act(() => result.current.handleFormulaSwitch());

            act(() => {
                vi.advanceTimersByTime(1000);
            });

            expect(result.current.isFormulaMode).toBe(true);
            expect(result.current.propertyParameterValue).toBe('');
            expect(saveProperty).toHaveBeenCalledTimes(1);
            expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: null}));
        });

        const renderEditorProperty = (controlType: string, parameterValue: unknown) =>
            renderHook(
                () =>
                    useProperty({
                        parameterValue,
                        path: 'parameters.body',
                        property: {
                            controlType,
                            expressionEnabled: true,
                            name: 'body',
                            type: 'STRING',
                        } as PropertyAllType,
                    }),
                {wrapper}
            );

        it('drops a schema edit still waiting to be saved when Formula turns on', () => {
            vi.useFakeTimers();

            const {result} = renderEditorProperty('JSON_SCHEMA_BUILDER', '{"type":"object"}');

            act(() => result.current.handleJsonSchemaBuilderChange({type: 'string'} as never));

            act(() => result.current.handleFormulaSwitch());

            act(() => {
                vi.advanceTimersByTime(1000);
            });

            expect(saveProperty).toHaveBeenCalledTimes(1);
            expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: `='{"type":"object"}'`}));
        });

        it('drops a schema edit still waiting to be saved when a pill replaces the value', () => {
            vi.useFakeTimers();

            const {result} = renderEditorProperty('JSON_SCHEMA_BUILDER', '{"type":"object"}');

            act(() => result.current.handleJsonSchemaBuilderChange({type: 'string'} as never));

            act(() => result.current.insertPillValue('trigger_1.schema'));

            act(() => {
                vi.advanceTimersByTime(1000);
            });

            expect(saveProperty).toHaveBeenCalledTimes(1);
            expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: '${trigger_1.schema}'}));
        });

        it('drops a code edit still waiting to be saved when a pill replaces the value', () => {
            vi.useFakeTimers();

            const {result} = renderEditorProperty('CODE_EDITOR', 'return 1;');

            act(() => result.current.handleCodeEditorChange('return 2;'));

            act(() => result.current.insertPillValue('trigger_1.code'));

            act(() => {
                vi.advanceTimersByTime(1000);
            });

            expect(saveProperty).toHaveBeenCalledTimes(1);
            expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: '${trigger_1.code}'}));
        });

        it('drops a typed constant still waiting to be saved when the model takes the field', () => {
            vi.useFakeTimers();

            const {result} = renderCount('');

            act(() => result.current.handleInputChange({target: {value: '7'}} as never));

            act(() => result.current.handleFromAiClick?.(true));

            act(() => {
                vi.advanceTimersByTime(1000);
            });

            expect(saveProperty).toHaveBeenCalledTimes(1);
            expect(saveProperty).toHaveBeenLastCalledWith(
                expect.objectContaining({value: expect.stringMatching(/^=fromAi\('count'/)})
            );
        });
    });
});
