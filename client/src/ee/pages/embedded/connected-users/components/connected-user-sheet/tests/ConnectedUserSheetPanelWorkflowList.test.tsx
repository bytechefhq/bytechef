import ConnectedUserSheetPanelWorkflowList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelWorkflowList';
import {ConnectedUserWorkflowTemplateSourceI} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowListItem';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    automationWorkflowProjectsQuery: vi.fn(),
    connectedUserProjectsQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationWorkflowProjectsQuery: hoisted.automationWorkflowProjectsQuery,
    useConnectedUserProjectsQuery: hoisted.connectedUserProjectsQuery,
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 2}),
}));

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowList',
    () => ({
        default: ({
            connectedUserProjectWorkflows,
            workflowTemplateSourcesByWorkflowUuid,
        }: {
            connectedUserProjectWorkflows: Array<{id: string}>;
            workflowTemplateSourcesByWorkflowUuid: Map<string, ConnectedUserWorkflowTemplateSourceI>;
        }) => (
            <div>
                {`workflows ${connectedUserProjectWorkflows.map((workflow) => workflow.id).join(',')} templates ${[
                    ...workflowTemplateSourcesByWorkflowUuid.entries(),
                ]
                    .map(([uuid, source]) => `${uuid}=${source.projectName}`)
                    .join(',')}`}
            </div>
        ),
    })
);

describe('ConnectedUserSheetPanelWorkflowList', () => {
    beforeEach(() => {
        hoisted.automationWorkflowProjectsQuery.mockReturnValue({
            data: {
                automationWorkflowProjects: [
                    {name: 'Automation Playground', workflowTemplates: [{workflowUuid: 'template-greeting'}]},
                ],
            },
            isLoading: false,
        });
        hoisted.connectedUserProjectsQuery.mockReturnValue({
            data: {connectedUserProjects: [{connectedUserProjectWorkflows: [{id: '1'}, {id: '2'}]}]},
            isLoading: false,
        });
    });

    it("lists the user's workflows in the current environment with their source templates", () => {
        render(<ConnectedUserSheetPanelWorkflowList connectedUserId={1050} />);

        expect(hoisted.connectedUserProjectsQuery).toHaveBeenCalledWith({connectedUserId: '1050', environmentId: '2'});
        expect(screen.getByText('workflows 1,2 templates template-greeting=Automation Playground')).toBeInTheDocument();
    });

    it('shows a loading state and then an empty state', () => {
        hoisted.connectedUserProjectsQuery.mockReturnValue({data: undefined, isLoading: true});

        const {rerender} = render(<ConnectedUserSheetPanelWorkflowList connectedUserId={1050} />);

        expect(screen.getByText('Loading...')).toBeInTheDocument();

        hoisted.connectedUserProjectsQuery.mockReturnValue({data: {connectedUserProjects: []}, isLoading: false});

        rerender(<ConnectedUserSheetPanelWorkflowList connectedUserId={1050} />);

        expect(screen.getByText('No automation workflows.')).toBeInTheDocument();
    });
});
