import {
    ApiCollection,
    ApiCollectionApi,
    ApiCollectionVersionUpdate,
} from '@/ee/shared/middleware/automation/api-platform';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useMutation} from '@tanstack/react-query';

interface CreateApiCollectionMutationProps {
    onError?: (error: Error, variables: ApiCollection) => void;
    onSuccess?: (result: ApiCollection, variables: ApiCollection) => void;
}

export const useCreateApiCollectionMutation = (mutationProps?: CreateApiCollectionMutationProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    return useMutation<ApiCollection, Error, ApiCollection>({
        mutationFn: (apiCollection: ApiCollection) => {
            return new ApiCollectionApi().createApiCollection({
                apiCollection: {
                    ...apiCollection,
                    workspaceId: currentWorkspaceId!,
                },
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
};

interface DeleteApiCollectionMutationProps {
    onError?: (error: Error, variables: number) => void;
    onSuccess?: (result: void, variables: number) => void;
}

export const useDeleteApiCollectionMutation = (mutationProps?: DeleteApiCollectionMutationProps) =>
    useMutation<void, Error, number>({
        mutationFn: (id: number) => {
            return new ApiCollectionApi().deleteApiCollection({
                id,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateApiCollectionMutationProps {
    onError?: (error: Error, variables: ApiCollection) => void;
    onSuccess?: (result: ApiCollection, variables: ApiCollection) => void;
}

export const useUpdateApiCollectionMutation = (mutationProps?: UpdateApiCollectionMutationProps) =>
    useMutation<ApiCollection, Error, ApiCollection>({
        mutationFn: (apiCollection: ApiCollection) => {
            return new ApiCollectionApi().updateApiCollection({
                apiCollection,
                id: apiCollection.id!,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

export interface UpdateApiCollectionProjectVersionVariablesI {
    id: number;
    projectVersion: number;
    removeMissingEndpoints: boolean;
}

interface UpdateApiCollectionProjectVersionMutationProps {
    onError?: (error: Error, variables: UpdateApiCollectionProjectVersionVariablesI) => void;
    onSuccess?: (result: ApiCollectionVersionUpdate, variables: UpdateApiCollectionProjectVersionVariablesI) => void;
}

export const useUpdateApiCollectionProjectVersionMutation = (
    mutationProps?: UpdateApiCollectionProjectVersionMutationProps
) =>
    useMutation<ApiCollectionVersionUpdate, Error, UpdateApiCollectionProjectVersionVariablesI>({
        mutationFn: ({id, projectVersion, removeMissingEndpoints}: UpdateApiCollectionProjectVersionVariablesI) => {
            return new ApiCollectionApi().updateApiCollectionProjectVersion({
                id,
                updateApiCollectionProjectVersionRequest: {
                    projectVersion,
                    removeMissingEndpoints,
                },
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
