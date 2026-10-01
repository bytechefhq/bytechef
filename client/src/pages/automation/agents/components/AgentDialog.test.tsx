import AgentDialog from '@/pages/automation/agents/components/AgentDialog';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {ComponentProps} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {addChannelMock, createAgentMock, navigateMock, updateAgentMock} = vi.hoisted(() => ({
    addChannelMock: vi.fn(),
    createAgentMock: vi.fn(),
    navigateMock: vi.fn(),
    updateAgentMock: vi.fn(),
}));

vi.mock('react-router-dom', async () => ({
    ...(await vi.importActual<typeof import('react-router-dom')>('react-router-dom')),
    useNavigate: () => navigateMock,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAddAiAgentChannelMutation: () => ({mutate: addChannelMock}),
    useCreateAiAgentMutation: () => ({mutate: createAgentMock}),
    useUpdateAiAgentMutation: () => ({mutate: updateAgentMock}),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    ProjectKeys: {
        filteredProjects: (filters: {id: number}) => ['projects', filters.id, filters],
    },
    useGetWorkspaceProjectsQuery: () => ({data: [{id: 5, name: 'Existing Project'}]}),
}));

const renderDialog = (props: Partial<ComponentProps<typeof AgentDialog>> = {}) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <MemoryRouter>
                <AgentDialog onOpenChange={vi.fn()} open={true} {...props} />
            </MemoryRouter>
        </QueryClientProvider>
    );

describe('AgentDialog', () => {
    beforeEach(() => {
        addChannelMock.mockReset();
        createAgentMock.mockReset();
        navigateMock.mockReset();
        updateAgentMock.mockReset();
    });

    it('creates only the agent when no schedule is filled in', async () => {
        renderDialog();

        fireEvent.change(screen.getByPlaceholderText('Enter agent title'), {target: {value: 'Agent1'}});
        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(createAgentMock).toHaveBeenCalled());

        expect(addChannelMock).not.toHaveBeenCalled();
    });

    it('adds a schedule channel after creating the agent when one is filled in', async () => {
        createAgentMock.mockImplementation((_variables, options) => options?.onSuccess?.({createAiAgent: {id: '42'}}));

        renderDialog();

        fireEvent.change(screen.getByPlaceholderText('Enter agent title'), {target: {value: 'Agent1'}});
        fireEvent.click(screen.getByRole('button', {name: 'Add a schedule'}));
        fireEvent.change(screen.getByLabelText('Prompt'), {target: {value: 'Summarise yesterday'}});
        fireEvent.change(screen.getByLabelText('Time'), {target: {value: '09:30'}});
        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(addChannelMock).toHaveBeenCalled());

        expect(addChannelMock).toHaveBeenCalledWith(
            expect.objectContaining({
                input: expect.objectContaining({
                    agentId: '42',
                    channelType: 'schedule',
                    parameters: expect.objectContaining({expression: '30 9 * * ?', frequencyKind: 'DAILY'}),
                }),
            })
        );
    });

    it('offers every frequency, not just the daily default', () => {
        renderDialog();

        fireEvent.click(screen.getByRole('button', {name: 'Add a schedule'}));

        expect(screen.getByLabelText('Frequency')).toBeInTheDocument();
    });

    it('blocks the save while the schedule prompt is blank', async () => {
        renderDialog();

        fireEvent.change(screen.getByPlaceholderText('Enter agent title'), {target: {value: 'Agent1'}});
        fireEvent.click(screen.getByRole('button', {name: 'Add a schedule'}));
        fireEvent.change(screen.getByLabelText('Time'), {target: {value: '09:30'}});
        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(screen.getByText('Prompt is required')).toBeInTheDocument());

        expect(createAgentMock).not.toHaveBeenCalled();
    });

    it('discards a half-filled schedule when the dialog is closed', () => {
        renderDialog();

        fireEvent.click(screen.getByRole('button', {name: 'Add a schedule'}));
        fireEvent.change(screen.getByLabelText('Prompt'), {target: {value: 'Summarise yesterday'}});
        fireEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(screen.getByRole('button', {name: 'Add a schedule'})).toBeInTheDocument();
    });

    it('removes the schedule block on demand', () => {
        renderDialog();

        fireEvent.click(screen.getByRole('button', {name: 'Add a schedule'}));
        fireEvent.click(screen.getByRole('button', {name: 'Remove schedule'}));

        expect(screen.getByRole('button', {name: 'Add a schedule'})).toBeInTheDocument();
    });

    it('renders no schedule block when editing an existing agent', () => {
        render(
            <QueryClientProvider client={new QueryClient()}>
                <MemoryRouter>
                    <AgentDialog agent={{id: '1', title: 'Agent1'}} onOpenChange={vi.fn()} open={true} />
                </MemoryRouter>
            </QueryClientProvider>
        );

        expect(screen.queryByRole('button', {name: 'Add a schedule'})).not.toBeInTheDocument();
        expect(addChannelMock).not.toHaveBeenCalled();
    });

    it('creates the agent in a new project by default and lands on the project-scoped route', async () => {
        createAgentMock.mockImplementation((_variables, options) =>
            options?.onSuccess?.({createAiAgent: {id: '99', projectId: '7'}})
        );

        renderDialog();

        fireEvent.change(screen.getByPlaceholderText('Enter agent title'), {target: {value: 'Support Bot'}});
        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(createAgentMock).toHaveBeenCalled());

        expect(createAgentMock.mock.calls[0][0].input.projectId).toBeUndefined();
        expect(navigateMock).toHaveBeenCalledWith('/automation/projects/7/agents/99');
    });

    it('sends the locked project id when opened from inside a project', async () => {
        renderDialog({projectId: 7});

        fireEvent.change(screen.getByPlaceholderText('Enter agent title'), {target: {value: 'Support Bot'}});
        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(createAgentMock).toHaveBeenCalled());

        expect(createAgentMock.mock.calls[0][0].input.projectId).toBe('7');
    });

    it('does not offer a project picker when opened with a locked project', () => {
        renderDialog({projectId: 7});

        expect(screen.queryByLabelText('Project')).not.toBeInTheDocument();
    });
});

