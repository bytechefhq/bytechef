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

const getFromAiButton = (container: HTMLElement) =>
    container.querySelector('.lucide-sparkles')?.closest('button') ?? null;

describe('fromAi on a code editor property', () => {
    beforeEach(() => {
        (saveProperty as unknown as Mock).mockReset();

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-code-editor-from-ai', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({currentNode: undefined, pillTarget: null});
    });

    it('offers the fromAi button next to the code editor of a tool', async () => {
        const {container} = renderCodeProperty("print('hi')");

        await settle();

        expect(screen.getByRole('button', {name: 'Open Code Editor'})).toBeInTheDocument();
        expect(getFromAiButton(container)).not.toBeNull();
    });

    it('lets the model define the code of a tool once the fromAi button is clicked', async () => {
        const {container} = renderCodeProperty("print('hi')");

        await settle();

        fireEvent.click(getFromAiButton(container)!);

        await settle();

        expect(screen.getByText('Automatically defined by the model')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Open Code Editor'})).toBeNull();
        expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({fromAi: true, path: 'code'}));
    });

    it('shows a tool code value that is already defined by the model as fromAi', async () => {
        renderCodeProperty("=fromAi('code', 'STRING', {'required': true})");

        await settle();

        expect(screen.getByText('Automatically defined by the model')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Open Code Editor'})).toBeNull();
    });

    it('shows a customized fromAi formula of a tool as a formula instead of code', async () => {
        const {container} = renderCodeProperty(
            "=fromAi('code', 'STRING', {'description': 'Complete Python 3 program to run.'})"
        );

        await settle();

        expect(container.textContent).toContain("fromAi('code', 'STRING'");
        expect(screen.queryByRole('button', {name: 'Open Code Editor'})).toBeNull();
    });

    it('keeps only the code editor on a property that is not a tool property', async () => {
        const {container} = renderCodeProperty("print('hi')", {tools: false});

        await settle();

        expect(screen.getByRole('button', {name: 'Open Code Editor'})).toBeInTheDocument();
        expect(getFromAiButton(container)).toBeNull();
    });
});
