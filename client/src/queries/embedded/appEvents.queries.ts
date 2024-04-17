/* eslint-disable sort-keys */
import {AppEventApi, AppEventModel} from '@/middleware/embedded/configuration';
import {useQuery} from '@tanstack/react-query';

export const AppEventKeys = {
    appEvents: ['appEvents'] as const,
};

export const useGetAppEventsQuery = () =>
    useQuery<AppEventModel[], Error>({
        queryKey: AppEventKeys.appEvents,
        queryFn: () => new AppEventApi().getAppEvents(),
    });
