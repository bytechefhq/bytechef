import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import TriggerEvents from '../TriggerEvents';

interface MutationOptionsI {
    onSuccess?: (data: unknown) => void;
}

const hoisted = vi.hoisted(() => ({
    copyToClipboardMock: vi.fn(),
    rotateMutateMock: vi.fn(),
    rotateOptions: undefined as MutationOptionsI | undefined,
    sendTestEventMutateMock: vi.fn(),
    sendTestEventOptions: undefined as MutationOptionsI | undefined,
    toastMock: vi.fn(),
    updateWebhookUrlMutateMock: vi.fn(),
    updateWebhookUrlOptions: undefined as MutationOptionsI | undefined,
    webhookSettingsQueryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useRotateTriggerSubscriptionWebhookSecretMutation: (options: MutationOptionsI) => {
        hoisted.rotateOptions = options;

        return {isPending: false, mutate: hoisted.rotateMutateMock};
    },
    useSendTriggerSubscriptionTestEventMutation: (options: MutationOptionsI) => {
        hoisted.sendTestEventOptions = options;

        return {isPending: false, mutate: hoisted.sendTestEventMutateMock};
    },
    useTriggerSubscriptionWebhookSettingsQuery: hoisted.webhookSettingsQueryMock,
    useUpdateTriggerSubscriptionWebhookUrlMutation: (options: MutationOptionsI) => {
        hoisted.updateWebhookUrlOptions = options;

        return {isPending: false, mutate: hoisted.updateWebhookUrlMutateMock};
    },
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 2}),
}));

vi.mock('@uidotdev/usehooks', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@uidotdev/usehooks')>()),
    useCopyToClipboard: () => [null, hoisted.copyToClipboardMock],
}));

vi.mock('sonner', async (importOriginal) => ({
    ...(await importOriginal<typeof import('sonner')>()),
    toast: hoisted.toastMock,
}));

const mockWebhookSettings = (webhookUrl?: string, signingSecret?: string) =>
    hoisted.webhookSettingsQueryMock.mockReturnValue({
        data: {triggerSubscriptionWebhookSettings: {signingSecret, webhookUrl}},
        error: null,
        isLoading: false,
    });

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('TriggerEvents', () => {
    it('renders empty state without webhook url', () => {
        mockWebhookSettings();

        render(<TriggerEvents />);

        expect(hoisted.webhookSettingsQueryMock).toHaveBeenCalledWith({environmentId: '2'});
        expect(
            screen.getByText('Set a webhook URL to receive trigger events for this environment.')
        ).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Send test event'})).not.toBeInTheDocument();
    });

    it('saves webhook url', async () => {
        mockWebhookSettings();

        render(<TriggerEvents />);

        await userEvent.type(screen.getByLabelText('Webhook URL'), 'https://example.com/hook');

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.updateWebhookUrlMutateMock).toHaveBeenCalledWith({
            environmentId: '2',
            webhookUrl: 'https://example.com/hook',
        });
    });

    it('clears the webhook url', async () => {
        mockWebhookSettings('https://example.com/hook', 'whsec_secret');

        render(<TriggerEvents />);

        await userEvent.clear(screen.getByLabelText('Webhook URL'));

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.updateWebhookUrlMutateMock).toHaveBeenCalledWith({
            environmentId: '2',
            webhookUrl: '',
        });
    });

    it('reveals and copies secret', async () => {
        mockWebhookSettings('https://example.com/hook', 'whsec_123');

        render(<TriggerEvents />);

        expect(screen.getByLabelText('Webhook URL')).toHaveValue('https://example.com/hook');
        expect(screen.queryByText('whsec_123')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Reveal'}));

        expect(screen.getByText('whsec_123')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Hide'}));

        expect(screen.queryByText('whsec_123')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Copy'}));

        expect(hoisted.copyToClipboardMock).toHaveBeenCalledWith('whsec_123');
    });

    it('rotates the secret after confirmation', async () => {
        mockWebhookSettings('https://example.com/hook', 'whsec_123');

        render(<TriggerEvents />);

        await userEvent.click(screen.getByRole('button', {name: 'Rotate'}));

        expect(
            screen.getByText(
                'New events are signed with the new secret right away. Events already waiting for delivery keep the old one.'
            )
        ).toBeInTheDocument();
        expect(hoisted.rotateMutateMock).not.toHaveBeenCalled();

        await userEvent.click(screen.getByRole('button', {name: 'Rotate secret'}));

        expect(hoisted.rotateMutateMock).toHaveBeenCalledWith({environmentId: '2'});
    });

    it('shows test event result', async () => {
        mockWebhookSettings('https://example.com/hook', 'whsec_123');

        hoisted.sendTestEventMutateMock.mockImplementationOnce(() =>
            hoisted.sendTestEventOptions?.onSuccess?.({
                sendTriggerSubscriptionTestEvent: {error: null, statusCode: 204, success: true},
            })
        );

        render(<TriggerEvents />);

        await userEvent.click(screen.getByRole('button', {name: 'Send test event'}));

        expect(hoisted.sendTestEventMutateMock).toHaveBeenCalledWith({environmentId: '2'});

        await waitFor(() => {
            expect(screen.getByText('Delivered (HTTP 204)')).toBeInTheDocument();
        });

        hoisted.sendTestEventMutateMock.mockImplementationOnce(() =>
            hoisted.sendTestEventOptions?.onSuccess?.({
                sendTriggerSubscriptionTestEvent: {error: 'Connection refused', statusCode: null, success: false},
            })
        );

        await userEvent.click(screen.getByRole('button', {name: 'Send test event'}));

        await waitFor(() => {
            expect(screen.getByText('Connection refused')).toBeInTheDocument();
        });

        expect(screen.queryByText('Delivered (HTTP 204)')).not.toBeInTheDocument();
    });
});
