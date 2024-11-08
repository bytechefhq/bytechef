import {AppEvent, AppEventApi} from '@/shared/middleware/embedded/configuration';
import {useMutation} from '@tanstack/react-query';

interface CreateAppEventMutationProps {
    onError?: (error: Error, variables: AppEvent) => void;
    onSuccess?: (result: number, variables: AppEvent) => void;
}

export const useCreateAppEventMutation = (mutationProps?: CreateAppEventMutationProps) =>
    useMutation<number, Error, AppEvent>({
        mutationFn: (appEvent: AppEvent) => {
            return new AppEventApi().createAppEvent({
                appEvent,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DeleteAppEventMutationProps {
    onError?: (error: Error, variables: number) => void;
    onSuccess?: (result: void, variables: number) => void;
}

export const useDeleteAppEventMutation = (mutationProps?: DeleteAppEventMutationProps) =>
    useMutation<void, Error, number>({
        mutationFn: (id: number) => {
            return new AppEventApi().deleteAppEvent({
                id,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateAppEventMutationProps {
    onError?: (error: Error, variables: AppEvent) => void;
    onSuccess?: (result: void, variables: AppEvent) => void;
}

export const useUpdateAppEventMutation = (mutationProps?: UpdateAppEventMutationProps) =>
    useMutation<void, Error, AppEvent>({
        mutationFn: (appEvent: AppEvent) => {
            return new AppEventApi().updateAppEvent({
                appEvent,
                id: appEvent.id!,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
