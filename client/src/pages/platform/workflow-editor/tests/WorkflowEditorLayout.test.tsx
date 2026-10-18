import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowEditorLayout from '../WorkflowEditorLayout';

const routeParams = vi.hoisted(() => ({current: {} as Record<string, string>}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useParams: () => routeParams.current,
}));

vi.mock('@/pages/platform/workflow-editor/hooks/useWorkflowLayout', () => ({
    useWorkflowLayout: () => ({}),
}));

vi.mock('@/pages/platform/workflow-editor/hooks/useWorkflowEditorLayout', () => ({
    default: () => ({}),
}));

vi.mock('@/pages/platform/workflow-editor/hooks/useWorkflowIssues', () => ({default: () => []}));
vi.mock('@/pages/platform/workflow-editor/hooks/useWorkflowIssuesSweep', () => ({default: () => undefined}));
vi.mock('@/pages/platform/workflow-editor/hooks/useWorkflowIssuesValidation', () => ({default: () => undefined}));
vi.mock('@/pages/platform/workflow-editor/hooks/useDelayedUnmount', () => ({
    default: () => ({mounted: false, visible: false}),
}));
vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({}),
}));
vi.mock('@/shared/components/copilot/hooks/useCopilotLayoutShifted', () => ({default: () => false}));
vi.mock('@/pages/platform/workflow-editor/components/ClusterElementsCanvasDialog', () => ({default: () => null}));
vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodeDetailsPanel', () => ({default: () => null}));
vi.mock('@/pages/platform/workflow-editor/components/workflow-test-chat/WorkflowTestChatPanel', () => ({
    default: () => null,
}));
vi.mock('@/pages/platform/workflow-editor/components/SubflowBanner', () => ({default: () => null}));

describe('WorkflowEditorLayout post-turn invalidation', () => {
    let queryClient: QueryClient;
    let invalidateQueries: ReturnType<typeof vi.spyOn>;

    const renderLayout = () =>
        render(
            <QueryClientProvider client={queryClient}>
                <WorkflowEditorLayout runDisabled={false} showWorkflowInputs={false} />
            </QueryClientProvider>
        );

    beforeEach(() => {
        queryClient = new QueryClient();
        invalidateQueries = vi.spyOn(queryClient, 'invalidateQueries');
    });

    it('invalidates workflow test configurations after a Copilot turn in the project editor', () => {
        routeParams.current = {projectId: '1', projectWorkflowId: '2'};

        renderLayout();

        useCopilotPostTurnRegistry.getState().runFor(Source.WORKFLOW_EDITOR);

        expect(invalidateQueries).toHaveBeenCalledWith({queryKey: ['workflowTestConfigurations']});
    });

    it('invalidates workflow test configurations after a Copilot turn in the embedded editor', () => {
        routeParams.current = {integrationId: '1', integrationWorkflowId: '2'};

        renderLayout();

        useCopilotPostTurnRegistry.getState().runFor(Source.WORKFLOW_EDITOR_EMBEDDED);

        expect(invalidateQueries).toHaveBeenCalledWith({queryKey: ['workflowTestConfigurations']});
    });
});
