import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiHubWorkflowViewer from '../AiHubWorkflowViewer';

const hoisted = vi.hoisted(() => ({getProjectWorkflow: vi.fn()}));

vi.mock('@/pages/platform/workflow-editor/EmbeddableWorkflowEditor', () => ({
    default: (props: {projectId: number; projectWorkflowId: number; showPublishDeploy?: boolean}) => (
        <div data-testid="embedded-editor">{JSON.stringify(props)}</div>
    ),
}));

vi.mock('@/shared/middleware/automation/configuration', () => ({
    WorkflowApi: class {
        getProjectWorkflow = hoisted.getProjectWorkflow;
    },
}));

const renderViewer = (projectWorkflowId: number) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <AiHubWorkflowViewer projectId="5" projectWorkflowId={projectWorkflowId} />
        </QueryClientProvider>
    );

describe('AiHubWorkflowViewer', () => {
    beforeEach(() => {
        hoisted.getProjectWorkflow.mockReset();
    });

    it('renders the embeddable editor for the workflow, with publish and deploy', async () => {
        hoisted.getProjectWorkflow.mockResolvedValue({id: 'wf-12'});

        renderViewer(12);

        expect(JSON.parse((await screen.findByTestId('embedded-editor')).textContent!)).toEqual({
            projectId: 5,
            projectWorkflowId: 12,
            showPublishDeploy: true,
        });
        expect(hoisted.getProjectWorkflow).toHaveBeenCalledWith({projectWorkflowId: 12});
    });

    it('explains a reference without a project workflow id', () => {
        renderViewer(0);

        expect(screen.queryByTestId('embedded-editor')).toBeNull();
        expect(screen.getByText('Workflow reference unavailable.')).toBeInTheDocument();
        expect(hoisted.getProjectWorkflow).not.toHaveBeenCalled();
    });

    it('says a deleted workflow no longer exists and asks for it only once, without mounting the editor', async () => {
        hoisted.getProjectWorkflow.mockRejectedValue(new Error('ProjectWorkflow not found'));

        renderViewer(1056);

        expect(
            await screen.findByText('This workflow no longer exists. It may have been deleted.')
        ).toBeInTheDocument();
        expect(screen.queryByTestId('embedded-editor')).toBeNull();
        expect(hoisted.getProjectWorkflow).toHaveBeenCalledOnce();
    });
});
