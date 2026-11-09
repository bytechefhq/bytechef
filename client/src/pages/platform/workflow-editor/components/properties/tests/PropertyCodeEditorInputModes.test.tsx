vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({default: vi.fn()}));

import {TooltipProvider} from '@/components/ui/tooltip';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {PropertyAllType} from '@/shared/types';
import {render} from '@/shared/util/test-utils';
import {act, fireEvent, screen} from '@testing-library/react';
import {type Mock, beforeEach, describe, expect, it, vi} from 'vitest';

const codeProperty = {
    controlType: 'CODE_EDITOR',
    expressionEnabled: true,
    label: 'Code',
    languageId: 'python',
    name: 'code',
    required: true,
    type: 'STRING',
} as PropertyAllType;

const settle = async () => {
    for (let index = 0; index < 4; index++) {
        await act(async () => {
            await new Promise<void>((resolve) => setTimeout(resolve, 0));
        });
    }

    await act(async () => {
        await new Promise<void>((resolve) => setTimeout(resolve, 120));
    });
};

const renderCodeProperty = (value: unknown, {tools = true} = {}) => {
    useWorkflowNodeDetailsPanelStore.setState({
        currentNode: {
            clusterElementType: tools ? 'tools' : undefined,
            name: 'daytona_1',
            parameters: {code: value},
            workflowNodeName: 'daytona_1',
        },
    } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);

    return render(
        <TooltipProvider>
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                <Property parameterValue={value} path="code" property={codeProperty} />
            </WorkflowEditorProvider>
        </TooltipProvider>
    );
};

const getTextSwitch = () => screen.getByRole('switch', {name: 'Text'});

const getFormulaSwitch = () => screen.getByRole('switch', {name: 'Formula'});

const getCodeEditorButton = () => screen.queryByRole('button', {name: 'Open Code Editor'});

describe('Text and Formula switches on a code editor property', () => {
    beforeEach(() => {
        (saveProperty as unknown as Mock).mockReset();

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-code-editor-input-modes', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({currentNode: undefined, pillTarget: null});
    });

    it.each([
        {name: 'a tool', tools: true},
        {name: 'a workflow step', tools: false},
    ])('offers both switches next to the code editor of $name', async ({tools}) => {
        renderCodeProperty("print('hi')", {tools});

        await settle();

        expect(getCodeEditorButton()).toBeInTheDocument();
        expect(getTextSwitch()).not.toBeChecked();
        expect(getFormulaSwitch()).not.toBeChecked();
    });

    it('turns the code into a formula with the Formula switch', async () => {
        renderCodeProperty("print('hi')");

        await settle();

        fireEvent.click(getFormulaSwitch());

        await settle();

        expect(getCodeEditorButton()).toBeNull();
        expect(getFormulaSwitch()).toBeChecked();
        expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: "='print(''hi'')'"}));
    });

    it('opens the pill editor with the Text switch and keeps the code when it is turned off without a pill', async () => {
        renderCodeProperty("print('hi')");

        await settle();

        fireEvent.click(getTextSwitch());

        await settle();

        expect(getCodeEditorButton()).toBeNull();
        expect(getTextSwitch()).toBeChecked();

        fireEvent.click(getTextSwitch());

        await settle();

        expect(getCodeEditorButton()).toBeInTheDocument();
        expect(saveProperty).not.toHaveBeenCalled();
    });

    it('shows a code value that is a lone pill in the pill editor', async () => {
        renderCodeProperty('${httpClient_1.body}');

        await settle();

        expect(getCodeEditorButton()).toBeNull();
        expect(getTextSwitch()).toBeChecked();
    });

    it('keeps code with template literals in the code editor', async () => {
        renderCodeProperty('const greeting = `Hello ${name}`;', {tools: false});

        await settle();

        expect(getCodeEditorButton()).toBeInTheDocument();
        expect(getTextSwitch()).not.toBeChecked();
    });
});
