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
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
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
    projectId: '5',
    projectVersion: 2,
    workflowIds: ['workflow-a', 'workflow-b'],
};

const supportA2aProject: A2aProjectItemType = {
    id: '12',
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
    });

    it('lists every project of the server', () => {
        render(<A2aProjectList a2aServer={a2aServer} />);

        expect(screen.getByText('Sales project')).toBeInTheDocument();
        expect(screen.getByText('Support project')).toBeInTheDocument();
        expect(screen.getByText('v2')).toBeInTheDocument();
        expect(screen.getByText('2 skills')).toBeInTheDocument();
        expect(screen.getByText('1 skill')).toBeInTheDocument();
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

    it('shows an empty state', () => {
        hoisted.a2aProjects = [];

        render(<A2aProjectList a2aServer={a2aServer} />);

        expect(screen.getByText(/No projects yet/)).toBeInTheDocument();
    });

    it('opens the skills dialog for the chosen project', async () => {
        const user = userEvent.setup();

        render(<A2aProjectList a2aServer={a2aServer} />);

        await user.click(screen.getAllByRole('button', {name: 'Project actions'})[1]);
        await user.click(await screen.findByRole('menuitem', {name: 'Edit Skills'}));

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
});
