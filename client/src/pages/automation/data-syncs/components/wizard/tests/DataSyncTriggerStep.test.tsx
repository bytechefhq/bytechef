import DataSyncTriggerStep from '@/pages/automation/data-syncs/components/wizard/DataSyncTriggerStep';
import {DataSyncTriggerType} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {afterEach, beforeAll, describe, expect, it, vi} from 'vitest';

const {mutateMock} = vi.hoisted(() => ({mutateMock: vi.fn()}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<typeof import('@/shared/middleware/graphql')>('@/shared/middleware/graphql');

    return {
        ...actual,
        useUpdateDataSyncTriggerMutation: () => ({isPending: false, mutate: mutateMock}),
    };
});

beforeAll(() => {
    if (!Element.prototype.hasPointerCapture) {
        Element.prototype.hasPointerCapture = () => false;
    }

    if (!Element.prototype.setPointerCapture) {
        Element.prototype.setPointerCapture = () => {};
    }

    if (!Element.prototype.releasePointerCapture) {
        Element.prototype.releasePointerCapture = () => {};
    }
});

afterEach(() => {
    mutateMock.mockClear();
});

const renderStep = (triggerType: DataSyncTriggerType, triggerParameters: Record<string, unknown> | null = null) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <DataSyncTriggerStep dataSync={{id: '10', triggerParameters, triggerType} as never} />
        </QueryClientProvider>
    );

describe('DataSyncTriggerStep', () => {
    it('switches to manual immediately', async () => {
        const user = userEvent.setup();

        renderStep(DataSyncTriggerType.Schedule, {expression: '0 9 * * ?', frequencyKind: 'DAILY', timeOfDay: '09:00'});

        await user.click(screen.getByLabelText('Manual'));

        expect(mutateMock).toHaveBeenCalledWith({
            input: {id: '10', triggerParameters: null, triggerType: DataSyncTriggerType.Manual},
        });
    });

    it('saves a valid cadence with its cron expression and timezone', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        renderStep(DataSyncTriggerType.Manual);

        await user.click(screen.getByLabelText('Scheduled'));

        await user.click(screen.getByLabelText('Frequency'));
        await user.click(await screen.findByRole('option', {name: 'Hourly'}));

        await user.clear(screen.getByLabelText(/minute of hour/i));
        await user.type(screen.getByLabelText(/minute of hour/i), '15');

        await waitFor(
            () => {
                expect(mutateMock).toHaveBeenLastCalledWith({
                    input: {
                        id: '10',
                        triggerParameters: expect.objectContaining({
                            expression: '15 * * * ?',
                            frequencyKind: 'HOURLY',
                            minuteOfHour: '15',
                            timezone: 'UTC',
                        }),
                        triggerType: DataSyncTriggerType.Schedule,
                    },
                });
            },
            {timeout: 3000}
        );
    });

    it('restores the schedule when toggled back within the debounce window', async () => {
        const user = userEvent.setup();

        renderStep(DataSyncTriggerType.Schedule, {
            expression: '0 9 * * ?',
            frequencyKind: 'DAILY',
            timeOfDay: '09:00',
            timezone: 'UTC',
        });

        await user.click(screen.getByLabelText('Manual'));

        expect(mutateMock).toHaveBeenCalledWith({
            input: {id: '10', triggerParameters: null, triggerType: DataSyncTriggerType.Manual},
        });

        mutateMock.mockClear();

        await user.click(screen.getByLabelText('Scheduled'));

        await waitFor(
            () => {
                expect(mutateMock).toHaveBeenCalledWith({
                    input: {
                        id: '10',
                        triggerParameters: expect.objectContaining({
                            expression: '0 9 * * ?',
                            frequencyKind: 'DAILY',
                            timeOfDay: '09:00',
                            timezone: 'UTC',
                        }),
                        triggerType: DataSyncTriggerType.Schedule,
                    },
                });
            },
            {timeout: 3000}
        );
    });

    it('never persists a stored cadence that fails validation', async () => {
        renderStep(DataSyncTriggerType.Schedule, {frequencyKind: 'WEEKLY', timeOfDay: '09:00'});

        expect(screen.getByText('A day of the week is required.')).toBeInTheDocument();

        await new Promise((resolve) => setTimeout(resolve, 700));

        expect(mutateMock).not.toHaveBeenCalled();
    });
});
