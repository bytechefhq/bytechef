import {NotificationEventTypeEnum, NotificationTypeEnum} from '@/shared/middleware/platform/notification';
import {act, renderHook} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import useNotifications, {notificationFormSchema} from '../useNotifications';

const {createNotificationMutate} = vi.hoisted(() => ({
    createNotificationMutate: vi.fn(),
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
    useCreateNotificationMutation: vi.fn(() => ({mutate: createNotificationMutate})),
    useDeleteNotificationMutation: vi.fn(() => ({mutate: vi.fn()})),
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

function slackWebhookUrlIssues(settings: Record<string, unknown>, type: 'EMAIL' | 'SLACK' | 'WEBHOOK' = 'SLACK') {
    const result = notificationFormSchema.safeParse({...baseValues, settings, type});

    if (result.success) {
        return [];
    }

    return result.error.issues
        .filter((issue) => issue.path.join('.') === 'settings.slackWebhookUrl')
        .map((issue) => issue.message);
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
});
