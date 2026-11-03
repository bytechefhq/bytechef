import {NotificationEventTypeEnum, NotificationTypeEnum} from '@/shared/middleware/platform/notification';
import {act, renderHook} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import useNotifications, {notificationFormSchema} from '../useNotifications';

const {createNotificationMutate, mutationOptions, regenerateMutate} = vi.hoisted(() => ({
    createNotificationMutate: vi.fn(),
    mutationOptions: {} as Record<string, ((result: unknown) => void) | undefined>,
    regenerateMutate: vi.fn(),
}));

vi.mock('@/shared/queries/platform/notificationEvents.queries', () => ({
    useGetNotificationEventsQuery: vi.fn(() => ({
        data: [
            {id: 1, type: 'JOB_CANCELLED'},
            {id: 2, type: 'JOB_CREATED'},
            {id: 3, type: 'JOB_COMPLETED'},
            {id: 4, type: 'JOB_FAILED'},
            {id: 5, type: 'JOB_STARTED'},
            {id: 6, type: 'JOB_STOPPED'},
        ],
        isLoading: false,
    })),
}));

vi.mock('@/shared/queries/platform/notifications.queries', () => ({
    NotificationKeys: ['notifications'],
    useGetNotificationsQuery: vi.fn(() => ({data: [], error: null, isLoading: false})),
}));

vi.mock('@/shared/mutations/platform/notifications.mutations', () => ({
    useCreateNotificationMutation: vi.fn((options?: {onSuccess?: (result: unknown) => void}) => {
        mutationOptions.create = options?.onSuccess;

        return {mutate: createNotificationMutate};
    }),
    useDeleteNotificationMutation: vi.fn(() => ({mutate: vi.fn()})),
    useRegenerateNotificationSigningSecretMutation: vi.fn((options?: {onSuccess?: (result: unknown) => void}) => {
        mutationOptions.regenerate = options?.onSuccess;

        return {mutate: regenerateMutate};
    }),
    useUpdateNotificationMutation: vi.fn(() => ({mutate: vi.fn()})),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => true,
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({invalidateQueries: vi.fn()})),
}));

const baseValues = {
    name: 'Failures',
    notificationEventIds: ['1'],
};

function settingIssues(
    settings: Record<string, unknown>,
    type: 'EMAIL' | 'SLACK' | 'WEBHOOK',
    settingKey: 'slackWebhookUrl' | 'webhook'
) {
    const result = notificationFormSchema.safeParse({...baseValues, settings, type});

    if (result.success) {
        return [];
    }

    return result.error.issues
        .filter((issue) => issue.path.join('.') === `settings.${settingKey}`)
        .map((issue) => issue.message);
}

function slackWebhookUrlIssues(settings: Record<string, unknown>, type: 'EMAIL' | 'SLACK' | 'WEBHOOK' = 'SLACK') {
    return settingIssues(settings, type, 'slackWebhookUrl');
}

function webhookUrlIssues(settings: Record<string, unknown>, type: 'EMAIL' | 'SLACK' | 'WEBHOOK' = 'WEBHOOK') {
    return settingIssues(settings, type, 'webhook');
}

describe('useNotifications', () => {
    it('does not offer the job cancelled event', () => {
        const {result} = renderHook(() => useNotifications());

        const notificationEventTypes = result.current.notificationEvents?.map(
            (notificationEvent) => notificationEvent.type
        );

        expect(notificationEventTypes).not.toContain(NotificationEventTypeEnum.JobCancelled);
    });

    it('offers every other job event, including job stopped', () => {
        const {result} = renderHook(() => useNotifications());

        const notificationEventTypes = result.current.notificationEvents?.map(
            (notificationEvent) => notificationEvent.type
        );

        expect(notificationEventTypes).toEqual([
            NotificationEventTypeEnum.JobCreated,
            NotificationEventTypeEnum.JobCompleted,
            NotificationEventTypeEnum.JobFailed,
            NotificationEventTypeEnum.JobStarted,
            NotificationEventTypeEnum.JobStopped,
        ]);
    });

    it('saves a Slack webhook URL without surrounding whitespace', () => {
        const {result} = renderHook(() => useNotifications());

        act(() => {
            result.current.form.reset({
                name: 'Failures',
                notificationEventIds: ['1'],
                settings: {slackWebhookUrl: '  https://hooks.slack.com/services/T/B/X  '},
                type: NotificationTypeEnum.Slack,
            });
        });

        act(() => {
            result.current.saveNotification();
        });

        expect(createNotificationMutate).toHaveBeenCalledWith(
            expect.objectContaining({settings: {slackWebhookUrl: 'https://hooks.slack.com/services/T/B/X'}})
        );
    });
    it('saves a Webhook URL without surrounding whitespace', () => {
        const {result} = renderHook(() => useNotifications());

        act(() => {
            result.current.form.reset({
                name: 'Failures',
                notificationEventIds: ['1'],
                settings: {webhook: '  https://example.com/ingest  '},
                type: NotificationTypeEnum.Webhook,
            });
        });

        act(() => {
            result.current.saveNotification();
        });

        expect(createNotificationMutate).toHaveBeenCalledWith(
            expect.objectContaining({settings: {webhook: 'https://example.com/ingest'}})
        );
    });
});

