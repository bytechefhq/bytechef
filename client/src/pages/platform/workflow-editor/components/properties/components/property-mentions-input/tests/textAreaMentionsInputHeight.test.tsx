vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {type ControlType} from '@/shared/middleware/platform/configuration';
import {render} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyMentionsInput from '../PropertyMentionsInput';

/**
 * A TEXT_AREA property renders as a mentions input outside a form, and as a text area inside one. The text
 * area sits on the min-h-16 floor its own primitive sets, while the mentions input opened one row tall, so
 * the same property looked like a different control per surface.
 */

const renderInput = (controlType: ControlType, isFormulaMode = false) =>
    render(
        <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
            <PropertyMentionsInput
                controlType={controlType}
                isFormulaMode={isFormulaMode}
                label="Prompt"
                path="parameters.prompt"
                type="STRING"
                value=""
            />
        </WorkflowEditorProvider>
    );

const getEditorContainer = (container: HTMLElement) => container.querySelector('.property-mentions-editor')!;

describe('text area mentions input height', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({
            workflow: {id: 'wf-text-area-height', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {connectionId: undefined, workflowNodeName: 'test_1'},
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    it('opens a text area taller than a single row', () => {
        const {container} = renderInput('TEXT_AREA');

        expect(getEditorContainer(container)).toHaveClass('min-h-16');
    });

    it('leaves a plain text property one row tall', () => {
        const {container} = renderInput('TEXT');

        expect(getEditorContainer(container)).not.toHaveClass('min-h-16');
    });

    it('keeps a text area in formula mode one row tall', () => {
        const {container} = renderInput('TEXT_AREA', true);

        expect(getEditorContainer(container)).not.toHaveClass('min-h-16');
    });
});
