import {TooltipProvider} from '@/components/ui/tooltip';
import {ProjectDeploymentWorkflow} from '@/shared/middleware/automation/configuration';
import {DataSyncTriggerType} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentDataSyncListItem, {ProjectDeploymentDataSyncType} from './ProjectDeploymentDataSyncListItem';

const {enableMock, invalidateQueriesMock, runMock} = vi.hoisted(() => ({
    enableMock: vi.fn(),
    invalidateQueriesMock: vi.fn(),
    runMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<typeof import('@/shared/middleware/graphql')>('@/shared/middleware/graphql');

    return {
        ...actual,
        useRunDataSyncDeploymentMutation: () => ({isPending: false, mutate: runMock}),
    };
});

vi.mock('@/shared/mutations/automation/projectDeploymentWorkflows.mutations', () => ({
    useEnableProjectDeploymentWorkflowMutation: () => ({isPending: false, mutate: enableMock}),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@tanstack/react-query')>();

    return {
        ...actual,
        useQueryClient: () => ({invalidateQueries: invalidateQueriesMock}),
    };
});

const dataSyncDeployment: ProjectDeploymentDataSyncType = {
    dataSyncId: '10',
    dataSyncTitle: 'CRM to DB',
    enabled: true,
    environmentId: 2,
    id: '20',
    lastExecutionDate: null,
    name: 'deploy',
    projectId: '100',
    projectVersion: 1,
    triggerType: DataSyncTriggerType.Schedule,
    workflowId: 'wf',
};

const projectDeploymentWorkflow = {
    enabled: true,
    workflowId: 'wf',
    workflowUuid: 'uuid-1',
} as ProjectDeploymentWorkflow;

const renderItem = (
    overrides: Partial<ProjectDeploymentDataSyncType> = {},
    projectDeploymentWorkflowOverride: ProjectDeploymentWorkflow | 'none' = projectDeploymentWorkflow
) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <TooltipProvider>
                <ProjectDeploymentDataSyncListItem
                    dataSyncDeployment={{...dataSyncDeployment, ...overrides}}
                    projectDeploymentWorkflow={
                        projectDeploymentWorkflowOverride === 'none' ? undefined : projectDeploymentWorkflowOverride
                    }
                />
            </TooltipProvider>
        </QueryClientProvider>
    );

describe('ProjectDeploymentDataSyncListItem', () => {
    beforeEach(() => {
        enableMock.mockReset();
        invalidateQueriesMock.mockReset();
        runMock.mockReset();
    });

    it('shows the sync title', () => {
        renderItem();

        expect(screen.getByText('CRM to DB')).toBeInTheDocument();
    });

    it('shows "Scheduled" for a scheduled deployment', () => {
        renderItem();

        expect(screen.getByText('Scheduled')).toBeInTheDocument();
    });

    it('shows "Manual" for a manual deployment', () => {
        renderItem({triggerType: DataSyncTriggerType.Manual});

        expect(screen.getByText('Manual')).toBeInTheDocument();
    });

    it('shows No executions when there is no last execution date', () => {
        renderItem();

        expect(screen.getByText('No executions')).toBeInTheDocument();
    });

    it('shows the last execution date when present', () => {
        renderItem({lastExecutionDate: '2026-01-02T03:04:05Z'});

        expect(screen.getByText(/Executed at/)).toBeInTheDocument();
    });

    it('runs the deployment through the Data Sync mutation, passing the sync id first and the deployment id second', async () => {
        renderItem();

        await userEvent.click(screen.getByRole('button', {name: /run now/i}));

        expect(runMock).toHaveBeenCalledWith({id: '10', projectDeploymentId: '20'});
    });

    it('disables Run now while the deployment itself is disabled', () => {
        renderItem({enabled: false});

        expect(screen.getByRole('button', {name: /run now/i})).toBeDisabled();
    });

    it("disables Run now while the sync's own workflow row is disabled", () => {
        renderItem({}, {...projectDeploymentWorkflow, enabled: false});

        expect(screen.getByRole('button', {name: /run now/i})).toBeDisabled();
    });

    it('enables Run now while both the deployment and the workflow row are enabled', () => {
        renderItem();

        expect(screen.getByRole('button', {name: /run now/i})).toBeEnabled();
    });

    it('toggles the enable switch through the enable project deployment workflow mutation', async () => {
        renderItem();

        await userEvent.click(screen.getByRole('switch'));

        expect(enableMock).toHaveBeenCalledWith({enable: false, id: 20, workflowId: 'wf'});
    });

    it('renders no switch when the deployment has no matching workflow row', () => {
        renderItem({}, 'none');

        expect(screen.queryByRole('switch')).not.toBeInTheDocument();
    });
});
