import {A2aServer} from '@/shared/middleware/graphql';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import A2aProjectList from '../A2aProjectList';
import {A2aProjectItemType} from '../hooks/useA2aProjectList';

const hoisted = vi.hoisted(() => ({
    a2aProjects: [] as Array<A2aProjectItemType | null>,
    deleteMutate: vi.fn(),
    invalidateQueries: vi.fn(),
    isError: false,
    isLoading: false,
    updateEnabledMutate: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useA2aProjectWorkflowsByA2aProjectIdQuery: ({a2aProjectId}: {a2aProjectId: string}) => ({
        data: {
            a2aProjectWorkflowsByA2aProjectId:
                a2aProjectId === '11'
                    ? [
                          {
                              enabled: true,
                              id: '21',
                              skillName: 'Qualify lead',
                              workflowId: 'workflow-a',
                              workflowLabel: 'Lead intake',
                          },
                          {
                              enabled: false,
                              id: '22',
                              skillName: null,
                              workflowId: 'workflow-b',
                              workflowLabel: 'Follow up',
                          },
                      ]
                    : [{enabled: true, id: '23', skillName: null, workflowId: 'workflow-c', workflowLabel: 'Triage'}],
        },
        isLoading: false,
    }),
    useA2aProjectsByServerIdQuery: () => ({
        data: hoisted.isError || hoisted.isLoading ? undefined : {a2aProjectsByServerId: hoisted.a2aProjects},
        isError: hoisted.isError,
        isLoading: hoisted.isLoading,
    }),
    useDeleteA2aProjectMutation: (options?: {onSuccess?: () => void}) => ({
        isPending: false,
        mutate: (variables: {id: string}) => {
            hoisted.deleteMutate(variables);

            options?.onSuccess?.();
        },
    }),
    useUpdateA2aProjectMutation: (options?: {onSuccess?: () => void}) => ({
        isPending: false,
        mutate: (variables: {id: string; input: {selectedWorkflowIds: string[]}}) => {
            hoisted.updateMutate(variables);

            options?.onSuccess?.();
        },
    }),
    useUpdateA2aProjectWorkflowEnabledMutation: () => ({
        isPending: false,
        mutate: hoisted.updateEnabledMutate,
    }),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    useGetWorkspaceProjectsQuery: () => ({
        data: [
            {id: 5, name: 'Sales project'},
            {id: 6, name: 'Support project'},
        ],
    }),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => {
    const state = {currentWorkspaceId: 1};

    return {useWorkspaceStore: (selector: (currentState: typeof state) => unknown) => selector(state)};
});

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    useGetProjectDeploymentQuery: (id: number, enabled: boolean) => ({
        data: enabled
            ? {
                  id,
                  projectDeploymentWorkflows: [
                      {id: 41, workflowId: 'workflow-a', workflowUuid: 'uuid-a'},
                      {id: 42, workflowId: 'workflow-x', workflowUuid: 'uuid-x'},
                  ],
              }
            : undefined,
    }),
}));

vi.mock('@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: ({
        filterWorkflowUuids,
        projectDeployment,
    }: {
        filterWorkflowUuids: string[];
        projectDeployment: {id: number};
    }) => <div>{`Changing version of deployment ${projectDeployment.id} for ${filterWorkflowUuids.join(',')}`}</div>,
}));

vi.mock('../../A2aServerWorkflowDialog', () => ({
    default: ({a2aProject, open}: {a2aProject?: A2aProjectItemType; open: boolean}) =>
        open ? <div>{`Editing skills of A2A project ${a2aProject?.id}`}</div> : null,
}));

const a2aServer = {
    authenticationRequired: true,
    enabled: true,
    environmentId: '2',
    id: '7',
    name: 'Sales agent',
} as A2aServer;

const salesA2aProject: A2aProjectItemType = {
    id: '11',
    lastModifiedDate: Date.UTC(2026, 8, 7, 19, 20, 7),
    projectDeploymentId: '31',
    projectId: '5',
    projectVersion: 2,
    workflowIds: ['workflow-a', 'workflow-b'],
};

const supportA2aProject: A2aProjectItemType = {
    id: '12',
    lastModifiedDate: null,
    projectDeploymentId: '32',
    projectId: '6',
    projectVersion: 1,
    workflowIds: ['workflow-c'],
};