describe('useNotifications signing secret', () => {
    it('opens the signing secret dialog after creating a webhook', () => {
        const {result} = renderHook(() => useNotifications());

        expect(result.current.signingSecret).toBeUndefined();

        act(() => {
            mutationOptions.create?.({id: 1, signingSecret: 'whsec_created'});
        });

        expect(result.current.signingSecret).toBe('whsec_created');

        act(() => {
            result.current.closeSigningSecretDialog();
        });

        expect(result.current.signingSecret).toBeUndefined();
    });

    it('does not open the signing secret dialog when none is returned', () => {
        const {result} = renderHook(() => useNotifications());

        act(() => {
            mutationOptions.create?.({id: 1});
        });

        expect(result.current.signingSecret).toBeUndefined();
    });

    it('regenerates the signing secret', () => {
        const {result} = renderHook(() => useNotifications());

        act(() => {
            result.current.regenerateSigningSecret(7);
        });

        expect(regenerateMutate).toHaveBeenCalledWith(7);

        act(() => {
            mutationOptions.regenerate?.({signingSecret: 'whsec_new'});
        });

        expect(result.current.signingSecret).toBe('whsec_new');
    });
});

describe('notificationFormSchema header validation', () => {
    function headerMessages(webhookHeaders: unknown) {
        const result = notificationFormSchema.safeParse({
            ...baseValues,
            settings: {webhook: 'https://example.com/ingest', webhookHeaders},
            type: 'WEBHOOK',
        });

        return result.success ? [] : result.error.issues.map((issue) => issue.message);
    }

    it('accepts valid headers', () => {
        expect(headerMessages([{name: 'X-Api-Key', value: '********'}])).toEqual([]);
    });

    it('rejects more than 20 headers', () => {
        const headers = Array.from({length: 21}, (_, index) => ({name: `X-H${index}`, value: 'v'}));

        expect(headerMessages(headers)).toContain('At most 20 headers are allowed.');
    });

    it('rejects reserved, invalid and duplicate names', () => {
        expect(headerMessages([{name: 'content-type', value: 'v'}])).toContain('This header name is reserved.');
        expect(headerMessages([{name: 'bad name', value: 'v'}])).toContain('Enter a valid header name.');
        expect(
            headerMessages([
                {name: 'X-A', value: 'v'},
                {name: 'x-a', value: 'v'},
            ])
        ).toContain('Header names must be unique.');
    });

    it('rejects a value longer than 4096 characters', () => {
        expect(headerMessages([{name: 'X-A', value: 'v'.repeat(4097)}])).toContain(
            'Header value cannot be longer than 4096 characters.'
        );
    });

    it('does not validate headers for a Slack notification', () => {
        const result = notificationFormSchema.safeParse({
            ...baseValues,
            settings: {slackWebhookUrl: '********'},
            type: 'SLACK',
        });

        expect(result.success).toBe(true);
    });
});

describe('notificationFormSchema', () => {
    it('requires a Slack webhook URL for a Slack notification', () => {
        expect(slackWebhookUrlIssues({})).toEqual(['Slack webhook URL is required.']);
        expect(slackWebhookUrlIssues({slackWebhookUrl: '   '})).toEqual(['Slack webhook URL is required.']);
    });

    it('rejects a Slack webhook URL that is not a URL', () => {
        expect(slackWebhookUrlIssues({slackWebhookUrl: 'not a url'})).toEqual(['Enter a valid Slack webhook URL.']);
    });

    it('accepts a valid Slack webhook URL', () => {
        const result = notificationFormSchema.safeParse({
            ...baseValues,
            settings: {slackWebhookUrl: 'https://hooks.slack.com/services/T/B/X'},
            type: 'SLACK',
        });

        expect(result.success).toBe(true);
    });

    it('does not require a Slack webhook URL for other notification types', () => {
        expect(slackWebhookUrlIssues({email: 'ops@example.com'}, 'EMAIL')).toEqual([]);
        expect(slackWebhookUrlIssues({webhook: 'https://example.com/hook'}, 'WEBHOOK')).toEqual([]);
    });

    it('requires a Webhook URL for a Webhook notification', () => {
        expect(webhookUrlIssues({})).toEqual(['Webhook URL is required.']);
        expect(webhookUrlIssues({webhook: '   '})).toEqual(['Webhook URL is required.']);
    });

    it('rejects a Webhook URL that is not an http or https URL', () => {
        expect(webhookUrlIssues({webhook: 'not a url'})).toEqual(['Enter a valid http or https URL.']);
        expect(webhookUrlIssues({webhook: 'ftp://example.com/hook'})).toEqual(['Enter a valid http or https URL.']);
    });

    it('accepts a valid Webhook URL', () => {
        expect(webhookUrlIssues({webhook: 'https://example.com/ingest'})).toEqual([]);
    });

    it('does not require a Webhook URL for other notification types', () => {
        expect(webhookUrlIssues({email: 'ops@example.com'}, 'EMAIL')).toEqual([]);
        expect(webhookUrlIssues({slackWebhookUrl: 'https://hooks.slack.com/services/T/B/X'}, 'SLACK')).toEqual([]);
    });
});
