import {
    AiProviderApi,
    EnableAiProviderRequest,
    UpdateAiProviderOperationRequest,
} from '@/ee/shared/middleware/platform/configuration';
import {useMutation} from '@tanstack/react-query';

interface UpdateAiProviderMutationProps {
    onError?: (error: Error, variables: UpdateAiProviderOperationRequest) => void;
    onSuccess?: (result: void, variables: UpdateAiProviderOperationRequest) => void;
}

export const useUpdateAiProviderMutation = (mutationProps?: UpdateAiProviderMutationProps) =>
    useMutation<void, Error, UpdateAiProviderOperationRequest>({
        mutationFn: (requestParameters: UpdateAiProviderOperationRequest) => {
            return new AiProviderApi().updateAiProvider(requestParameters);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DeleteAiProviderMutationProps {
    onError?: (error: Error, variables: number) => void;
    onSuccess?: (result: void, variables: number) => void;
}

export const useDeleteAiProviderMutation = (mutationProps?: DeleteAiProviderMutationProps) =>
    useMutation<void, Error, number>({
        mutationFn: (id: number) => {
            return new AiProviderApi().deleteAiProvider({
                id,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface EnableAiProviderMutationProps {
    onError?: (error: Error, variables: EnableAiProviderRequest) => void;
    onSuccess?: (result: void, variables: EnableAiProviderRequest) => void;
}

export const useEnableAiProviderMutation = (mutationProps?: EnableAiProviderMutationProps) =>
    useMutation<void, Error, EnableAiProviderRequest>({
        mutationFn: (requestParameters: EnableAiProviderRequest) => {
            return new AiProviderApi().enableAiProvider(requestParameters);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