describe('A2aProjectList', () => {
    beforeEach(() => {
        hoisted.a2aProjects = [salesA2aProject, null, supportA2aProject];
        hoisted.deleteMutate.mockReset();
        hoisted.invalidateQueries.mockReset();
        hoisted.isError = false;
        hoisted.isLoading = false;
        hoisted.updateEnabledMutate.mockReset();
        hoisted.updateMutate.mockReset();
    });

    it('lists every project of the server', () => {
        render(<A2aProjectList a2aServer={a2aServer} />);

        expect(screen.getByText('Sales project')).toBeInTheDocument();
        expect(screen.getByText('Support project')).toBeInTheDocument();
        expect(screen.getByText('v2')).toBeInTheDocument();
        expect(screen.getAllByRole('button', {name: 'Project actions'})).toHaveLength(2);
    });

    it('shows a loading state', () => {
        hoisted.isLoading = true;

        render(<A2aProjectList a2aServer={a2aServer} />);

        expect(screen.getByText('Loading projects...')).toBeInTheDocument();
    });

    it('shows an error state', () => {
        hoisted.isError = true;

        render(<A2aProjectList a2aServer={a2aServer} />);

        expect(screen.getByText('The projects of this server could not be loaded.')).toBeInTheDocument();
    });

    it('shows when a project was last modified', () => {
        hoisted.a2aProjects = [salesA2aProject, supportA2aProject];

        render(<A2aProjectList a2aServer={a2aServer} />);

        const lastModifiedDate = new Date(Date.UTC(2026, 8, 7, 19, 20, 7));

        expect(
            screen.getByText(
                `Modified at ${lastModifiedDate.toLocaleDateString()} ${lastModifiedDate.toLocaleTimeString()}`
            )
        ).toBeInTheDocument();
        expect(screen.getAllByText(/Modified at/)).toHaveLength(1);
    });

    it('renders nothing when the server has no projects', () => {
        hoisted.a2aProjects = [];

        const {container} = render(<A2aProjectList a2aServer={a2aServer} />);

        expect(container).toBeEmptyDOMElement();
    });

    it('opens the workflows dialog for the chosen project', async () => {
        const user = userEvent.setup();

        render(<A2aProjectList a2aServer={a2aServer} />);

        await user.click(screen.getAllByRole('button', {name: 'Project actions'})[1]);
        await user.click(await screen.findByRole('menuitem', {name: 'Edit Workflows'}));

        expect(await screen.findByText('Editing skills of A2A project 12')).toBeInTheDocument();
    });

    it('deletes the chosen project only after confirmation', async () => {
        const user = userEvent.setup();

        render(<A2aProjectList a2aServer={a2aServer} />);

        await user.click(screen.getAllByRole('button', {name: 'Project actions'})[0]);
        await user.click(await screen.findByRole('menuitem', {name: 'Delete'}));

        expect(await screen.findByText('Are you absolutely sure?')).toBeInTheDocument();
        expect(hoisted.deleteMutate).not.toHaveBeenCalled();

        await user.click(screen.getByRole('button', {name: 'Delete'}));

        await waitFor(() => expect(hoisted.deleteMutate).toHaveBeenCalledWith({id: '11'}));

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['a2aProjectsByServerId']});
    });

    it('lists the workflows of a project once it is expanded', async () => {
        const user = userEvent.setup();

        render(<A2aProjectList a2aServer={a2aServer} />);

        expect(screen.queryByText('Lead intake')).not.toBeInTheDocument();

        await user.click(screen.getAllByRole('button', {name: 'Expand project'})[0]);

        expect(screen.getByText('Lead intake')).toBeInTheDocument();
        expect(screen.getByText('Skill: Qualify lead')).toBeInTheDocument();
        expect(screen.getByText('Follow up')).toBeInTheDocument();
        expect(screen.queryByText('Triage')).not.toBeInTheDocument();
    });

    it('removes one workflow and keeps the rest of the project', async () => {
        const user = userEvent.setup();

        render(<A2aProjectList a2aServer={a2aServer} />);

        await user.click(screen.getAllByRole('button', {name: 'Expand project'})[0]);
        await user.click(screen.getByRole('button', {name: 'Remove Lead intake'}));
        await user.click(await screen.findByRole('button', {name: 'Delete'}));

        await waitFor(() =>
            expect(hoisted.updateMutate).toHaveBeenCalledWith({
                id: '11',
                input: {selectedWorkflowIds: ['workflow-b']},
            })
        );

        expect(hoisted.deleteMutate).not.toHaveBeenCalled();
    });

    it('deletes the project when its last workflow is removed', async () => {
        const user = userEvent.setup();

        render(<A2aProjectList a2aServer={a2aServer} />);

        await user.click(screen.getAllByRole('button', {name: 'Expand project'})[1]);
        await user.click(screen.getByRole('button', {name: 'Remove Triage'}));
        await user.click(await screen.findByRole('button', {name: 'Delete'}));

        await waitFor(() => expect(hoisted.deleteMutate).toHaveBeenCalledWith({id: '12'}));

        expect(hoisted.updateMutate).not.toHaveBeenCalled();
    });

    it('changes the project version of the chosen project through its deployment', async () => {
        const user = userEvent.setup();

        render(<A2aProjectList a2aServer={a2aServer} />);

        await user.click(screen.getAllByRole('button', {name: 'Project actions'})[0]);
        await user.click(await screen.findByRole('menuitem', {name: 'Change Project Version'}));

        expect(await screen.findByText('Changing version of deployment 31 for uuid-a')).toBeInTheDocument();
    });

    it('enables and disables a workflow from its toggle', async () => {
        const user = userEvent.setup();

        render(<A2aProjectList a2aServer={a2aServer} />);

        await user.click(screen.getAllByRole('button', {name: 'Expand project'})[0]);

        expect(screen.getByRole('switch', {name: 'Enable Lead intake'})).toBeChecked();
        expect(screen.getByRole('switch', {name: 'Enable Follow up'})).not.toBeChecked();

        await user.click(screen.getByRole('switch', {name: 'Enable Follow up'}));

        expect(hoisted.updateEnabledMutate).toHaveBeenCalledWith({enabled: true, id: '22'});
    });
});
