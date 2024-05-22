import {
    CreateIntegrationWorkflowRequest,
    IntegrationApi,
    IntegrationModel,
    IntegrationTagApi,
    PublishIntegrationRequest,
    UpdateIntegrationTagsRequest,
    WorkflowModel,
} from '@/shared/middleware/embedded/configuration';
import {useMutation} from '@tanstack/react-query';

interface CreateIntegrationMutationProps {
    onSuccess?: (result: IntegrationModel, variables: IntegrationModel) => void;
    onError?: (error: Error, variables: IntegrationModel) => void;
}

export const useCreateIntegrationMutation = (mutationProps?: CreateIntegrationMutationProps) =>
    useMutation({
        mutationFn: (integration: IntegrationModel) => {
            return new IntegrationApi().createIntegration({
                integrationModel: integration,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateIntegrationMutationProps {
    onSuccess?: (result: IntegrationModel, variables: IntegrationModel) => void;
    onError?: (error: Error, variables: IntegrationModel) => void;
}

export const useUpdateIntegrationMutation = (mutationProps?: UpdateIntegrationMutationProps) =>
    useMutation({
        mutationFn: (integration: IntegrationModel) => {
            return new IntegrationApi().updateIntegration({
                id: integration.id!,
                integrationModel: integration,
            });
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface CreateIntegrationWorkflowMutationProps {
    onSuccess?: (result: WorkflowModel, variables: CreateIntegrationWorkflowRequest) => void;
    onError?: (error: Error, variables: CreateIntegrationWorkflowRequest) => void;
}

export const useCreateIntegrationWorkflowMutation = (mutationProps?: CreateIntegrationWorkflowMutationProps) =>
    useMutation({
        mutationFn: (request: CreateIntegrationWorkflowRequest) => {
            return new IntegrationApi().createIntegrationWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DeleteIntegrationMutationProps {
    onSuccess?: (result: void, variables: number) => void;
    onError?: (error: Error, variables: number) => void;
}

export const useDeleteIntegrationMutation = (mutationProps?: DeleteIntegrationMutationProps) =>
    useMutation({
        mutationFn: (id: number) => {
            return new IntegrationApi().deleteIntegration({id});
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface PublishIntegrationMutationProps {
    onSuccess?: (result: void, variables: PublishIntegrationRequest) => void;
    onError?: (error: Error, variables: PublishIntegrationRequest) => void;
}

export const usePublishIntegrationMutation = (mutationProps?: PublishIntegrationMutationProps) =>
    useMutation({
        mutationFn: (request: PublishIntegrationRequest) => {
            return new IntegrationApi().publishIntegration(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateIntegrationTagsMutationProps {
    onSuccess?: (result: void, variables: UpdateIntegrationTagsRequest) => void;
    onError?: (error: Error, variables: UpdateIntegrationTagsRequest) => void;
}

export const useUpdateIntegrationTagsMutation = (mutationProps?: UpdateIntegrationTagsMutationProps) =>
    useMutation({
        mutationFn: (request: UpdateIntegrationTagsRequest) => {
            return new IntegrationTagApi().updateIntegrationTags(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
