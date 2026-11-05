import {ResponseError} from '@/shared/middleware/automation/configuration';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiHubWorkflowViewer from '../AiHubWorkflowViewer';

const hoisted = vi.hoisted(() => ({getProjectWorkflow: vi.fn()}));

vi.mock('@/pages/platform/workflow-editor/EmbeddableWorkflowEditor', () => ({
    default: (props: {projectId: number; projectWorkflowId: number; showPublishDeploy?: boolean}) => (
        <div data-testid="embedded-editor">{JSON.stringify(props)}</div>
    ),
}));

vi.mock('@/shared/middleware/automation/configuration', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/automation/configuration')>()),
    WorkflowApi: class {
        getProjectWorkflow = hoisted.getProjectWorkflow;
    },
}));

const MISSING_MESSAGE = 'This workflow no longer exists. It may have been deleted.';

const responseError = (status: number) => new ResponseError(new Response(null, {status}), 'Response returned an error');

let queryClient: QueryClient;

const renderViewer = (projectWorkflowId: number) =>
    render(
        <QueryClientProvider client={queryClient}>
            <AiHubWorkflowViewer projectId="5" projectWorkflowId={projectWorkflowId} />
        </QueryClientProvider>
    );

const refetchWorkflow = async (projectWorkflowId: number) => {
    await act(async () => {
        await queryClient.refetchQueries({queryKey: ProjectWorkflowKeys.projectWorkflow(5, projectWorkflowId)});
    });

    await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 10));
    });
};

describe('AiHubWorkflowViewer', () => {
    beforeEach(() => {
        hoisted.getProjectWorkflow.mockReset();

        queryClient = new QueryClient();
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
        hoisted.getProjectWorkflow.mockRejectedValue(responseError(400));

        renderViewer(1056);

        expect(await screen.findByText(MISSING_MESSAGE)).toBeInTheDocument();
        expect(screen.queryByTestId('embedded-editor')).toBeNull();
        expect(hoisted.getProjectWorkflow).toHaveBeenCalledOnce();
    });

    it('keeps a loaded workflow open when a later refetch fails with a server error', async () => {
        hoisted.getProjectWorkflow.mockResolvedValueOnce({id: 'wf-12'}).mockRejectedValueOnce(responseError(500));

        renderViewer(12);

        await screen.findByTestId('embedded-editor');

        await refetchWorkflow(12);

        expect(hoisted.getProjectWorkflow).toHaveBeenCalledTimes(2);
        expect(screen.getByTestId('embedded-editor')).toBeInTheDocument();
        expect(screen.queryByText(MISSING_MESSAGE)).toBeNull();
    });

    it('switches an open tab to the missing state when its workflow is deleted', async () => {
        hoisted.getProjectWorkflow.mockResolvedValueOnce({id: 'wf-12'}).mockRejectedValueOnce(responseError(400));

        renderViewer(12);

        await screen.findByTestId('embedded-editor');

        await refetchWorkflow(12);

        await waitFor(() => expect(screen.getByText(MISSING_MESSAGE)).toBeInTheDocument());
        expect(screen.queryByTestId('embedded-editor')).toBeNull();
    });
});
