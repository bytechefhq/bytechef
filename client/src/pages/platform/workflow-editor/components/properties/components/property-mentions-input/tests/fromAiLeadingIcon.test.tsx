vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {render} from '@/shared/util/test-utils';
import {screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyMentionsInput from '../PropertyMentionsInput';

beforeEach(() => {
    useWorkflowDataStore.setState({
        workflow: {id: 'wf-from-ai-icon', nodeNames: []},
    } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

    useWorkflowNodeDetailsPanelStore.setState({
        currentNode: {connectionId: undefined, workflowNodeName: 'test_1'},
        pillTarget: null,
    } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
});

describe('fromAi leading icon', () => {
    it('shows the formula icon, not the type icon, on a field the model defines', () => {
        const {container} = render(
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                <PropertyMentionsInput
                    controlType="INTEGER"
                    isFromAi
                    leadingIcon={<span>type-icon</span>}
                    path="parameters.count"
                    type="INTEGER"
                    value="=fromAi('count', 'INTEGER')"
                />
            </WorkflowEditorProvider>
        );

        expect(container.querySelector('.lucide-square-function')).not.toBeNull();
        expect(screen.queryByText('type-icon')).toBeNull();
    });
});
