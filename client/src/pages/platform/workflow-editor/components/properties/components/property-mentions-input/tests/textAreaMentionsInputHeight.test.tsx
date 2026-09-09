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
 * A TEXT_AREA property renders as a mentions input outside a form, and as a text area inside one. Both open at
 * the height of a single line input and grow with their content.
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

    it('opens a text area at the height of a single line input', () => {
        const {container} = renderInput('TEXT_AREA');

        expect(getEditorContainer(container)).toHaveClass('min-h-9');
    });

    it('opens a plain text property at the same height', () => {
        const {container} = renderInput('TEXT');

        expect(getEditorContainer(container)).toHaveClass('min-h-9');
    });

    it('grows with its content instead of reserving rows', () => {
        const {container} = renderInput('TEXT_AREA');

        expect(getEditorContainer(container)).not.toHaveClass('min-h-14');
        expect(getEditorContainer(container)).not.toHaveClass('min-h-16');
    });
});
