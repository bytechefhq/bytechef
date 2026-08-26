import AgentDialog from '@/pages/automation/agents/components/AgentDialog';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {addChannelMock, createAgentMock, navigateMock, updateAgentMock} = vi.hoisted(() => ({
    addChannelMock: vi.fn(),
    createAgentMock: vi.fn(),
    navigateMock: vi.fn(),
    updateAgentMock: vi.fn(),
}));

// Only useNavigate is replaced — MemoryRouter below is still the real one, so the dialog renders inside a
// working router while the redirect it performs stays assertable.
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

const renderDialog = () =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <MemoryRouter>
                <AgentDialog onOpenChange={vi.fn()} open={true} />
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

    // The dialog stays mounted with its trigger, so nothing unmounts this state on close — an accidental
    // "Add a schedule" would otherwise attach a schedule to the next agent created from the same button.
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
