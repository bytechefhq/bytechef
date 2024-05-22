import {AppEventApi, AppEventModel} from '@/shared/middleware/embedded/configuration';
import {useMutation} from '@tanstack/react-query';

interface CreateAppEventMutationProps {
    onError?: (error: Error, variables: AppEventModel) => void;
    onSuccess?: (result: AppEventModel, variables: AppEventModel) => void;
}

export const useCreateAppEventMutation = (mutationProps?: CreateAppEventMutationProps) =>
    useMutation<AppEventModel, Error, AppEventModel>({
        mutationFn: (appEventModel: AppEventModel) => {
            return new AppEventApi().createAppEvent({
                appEventModel,
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
    onError?: (error: Error, variables: AppEventModel) => void;
    onSuccess?: (result: AppEventModel, variables: AppEventModel) => void;
}

export const useUpdateAppEventMutation = (mutationProps?: UpdateAppEventMutationProps) =>
    useMutation<AppEventModel, Error, AppEventModel>({
        mutationFn: (appEventModel: AppEventModel) => {
            return new AppEventApi().updateAppEvent({
                appEventModel,
                id: appEventModel.id!,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
