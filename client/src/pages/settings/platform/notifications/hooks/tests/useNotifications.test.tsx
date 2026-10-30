import {NotificationEventTypeEnum} from '@/shared/middleware/platform/notification';
import {renderHook} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import useNotifications from '../useNotifications';

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
    useCreateNotificationMutation: vi.fn(() => ({mutate: vi.fn()})),
    useDeleteNotificationMutation: vi.fn(() => ({mutate: vi.fn()})),
    useUpdateNotificationMutation: vi.fn(() => ({mutate: vi.fn()})),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => true,
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({invalidateQueries: vi.fn()})),
}));

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
});
