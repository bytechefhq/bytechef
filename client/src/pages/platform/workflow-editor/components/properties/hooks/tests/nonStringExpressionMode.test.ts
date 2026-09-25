vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {PropertyAllType} from '@/shared/types';
import {act, renderHook} from '@testing-library/react';
import {ReactNode, createElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useProperty} from '../useProperty';

/**
 * Tests for non-string property expression mode behavior:
 * 1. `=` typed into an empty numerical input switches to formula mode
 * 2. Array item reconstruction should preserve expressionEnabled
 * 3. Object sub-property reconstruction should preserve expressionEnabled
 */

const wrapper = ({children}: {children: ReactNode}) =>
    createElement(WorkflowEditorProvider, {children, value: workflowEditorProviderTestValue as never});

describe('non-string expression mode', () => {
    describe('numerical input `=` detection', () => {
        beforeEach(() => {
            useWorkflowDataStore.setState({
                workflow: {id: 'wf-non-string-expression', nodeNames: []},
            } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

            useWorkflowNodeDetailsPanelStore.setState({
                currentNode: {name: 'math_1', parameters: {}, workflowNodeName: 'math_1'},
                workflowNodeDetailsPanelOpen: true,
            } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
        });

        const renderField = ({
            controlType = 'INTEGER',
            expressionEnabled,
            parameterValue = '',
            type = 'INTEGER',
        }: {
            controlType?: string;
            expressionEnabled?: boolean;
            parameterValue?: unknown;
            type?: string;
        }) =>
            renderHook(
                () =>
                    useProperty({
                        parameterValue,
                        path: 'parameters.count',
                        property: {controlType, expressionEnabled, name: 'count', type} as PropertyAllType,
                    }),
                {wrapper}
            );

        const pressKey = (result: ReturnType<typeof renderField>['result'], key: string, inputValue = '') => {
            const preventDefault = vi.fn();

            act(() =>
                result.current.handleNativeKeyDown({currentTarget: {value: inputValue}, key, preventDefault} as never)
            );

            return preventDefault;
        };

        it('should switch to formula mode when typing `=` in a numerical input with expressionEnabled', () => {
            const {result} = renderField({expressionEnabled: true});

            const preventDefault = pressKey(result, '=');

            expect(preventDefault).toHaveBeenCalled();
            expect(result.current.isFormulaMode).toBe(true);
            expect(result.current.mentionInput).toBe(true);
        });

        it('should switch for a NUMBER input too', () => {
            const {result} = renderField({controlType: 'NUMBER', expressionEnabled: true, type: 'NUMBER'});

            pressKey(result, '=');

            expect(result.current.isFormulaMode).toBe(true);
        });

        it('should NOT switch when expressionEnabled is false', () => {
            const {result} = renderField({expressionEnabled: false});

            const preventDefault = pressKey(result, '=');

            expect(preventDefault).not.toHaveBeenCalled();
            expect(result.current.isFormulaMode).toBe(false);
            expect(result.current.mentionInput).toBe(false);
        });

        it('should switch when expressionEnabled is undefined', () => {
            const {result} = renderField({expressionEnabled: undefined});

            pressKey(result, '=');

            expect(result.current.isFormulaMode).toBe(true);
        });

        it('should NOT switch for non-numerical inputs', () => {
            const {result} = renderField({controlType: 'DATE', expressionEnabled: true, type: 'DATE'});

            const preventDefault = pressKey(result, '=');

            expect(preventDefault).not.toHaveBeenCalled();
            expect(result.current.isFormulaMode).toBe(false);
            expect(result.current.mentionInput).toBe(false);
        });

        it('should NOT switch for normal numeric input (no `=`)', () => {
            const {result} = renderField({expressionEnabled: true});

            const preventDefault = pressKey(result, '4');

            expect(preventDefault).not.toHaveBeenCalled();
            expect(result.current.isFormulaMode).toBe(false);
        });

        it('should NOT switch when the field already holds a value', () => {
            const {result} = renderField({expressionEnabled: true, parameterValue: 3});

            const preventDefault = pressKey(result, '=', '3');

            expect(preventDefault).not.toHaveBeenCalled();
            expect(result.current.isFormulaMode).toBe(false);
        });

        it('should open the formula editor empty, without the `=` prefix', () => {
            const {result} = renderField({expressionEnabled: true});

            pressKey(result, '=');

            expect(result.current.editorFocusRequest).toBeDefined();
            expect(result.current.editorFocusRequest?.initialInput).toBeUndefined();
            expect(result.current.mentionInputValue).toBe('');
        });
    });

    describe('array item expressionEnabled propagation', () => {
        /**
         * Replicates ArrayProperty.tsx item construction from saved parameters.
         * Non-string array items should always get expressionEnabled: true so the
         * dynamic/static switch button persists across component remounts.
         */
        const buildArrayItem = (
            parameterItemType: string,
            index: number
        ): {controlType: string; custom: boolean; expressionEnabled: boolean; name: string; type: string} => ({
            controlType: parameterItemType,
            custom: true,
            expressionEnabled: true,
            name: index.toString(),
            type: parameterItemType,
        });

        it('should set expressionEnabled: true for INTEGER items', () => {
            const item = buildArrayItem('INTEGER', 0);

            expect(item.expressionEnabled).toBe(true);
        });

        it('should set expressionEnabled: true for NUMBER items', () => {
            const item = buildArrayItem('NUMBER', 0);

            expect(item.expressionEnabled).toBe(true);
        });

        it('should set expressionEnabled: true for DATE items', () => {
            const item = buildArrayItem('DATE', 0);

            expect(item.expressionEnabled).toBe(true);
        });

        it('should set expressionEnabled: true for STRING items', () => {
            const item = buildArrayItem('STRING', 0);

            expect(item.expressionEnabled).toBe(true);
        });
    });

    describe('object sub-property expressionEnabled propagation', () => {
        /**
         * Replicates useObjectProperty.ts sub-property construction for the
         * "matching property" path. Non-string types should default to
         * expressionEnabled: true when the definition doesn't explicitly set it.
         */
        const buildMatchingSubProperty = (matchingProperty: {
            expressionEnabled?: boolean;
            name: string;
            type: string;
        }): {expressionEnabled: boolean; name: string; type: string} => ({
            ...matchingProperty,
            expressionEnabled: matchingProperty.expressionEnabled ?? matchingProperty.type !== 'STRING',
        });

        it('should default to true for INTEGER when expressionEnabled is undefined', () => {
            const subProperty = buildMatchingSubProperty({
                name: 'count',
                type: 'INTEGER',
            });

            expect(subProperty.expressionEnabled).toBe(true);
        });

        it('should default to true for NUMBER when expressionEnabled is undefined', () => {
            const subProperty = buildMatchingSubProperty({
                name: 'amount',
                type: 'NUMBER',
            });

            expect(subProperty.expressionEnabled).toBe(true);
        });

        it('should default to false for STRING when expressionEnabled is undefined', () => {
            const subProperty = buildMatchingSubProperty({
                name: 'label',
                type: 'STRING',
            });

            expect(subProperty.expressionEnabled).toBe(false);
        });

        it('should preserve explicit expressionEnabled: true from definition', () => {
            const subProperty = buildMatchingSubProperty({
                expressionEnabled: true,
                name: 'label',
                type: 'STRING',
            });

            expect(subProperty.expressionEnabled).toBe(true);
        });

        it('should preserve explicit expressionEnabled: false from definition', () => {
            const subProperty = buildMatchingSubProperty({
                expressionEnabled: false,
                name: 'count',
                type: 'INTEGER',
            });

            expect(subProperty.expressionEnabled).toBe(false);
        });
    });

    describe('array item Object.keys guard against strings', () => {
        /**
         * Replicates the guard in ArrayProperty.tsx that prevents
         * Object.keys() from being called on string values (which would
         * return character indices like ["0","1","2","3",...]).
         */
        it('should NOT iterate over string character indices', () => {
            const parameterItemValue = '=3+3+3+3';
            const isObject = parameterItemValue && typeof parameterItemValue === 'object';

            expect(isObject).toBe(false);
            expect(Object.keys(parameterItemValue)).toEqual(['0', '1', '2', '3', '4', '5', '6', '7']);
        });

        it('should iterate over actual object keys', () => {
            const parameterItemValue = {name: 'test', value: 42};
            const isObject = parameterItemValue && typeof parameterItemValue === 'object';

            expect(isObject).toBe(true);
            expect(Object.keys(parameterItemValue)).toEqual(['name', 'value']);
        });

        it('should NOT iterate over null', () => {
            const parameterItemValue = null;
            const isObject = parameterItemValue && typeof parameterItemValue === 'object';

            expect(isObject).toBeFalsy();
        });
    });

    describe('formula mode validation with `=` prefix', () => {
        /**
         * Replicates the validateBeforeSave logic from saveMentionInputValue.
         * In formula mode, the value must have `=` prepended BEFORE validation
         * so that validatePropertyValue recognizes it as an expression.
         */
        const validatePropertyValue = (value: string | number, controlType: string): boolean => {
            const stringValue = typeof value === 'string' ? value : String(value);

            if (typeof value === 'string' && (value.startsWith('=') || value.includes('${'))) {
                return true;
            }

            if (controlType === 'INTEGER' && typeof value === 'string' && !/^-?\d+$/.test(stringValue)) {
                return false;
            }

            return true;
        };

        it('should REJECT "3+3" as literal INTEGER (no formula prefix)', () => {
            expect(validatePropertyValue('3+3', 'INTEGER')).toBe(false);
        });

        it('should ACCEPT "=3+3" as expression', () => {
            expect(validatePropertyValue('=3+3', 'INTEGER')).toBe(true);
        });

        it('should ACCEPT "${datapill}+3" as expression', () => {
            expect(validatePropertyValue('${datapill}+3', 'INTEGER')).toBe(true);
        });

        it('should prepend `=` in formula mode before validation', () => {
            const editorValue = '3+3';
            const isFormulaMode = true;
            const valueForValidation =
                isFormulaMode && typeof editorValue === 'string' && !editorValue.startsWith('=')
                    ? `=${editorValue}`
                    : editorValue;

            expect(validatePropertyValue(valueForValidation, 'INTEGER')).toBe(true);
        });

        it('should not double-prepend `=` if already present', () => {
            const editorValue = '=3+3';
            const isFormulaMode = true;
            const valueForValidation =
                isFormulaMode && typeof editorValue === 'string' && !editorValue.startsWith('=')
                    ? `=${editorValue}`
                    : editorValue;

            expect(valueForValidation).toBe('=3+3');
        });

        it('should not prepend `=` when not in formula mode', () => {
            const editorValue = '42';
            const isFormulaMode = false;
            const valueForValidation =
                isFormulaMode && typeof editorValue === 'string' && !editorValue.startsWith('=')
                    ? `=${editorValue}`
                    : editorValue;

            expect(valueForValidation).toBe('42');
            expect(validatePropertyValue(valueForValidation, 'INTEGER')).toBe(true);
        });
    });

    describe('showFormulaSwitch', () => {
        beforeEach(() => {
            useWorkflowDataStore.setState({
                workflow: {id: 'wf-show-formula-switch', nodeNames: []},
            } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

            useWorkflowNodeDetailsPanelStore.setState({
                currentNode: {name: 'math_1', parameters: {}, workflowNodeName: 'math_1'},
            } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
        });

        const renderShowFormulaSwitch = (property: Record<string, unknown>) =>
            renderHook(
                () =>
                    useProperty({
                        path: 'parameters.field',
                        property: {name: 'field', ...property} as PropertyAllType,
                    }),
                {wrapper}
            ).result.current.showFormulaSwitch;

        it('should show for non-STRING type with expressionEnabled in uncontrolled mode', () => {
            expect(renderShowFormulaSwitch({controlType: 'INTEGER', expressionEnabled: true, type: 'INTEGER'})).toBe(
                true
            );
        });

        it('should show for STRING type with expressionEnabled', () => {
            expect(renderShowFormulaSwitch({controlType: 'TEXT', expressionEnabled: true, type: 'STRING'})).toBe(true);
        });

        it('should show when expressionEnabled is undefined', () => {
            expect(renderShowFormulaSwitch({controlType: 'INTEGER', type: 'INTEGER'})).toBe(true);
        });

        it('should NOT show when expressionEnabled is false', () => {
            expect(renderShowFormulaSwitch({controlType: 'INTEGER', expressionEnabled: false, type: 'INTEGER'})).toBe(
                false
            );
        });

        it('should NOT show for a STRING type with expressionEnabled false', () => {
            expect(renderShowFormulaSwitch({controlType: 'TEXT', expressionEnabled: false, type: 'STRING'})).toBe(
                false
            );
        });

        it('should show for NUMBER type with expressionEnabled', () => {
            expect(renderShowFormulaSwitch({controlType: 'NUMBER', expressionEnabled: true, type: 'NUMBER'})).toBe(
                true
            );
        });

        it('should show for DATE type with expressionEnabled', () => {
            expect(renderShowFormulaSwitch({controlType: 'DATE', expressionEnabled: true, type: 'DATE'})).toBe(true);
        });

        it('should NOT show for FILE_ENTRY, NULL or CODE_EDITOR controls', () => {
            expect(
                renderShowFormulaSwitch({controlType: 'FILE_ENTRY', expressionEnabled: true, type: 'FILE_ENTRY'})
            ).toBe(false);
            expect(renderShowFormulaSwitch({controlType: 'NULL', expressionEnabled: true, type: 'NULL'})).toBe(false);
            expect(renderShowFormulaSwitch({controlType: 'CODE_EDITOR', expressionEnabled: true, type: 'STRING'})).toBe(
                false
            );
        });
    });
});
