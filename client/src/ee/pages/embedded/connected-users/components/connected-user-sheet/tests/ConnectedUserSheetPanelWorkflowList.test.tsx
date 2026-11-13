import {TooltipProvider} from '@/components/ui/tooltip';
import ConnectedUserSheetPanelWorkflowList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelWorkflowList';
import {ConnectedUserWorkflowTemplateSourceI} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowListItem';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render as testingLibraryRender, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactElement} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const render = (ui: ReactElement) => testingLibraryRender(ui, {wrapper: TooltipProvider});

const hoisted = vi.hoisted(() => ({
    automationProjectsQuery: vi.fn(),
    connectedUserProjectsQuery: vi.fn(),
    provisionableProjects: [{projectId: '5'}] as unknown[],
    workflowListConnectedUsers: [] as unknown[],
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationProjectsQuery: hoisted.automationProjectsQuery,
    useConnectedUserProjectsQuery: hoisted.connectedUserProjectsQuery,
    useProvisionableConnectedUserProjectsQuery: () => ({
        data: {provisionableConnectedUserProjects: hoisted.provisionableProjects},
    }),
}));

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-project-workflow-dialog/ProvisionProjectWorkflowDialog',
    () => ({
        default: ({connectedUserId}: {connectedUserId: number}) => <div>{`provision dialog ${connectedUserId}`}</div>,
    })
);

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 2}),
}));

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowList',
    () => ({
        default: ({
            connectedUser,
            connectedUserProjectWorkflows,
            workflowTemplateSourcesByWorkflowUuid,
        }: {
            connectedUser: unknown;
            connectedUserProjectWorkflows: Array<{id: string}>;
            workflowTemplateSourcesByWorkflowUuid: Map<string, ConnectedUserWorkflowTemplateSourceI>;
        }) => {
            hoisted.workflowListConnectedUsers.push(connectedUser);

            return (
                <div>
                    {`workflows ${connectedUserProjectWorkflows.map((workflow) => workflow.id).join(',')} templates ${[
                        ...workflowTemplateSourcesByWorkflowUuid.entries(),
                    ]
                        .map(([uuid, source]) => `${uuid}=${source.projectName}`)
                        .join(',')}`}
                </div>
            );
        },
    })
);

describe('ConnectedUserSheetPanelWorkflowList', () => {
    beforeEach(() => {
        hoisted.provisionableProjects = [{projectId: '5'}];

        hoisted.workflowListConnectedUsers = [];
        hoisted.automationProjectsQuery.mockReturnValue({
            data: {
                automationProjects: [
                    {name: 'Automation Playground', workflowTemplates: [{workflowUuid: 'template-greeting'}]},
                ],
            },
            isLoading: false,
        });
        hoisted.connectedUserProjectsQuery.mockReturnValue({
            data: {
                connectedUserProjects: [
                    {
                        automationProjectConfigurations: [],
                        connectedUser: {externalId: 'ext-1050'},
                        connectedUserProjectWorkflows: [{id: '1'}, {id: '2'}],
                    },
                ],
            },
            isLoading: false,
        });
    });

    it("lists the user's workflows in the current environment with their source templates", () => {
        render(
            <MemoryRouter>
                <ConnectedUserSheetPanelWorkflowList connectedUserId={1050} />
            </MemoryRouter>
        );

        expect(hoisted.connectedUserProjectsQuery).toHaveBeenCalledWith({connectedUserId: '1050', environmentId: '2'});
        expect(screen.getByText('workflows 1,2 templates template-greeting=Automation Playground')).toBeInTheDocument();
    });

    it('shows a loading state and then an empty state', () => {
        hoisted.connectedUserProjectsQuery.mockReturnValue({data: undefined, isLoading: true});

        const {rerender} = render(
            <MemoryRouter>
                <ConnectedUserSheetPanelWorkflowList connectedUserId={1050} />
            </MemoryRouter>
        );

        expect(screen.getByRole('status', {name: 'Loading automations'})).toBeInTheDocument();
        expect(screen.queryByText('Loading...')).not.toBeInTheDocument();

        hoisted.connectedUserProjectsQuery.mockReturnValue({data: {connectedUserProjects: []}, isLoading: false});

        rerender(
            <MemoryRouter>
                <ConnectedUserSheetPanelWorkflowList connectedUserId={1050} />
            </MemoryRouter>
        );

        expect(screen.getByRole('heading', {name: 'No automations yet'})).toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: 'Automations'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Provision Workflow'})).toBeInTheDocument();
    });

    it('offers to open the Automations page when no project configuration is released', () => {
        hoisted.connectedUserProjectsQuery.mockReturnValue({data: {connectedUserProjects: []}, isLoading: false});
        hoisted.provisionableProjects = [];

        render(
            <MemoryRouter>
                <ConnectedUserSheetPanelWorkflowList connectedUserId={1050} />
            </MemoryRouter>
        );

        expect(screen.getByText(/No project has a configuration released/)).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Go to Automations'})).toBeInTheDocument();
    });

    it('opens the provisioning dialog from the Provision Workflow button', async () => {
        render(
            <QueryClientProvider client={new QueryClient()}>
                <ConnectedUserSheetPanelWorkflowList connectedUserId={1050} />
            </QueryClientProvider>
        );
        expect(screen.queryByText('provision dialog 1050')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Provision Workflow'}));

        expect(screen.getByText('provision dialog 1050')).toBeInTheDocument();
    });

    it('disables Provision Workflow and explains why when no project configuration is released', async () => {
        hoisted.provisionableProjects = [];

        render(
            <QueryClientProvider client={new QueryClient()}>
                <ConnectedUserSheetPanelWorkflowList connectedUserId={1050} />
            </QueryClientProvider>
        );

        const provisionButton = screen.getByRole('button', {name: 'Provision Workflow'});

        expect(provisionButton).toBeDisabled();

        await userEvent.hover(provisionButton.parentElement!);

        expect(
            await screen.findByRole('tooltip', {
                name: "No project has a configuration released to this user's environment yet.",
            })
        ).toBeInTheDocument();
        expect(screen.queryByText('provision dialog 1050')).not.toBeInTheDocument();
    });

    it('labels the connected user by name and email in the configuration menu', () => {
        render(
            <ConnectedUserSheetPanelWorkflowList
                connectedUserEmail="jane@example.com"
                connectedUserId={1050}
                connectedUserName="Jane"
            />
        );

        expect(hoisted.workflowListConnectedUsers.at(-1)).toEqual({
            connectedUserId: '1050',
            email: 'jane@example.com',
            externalId: 'ext-1050',
            name: 'Jane',
        });
    });
});
