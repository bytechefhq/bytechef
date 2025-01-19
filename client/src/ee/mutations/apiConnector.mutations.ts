import {
    ApiConnector,
    ApiConnectorApi,
    EnableApiConnectorRequest,
    ImportOpenApiSpecificationRequest,
} from '@/ee/shared/middleware/platform/api-connector';
import {useMutation} from '@tanstack/react-query';

interface CreateApiConnectorMutationProps {
    onError?: (error: Error, variables: ApiConnector) => void;
    onSuccess?: (result: ApiConnector, variables: ApiConnector) => void;
}

export const useCreateApiConnectorMutation = (mutationProps?: CreateApiConnectorMutationProps) =>
    useMutation<ApiConnector, Error, ApiConnector>({
        mutationFn: (apiConnector: ApiConnector) => {
            return new ApiConnectorApi().createApiConnector({
                apiConnector,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DeleteApiConnectorMutationProps {
    onError?: (error: Error, variables: number) => void;
    onSuccess?: (result: void, variables: number) => void;
}

export const useDeleteApiConnectorMutation = (mutationProps?: DeleteApiConnectorMutationProps) =>
    useMutation<void, Error, number>({
        mutationFn: (id: number) => {
            return new ApiConnectorApi().deleteApiConnector({
                id,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface ImportOpenApiSpecificationRequestApiConnectorMutationProps {
    onError?: (error: Error, variables: ImportOpenApiSpecificationRequest) => void;
    onSuccess?: (result: ApiConnector, variables: ImportOpenApiSpecificationRequest) => void;
}

interface EnableApiConnectorMutationProps {
    onSuccess?: (result: void, variables: EnableApiConnectorRequest) => void;
    onError?: (error: Error, variables: EnableApiConnectorRequest) => void;
}

export const useEnableApiConnectorMutation = (mutationProps: EnableApiConnectorMutationProps) =>
    useMutation({
        mutationFn: (request: EnableApiConnectorRequest) => {
            return new ApiConnectorApi().enableApiConnector(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

export const useImportOpenApiSpecificationMutation = (
    mutationProps?: ImportOpenApiSpecificationRequestApiConnectorMutationProps
) =>
    useMutation<ApiConnector, Error, ImportOpenApiSpecificationRequest>({
        mutationFn: (importOpenApiSpecificationRequest: ImportOpenApiSpecificationRequest) => {
            return new ApiConnectorApi().importOpenApiSpecification({
                importOpenApiSpecificationRequest,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateApiConnectorMutationProps {
    onError?: (error: Error, variables: ApiConnector) => void;
    onSuccess?: (result: ApiConnector, variables: ApiConnector) => void;
}

export const useUpdateApiConnectorMutation = (mutationProps?: UpdateApiConnectorMutationProps) =>
    useMutation<ApiConnector, Error, ApiConnector>({
        mutationFn: (apiConnector: ApiConnector) => {
            return new ApiConnectorApi().updateApiConnector({
                apiConnector,
                id: apiConnector.id!,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
