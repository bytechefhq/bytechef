import {ProjectDeploymentWorkflow} from '@/shared/middleware/automation/configuration';
import {DataSyncTriggerType} from '@/shared/middleware/graphql';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentDataSyncList from './ProjectDeploymentDataSyncList';
import {ProjectDeploymentDataSyncType} from './ProjectDeploymentDataSyncListItem';

const hoisted = vi.hoisted(() => ({
    dataSyncs: [] as {id: string; projectId: string; projectWorkflowUuid: string}[],
    itemPropsMock: vi.fn(),
}));

vi.mock('@/pages/automation/data-syncs/hooks/useDataSyncs', () => ({
    default: () => ({dataSyncs: hoisted.dataSyncs}),
}));

vi.mock('./ProjectDeploymentDataSyncListItem', async () => {
    const actual = await vi.importActual<typeof import('./ProjectDeploymentDataSyncListItem')>(
        './ProjectDeploymentDataSyncListItem'
    );

    return {
        ...actual,
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        default: (props: any) => {
            hoisted.itemPropsMock(props);

            return <div data-testid={`item-${props.dataSyncDeployment.dataSyncId}`} />;
        },
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

describe('ProjectDeploymentDataSyncList', () => {
    beforeEach(() => {
        hoisted.dataSyncs = [{id: '10', projectId: '100', projectWorkflowUuid: 'uuid-1'}];
        hoisted.itemPropsMock.mockReset();
    });

    it('shows the empty state when the deployment carries no data syncs', () => {
        render(<ProjectDeploymentDataSyncList dataSyncDeployments={[]} projectDeploymentWorkflows={[]} />);

        expect(screen.getByText('This deployment has no data syncs.')).toBeInTheDocument();
    });

    it('renders one row per data sync deployment', () => {
        render(
            <ProjectDeploymentDataSyncList
                dataSyncDeployments={[dataSyncDeployment]}
                projectDeploymentWorkflows={[projectDeploymentWorkflow]}
            />
        );

        expect(screen.getByTestId('item-10')).toBeInTheDocument();
    });

    it("finds the sync's workflow row by matching the DataSync's projectWorkflowUuid to the row's workflowUuid", () => {
        render(
            <ProjectDeploymentDataSyncList
                dataSyncDeployments={[dataSyncDeployment]}
                projectDeploymentWorkflows={[
                    {enabled: false, workflowId: 'unrelated', workflowUuid: 'other-uuid'} as ProjectDeploymentWorkflow,
                    projectDeploymentWorkflow,
                ]}
            />
        );

        expect(hoisted.itemPropsMock).toHaveBeenLastCalledWith(expect.objectContaining({projectDeploymentWorkflow}));
    });

    it('passes no matching workflow row when no DataSync resolves the uuid', () => {
        hoisted.dataSyncs = [];

        render(
            <ProjectDeploymentDataSyncList
                dataSyncDeployments={[dataSyncDeployment]}
                projectDeploymentWorkflows={[projectDeploymentWorkflow]}
            />
        );

        expect(hoisted.itemPropsMock).toHaveBeenLastCalledWith(
            expect.objectContaining({projectDeploymentWorkflow: undefined})
        );
    });
});
