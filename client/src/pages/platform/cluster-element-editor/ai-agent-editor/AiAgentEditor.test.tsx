import AiAgentEditor from '@/pages/platform/cluster-element-editor/ai-agent-editor/AiAgentEditor';
import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/pages/platform/cluster-element-editor/ai-agent-editor/hooks/useAiAgentEditor', () => ({
    default: () => ({
        handleNodeDetailsPanelClose: vi.fn(),
        showNodeDetailsPanel: false,
        updateWorkflowMutation: {},
    }),
}));

vi.mock('@/pages/platform/cluster-element-editor/ai-agent-evals/hooks/useAiAgentEvals', () => ({
    default: () => ({handleClose: vi.fn()}),
}));

vi.mock('@/pages/platform/cluster-element-editor/ai-agent-editor/components/AiAgentHeader', () => ({
    default: () => null,
}));

vi.mock(
    '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/AiAgentConfigurationPanel',
    () => ({
        AiAgentConfigurationPanel: () => null,
    })
);

vi.mock(
    '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-testing-panel/AiAgentTestingPanel',
    () => ({
        default: () => <div>Agent Playbook</div>,
    })
);

vi.mock('@/pages/platform/cluster-element-editor/ai-agent-evals/AiAgentEvals', () => ({
    default: () => null,
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodeDetailsPanel', () => ({
    default: () => null,
}));

const renderEditor = (readOnly: boolean) =>
    render(
        <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
            <AiAgentEditor />
        </WorkflowEditorReadOnlyContext.Provider>
    );

afterEach(() => {
    resetAll();
});

describe('AiAgentEditor', () => {
    it('shows the testing panel when the workflow is editable', () => {
        renderEditor(false);

        expect(screen.getByText('Agent Playbook')).toBeInTheDocument();
    });

    it('hides the testing panel from a viewer who cannot edit the workflow', () => {
        renderEditor(true);

        expect(screen.queryByText('Agent Playbook')).not.toBeInTheDocument();
    });
});
