import {TooltipProvider} from '@/components/ui/tooltip';
import {useAiAgentEvalsStore} from '@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiAgentEditor from './AiAgentEditor';

vi.mock('@/pages/platform/cluster-element-editor/ai-agent-editor/hooks/useAiAgentEditor', () => ({
    default: () => ({
        handleNodeDetailsPanelClose: vi.fn(),
        showNodeDetailsPanel: false,
        updateWorkflowMutation: undefined,
    }),
}));

vi.mock(
    '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/AiAgentConfigurationPanel',
    () => ({AiAgentConfigurationPanel: () => <div>ai agent configuration panel</div>})
);

vi.mock(
    '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-testing-panel/AiAgentTestingPanel',
    () => ({default: () => null})
);

vi.mock('@/pages/platform/cluster-element-editor/ai-agent-evals/AiAgentEvals', () => ({
    default: () => <div>ai agent evals</div>,
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => true,
}));

vi.mock('@/pages/platform/workflow-editor/stores/useDataPillPanelStore', () => ({
    default: (selector: (state: unknown) => unknown) => selector({dataPillPanelOpen: false}),
}));

const renderAiAgentEditor = () =>
    render(
        <TooltipProvider>
            <AiAgentEditor copilotEnabled onCopilotClick={vi.fn()} />
        </TooltipProvider>
    );

describe('AiAgentEditor - evals branch selection', () => {
    beforeEach(() => {
        useAiAgentEvalsStore.setState({
            evalsPanelOpen: false,
            evalsTab: 'tests',
            selectedRunId: null,
            selectedTestId: null,
        });
    });

    it('renders the evals surface (AiAgentHeader chrome + AiAgentEvals), not the configuration panel, when evalsPanelOpen is true', () => {
        useAiAgentEvalsStore.setState({evalsPanelOpen: true});

        const {container} = renderAiAgentEditor();

        expect(screen.getByText('Evals')).toBeInTheDocument();
        expect(screen.getByText('ai agent evals')).toBeInTheDocument();
        expect(container.querySelector('svg.lucide-sparkles')).toBeInTheDocument();
        expect(screen.queryByText('ai agent configuration panel')).not.toBeInTheDocument();
    });

    it('renders the configuration panel, not the evals surface, when evalsPanelOpen is false', () => {
        renderAiAgentEditor();

        expect(screen.getByText('ai agent configuration panel')).toBeInTheDocument();
        expect(screen.queryByText('ai agent evals')).not.toBeInTheDocument();
        expect(screen.queryByText('Evals')).not.toBeInTheDocument();
    });
});
