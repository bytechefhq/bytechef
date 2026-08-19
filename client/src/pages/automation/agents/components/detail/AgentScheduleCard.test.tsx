import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AgentScheduleCard from './AgentScheduleCard';

const {addAgentChannelMutate, deleteAgentChannelMutate, updateAgentChannelMutate} = vi.hoisted(() => ({
    addAgentChannelMutate: vi.fn(),
    deleteAgentChannelMutate: vi.fn(),
    updateAgentChannelMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAddAiAgentChannelMutation: () => ({isPending: false, mutate: addAgentChannelMutate}),
    useDeleteAiAgentChannelMutation: () => ({isPending: false, mutate: deleteAgentChannelMutate}),
    useUpdateAiAgentChannelMutation: () => ({isPending: false, mutate: updateAgentChannelMutate}),
}));

const scheduleChannel = {
    channelType: 'schedule',
    connectionId: null,
    id: 'schedule-1',
    parameters: {expression: '0 9 * * *', prompt: 'Summarize yesterday', timezone: 'UTC'},
    position: 0,
};

const slackChannel = {
    channelType: 'slack',
    connectionId: null,
    id: 'slack-1',
    parameters: {},
    position: 1,
};

const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
};

beforeEach(() => {
    addAgentChannelMutate.mockReset();
    deleteAgentChannelMutate.mockReset();
    updateAgentChannelMutate.mockReset();
});

describe('AgentScheduleCard', () => {
    it('shows the empty state when the agent has no schedule channels', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        wrap(<AgentScheduleCard agentId="agent-1" channels={[slackChannel] as any} />);

        expect(screen.getByText(/no schedules yet/i)).toBeInTheDocument();
    });

    // Adding opens the dialog rather than creating a blank row: a schedule with no cadence never runs, so the
    // frequency is collected before the row exists. The dialog defaults to DAILY, so only a time is needed.
    it('adds a schedule channel with the values entered in the dialog', async () => {
        wrap(<AgentScheduleCard agentId="agent-1" channels={[]} />);

        await userEvent.click(screen.getByRole('button', {name: 'Add schedule'}));

        fireEvent.change(screen.getByLabelText('Name'), {target: {value: 'Daily digest'}});
        fireEvent.change(screen.getByLabelText('Time'), {target: {value: '09:00'}});
        fireEvent.change(screen.getByLabelText('Prompt'), {target: {value: 'Summarise yesterday'}});
        await userEvent.click(screen.getByRole('button', {name: 'Add'}));

        expect(addAgentChannelMutate).toHaveBeenCalledWith({
            input: {
                agentId: 'agent-1',
                channelType: 'schedule',
                parameters: {
                    expression: '0 9 * * ?',
                    frequencyKind: 'DAILY',
                    name: 'Daily digest',
                    prompt: 'Summarise yesterday',
                    timeOfDay: '09:00',
                    timezone: 'UTC',
                },
            },
        });
    });

    // A blank prompt is the one invalid schedule the server accepts: the generator substitutes it into
    // branch_in as a literal and getRequiredString rejects only a MISSING key, so "" produces a schedule that
    // fires on time and hands the agent nothing. This dialog is the only gate.
    it('refuses to save a schedule with a blank prompt', async () => {
        wrap(<AgentScheduleCard agentId="agent-1" channels={[]} />);

        await userEvent.click(screen.getByRole('button', {name: 'Add schedule'}));

        fireEvent.change(screen.getByLabelText('Name'), {target: {value: 'Daily digest'}});
        fireEvent.change(screen.getByLabelText('Time'), {target: {value: '09:00'}});
        fireEvent.change(screen.getByLabelText('Prompt'), {target: {value: '   '}});
        await userEvent.click(screen.getByRole('button', {name: 'Add'}));

        expect(await screen.findByText('Prompt is required')).toBeInTheDocument();
        expect(addAgentChannelMutate).not.toHaveBeenCalled();
    });

    // The whole parameters map is sent back on every edit, so an untouched sibling field must survive. This
    // channel's stored parameters carry no frequencyKind (a row written before the picker existed), so it
    // opens as CUSTOM_CRON showing its stored expression.
    it('preserves the other parameters when one field is edited', async () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        wrap(<AgentScheduleCard agentId="agent-1" channels={[scheduleChannel] as any} />);

        await userEvent.click(screen.getByRole('button', {name: /edit/i}));

        fireEvent.change(screen.getByLabelText('Cron expression'), {target: {value: '0 8 * * *'}});
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(updateAgentChannelMutate).toHaveBeenCalledWith({
            input: {
                id: 'schedule-1',
                parameters: {
                    cronExpression: '0 8 * * *',
                    expression: '0 8 * * *',
                    frequencyKind: 'CUSTOM_CRON',
                    name: '',
                    prompt: 'Summarize yesterday',
                    timezone: 'UTC',
                },
            },
        });
    });

    it('deletes a schedule channel', async () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        wrap(<AgentScheduleCard agentId="agent-1" channels={[scheduleChannel] as any} />);

        const scheduleRow = screen.getByRole('listitem', {name: 'Schedule'});

        await userEvent.click(within(scheduleRow).getByRole('button', {name: /delete/i}));

        expect(deleteAgentChannelMutate).toHaveBeenCalledWith({id: 'schedule-1'});
    });

    it('ignores channels that are not schedules', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        wrap(<AgentScheduleCard agentId="agent-1" channels={[scheduleChannel, slackChannel] as any} />);

        expect(screen.getAllByRole('listitem')).toHaveLength(1);
    });
});