describe('AgentDialog with a required schedule', () => {
    const renderRequiredDialog = () =>
        render(
            <QueryClientProvider client={new QueryClient()}>
                <MemoryRouter>
                    <AgentDialog
                        createdRedirectPath="/automation/ai-hub/scheduled"
                        onOpenChange={vi.fn()}
                        open={true}
                        scheduleRequired
                    />
                </MemoryRouter>
            </QueryClientProvider>
        );

    beforeEach(() => {
        addChannelMock.mockReset();
        createAgentMock.mockReset();
        navigateMock.mockReset();
        updateAgentMock.mockReset();
    });

    it('opens with the schedule already shown and no way to drop it', () => {
        renderRequiredDialog();

        expect(screen.getByLabelText('Frequency')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Add a schedule'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Remove schedule'})).not.toBeInTheDocument();
    });

    it('does not create the agent while the schedule is incomplete', async () => {
        renderRequiredDialog();

        fireEvent.change(screen.getByPlaceholderText('Enter agent title'), {target: {value: 'Agent1'}});
        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(screen.getByText('Prompt is required')).toBeInTheDocument());

        expect(createAgentMock).not.toHaveBeenCalled();
    });

    it('returns to the page it was opened from instead of the new agent', async () => {
        createAgentMock.mockImplementation((_variables, options) => options?.onSuccess?.({createAiAgent: {id: '42'}}));

        renderRequiredDialog();

        fireEvent.change(screen.getByPlaceholderText('Enter agent title'), {target: {value: 'Agent1'}});
        fireEvent.change(screen.getByLabelText('Prompt'), {target: {value: 'Summarise yesterday'}});
        fireEvent.change(screen.getByLabelText('Time'), {target: {value: '09:30'}});
        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(addChannelMock).toHaveBeenCalled());

        expect(navigateMock).toHaveBeenCalledWith('/automation/ai-hub/scheduled');
    });
});
