import {CustomComponentApi, EnableCustomComponentRequest} from '@/ee/shared/middleware/platform/custom-component';
import {useMutation} from '@tanstack/react-query';

interface DeleteCustomComponentMutationProps {
    onError?: (error: Error, variables: number) => void;
    onSuccess?: (result: void, variables: number) => void;
}

export const useDeleteCustomComponentMutation = (mutationProps?: DeleteCustomComponentMutationProps) =>
    useMutation<void, Error, number>({
        mutationFn: (id: number) => {
            return new CustomComponentApi().deleteCustomComponent({
                id,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface EnableCustomComponentMutationProps {
    onSuccess?: (result: void, variables: EnableCustomComponentRequest) => void;
    onError?: (error: Error, variables: EnableCustomComponentRequest) => void;
}

export const useEnableCustomComponentMutation = (mutationProps: EnableCustomComponentMutationProps) =>
    useMutation({
        mutationFn: (request: EnableCustomComponentRequest) => {
            return new CustomComponentApi().enableCustomComponent(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
