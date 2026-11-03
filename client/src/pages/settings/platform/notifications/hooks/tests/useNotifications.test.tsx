import {NotificationEventTypeEnum, NotificationTypeEnum} from '@/shared/middleware/platform/notification';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useNotifications, {notificationFormSchema} from '../useNotifications';

const {
    createNotificationMutate,
    invalidateQueries,
    mutationOptions,
    notificationsQueryState,
    regenerateMutate,
    testNotificationMutateAsync,
    updateNotificationMutate,
} = vi.hoisted(() => ({
    createNotificationMutate: vi.fn(),
    invalidateQueries: vi.fn(),
    mutationOptions: {} as Record<string, ((result: unknown, variables?: number) => void) | undefined>,
    notificationsQueryState: {data: [] as unknown[]},
    regenerateMutate: vi.fn(),
    testNotificationMutateAsync: vi.fn(),
    updateNotificationMutate: vi.fn(),
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
    useGetNotificationsQuery: vi.fn(() => ({data: notificationsQueryState.data, error: null, isLoading: false})),
}));

vi.mock('@/shared/mutations/platform/notifications.mutations', () => ({
    useCreateNotificationMutation: vi.fn((options?: {onSuccess?: (result: unknown) => void}) => {
        mutationOptions.create = options?.onSuccess;

        return {mutate: createNotificationMutate};
    }),
    useDeleteNotificationMutation: vi.fn(() => ({mutate: vi.fn()})),
    useRegenerateNotificationSigningSecretMutation: vi.fn(
        (options?: {onSuccess?: (result: unknown, variables?: number) => void}) => {
            mutationOptions.regenerate = options?.onSuccess;

            return {mutate: regenerateMutate};
        }
    ),
    useTestNotificationMutation: vi.fn(() => ({isPending: false, mutateAsync: testNotificationMutateAsync})),
    useUpdateNotificationMutation: vi.fn((options?: {onSuccess?: (result: unknown) => void}) => {
        mutationOptions.update = options?.onSuccess;

        return {mutate: updateNotificationMutate};
    }),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => true,
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({invalidateQueries})),
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

describe('useNotifications signing secret state', () => {
    it('marks the selected notification as having a signing secret after generating one', () => {
        const {result} = renderHook(() => useNotifications());

        act(() => {
            result.current.openEditDialog({hasSigningSecret: false, id: 7, name: 'n'} as never);
        });

        expect(result.current.selectedNotification?.hasSigningSecret).toBe(false);

        act(() => {
            mutationOptions.regenerate?.({signingSecret: 'whsec_new'}, 7);
        });

        expect(result.current.selectedNotification?.hasSigningSecret).toBe(true);
    });
});

describe('useNotifications saving an existing notification', () => {
    const storedNotification = {
        hasSigningSecret: true,
        id: 7,
        name: 'Failures',
        notificationEvents: [{id: 1}],
        settings: {webhook: 'https://example.com/ingest'},
        type: NotificationTypeEnum.Webhook,
        version: 1,
    };

    beforeEach(() => {
        notificationsQueryState.data = [storedNotification];

        updateNotificationMutate.mockClear();
    });

    it('sends the refreshed version after a regenerate and keeps unsaved edits', () => {
        const {rerender, result} = renderHook(() => useNotifications());

        act(() => {
            result.current.openEditDialog(storedNotification as never);
        });

        act(() => {
            result.current.form.setValue('name', 'Edited before regenerate');
        });

        act(() => {
            mutationOptions.regenerate?.({signingSecret: 'whsec_new'}, 7);
        });

        notificationsQueryState.data = [{...storedNotification, version: 2}];

        rerender();

        expect(result.current.form.getValues('name')).toBe('Edited before regenerate');

        act(() => {
            result.current.saveNotification();
        });

        expect(updateNotificationMutate).toHaveBeenCalledWith(
            expect.objectContaining({id: 7, name: 'Edited before regenerate', version: 2})
        );
    });

    it('keeps the dialog open with the values when the update fails', () => {
        const {result} = renderHook(() => useNotifications());

        act(() => {
            result.current.openEditDialog(storedNotification as never);
        });

        act(() => {
            result.current.form.setValue('name', 'Unsaved name');
        });

        act(() => {
            result.current.saveNotification();
        });

        expect(updateNotificationMutate).toHaveBeenCalled();
        expect(result.current.isEditDialogOpen).toBe(true);
        expect(result.current.form.getValues('name')).toBe('Unsaved name');
        expect(result.current.selectedNotification?.id).toBe(7);
    });

    it('closes the dialog and shows a signing secret generated by the update', () => {
        const {result} = renderHook(() => useNotifications());

        act(() => {
            result.current.openEditDialog(storedNotification as never);
        });

        act(() => {
            mutationOptions.update?.({id: 7, signingSecret: 'whsec_from_update'});
        });

        expect(result.current.isEditDialogOpen).toBe(false);
        expect(result.current.signingSecret).toBe('whsec_from_update');
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

    it('requires a value unless it is the masked placeholder', () => {
        expect(headerMessages([{name: 'X-A', value: ''}])).toContain('Enter a value');
        expect(headerMessages([{name: 'X-A', value: '********'}])).toEqual([]);
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

describe('useNotifications testing a notification', () => {
    const storedNotification = {
        hasSigningSecret: true,
        id: 7,
        name: 'Failures',
        notificationEvents: [{id: 1}],
        settings: {webhook: 'https://example.com/ingest', webhookHeaders: [{name: 'Authorization', value: '********'}]},
        type: NotificationTypeEnum.Webhook,
        version: 3,
    };

    beforeEach(() => {
        notificationsQueryState.data = [{...storedNotification, version: 4}];

        invalidateQueries.mockClear();
        testNotificationMutateAsync.mockReset();
        testNotificationMutateAsync.mockResolvedValue({signed: true, success: true});
    });

    it('sends the same payload as saving, without invalidating or closing', async () => {
        const {result} = renderHook(() => useNotifications());

        act(() => {
            result.current.openEditDialog(storedNotification as never);
        });

        act(() => {
            result.current.form.setValue('settings', {
                webhook: '  https://example.com/edited  ',
                webhookHeaders: [{name: 'Authorization', value: '********'}],
            });
        });

        await act(async () => {
            await result.current.testNotification();
        });

        act(() => {
            result.current.saveNotification();
        });

        expect(testNotificationMutateAsync).toHaveBeenCalledWith(
            expect.objectContaining({
                id: 7,
                settings: expect.objectContaining({webhook: 'https://example.com/edited'}),
                version: 4,
            })
        );
        expect(testNotificationMutateAsync.mock.calls[0][0]).toEqual(updateNotificationMutate.mock.calls.at(-1)?.[0]);
        expect(invalidateQueries).not.toHaveBeenCalled();
        expect(result.current.isEditDialogOpen).toBe(true);
    });
});
