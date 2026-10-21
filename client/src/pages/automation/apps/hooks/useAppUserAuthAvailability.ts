import {AppUserAuthMode, useAppUserAuthAvailableQuery} from '@/shared/middleware/graphql';

export interface AppUserAuthAvailabilityI {
    defaultUserAuthMode: AppUserAuthMode;
    userAuthAvailabilityLoading: boolean;
    userAuthAvailable: boolean | undefined;
}

export default function useAppUserAuthAvailability(workspaceId: string, enabled: boolean): AppUserAuthAvailabilityI {
    const queryEnabled = enabled && !!workspaceId;

    const {data, isLoading} = useAppUserAuthAvailableQuery({workspaceId}, {enabled: queryEnabled});

    const userAuthAvailable = data?.appUserAuthAvailable;

    return {
        defaultUserAuthMode: userAuthAvailable === false ? AppUserAuthMode.None : AppUserAuthMode.Required,
        userAuthAvailabilityLoading: queryEnabled && isLoading,
        userAuthAvailable,
    };
}
