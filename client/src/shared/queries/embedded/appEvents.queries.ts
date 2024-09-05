/* eslint-disable sort-keys */
import {AppEvent, AppEventApi} from '@/shared/middleware/embedded/configuration';
import {useQuery} from '@tanstack/react-query';

export const AppEventKeys = {
    appEvents: ['appEvents'] as const,
};

export const useGetAppEventsQuery = () =>
    useQuery<AppEvent[], Error>({
        queryKey: AppEventKeys.appEvents,
        queryFn: () => new AppEventApi().getAppEvents(),
    });
