import {
    CreateProjectWorkflow200Response,
    CreateProjectWorkflowRequest,
    DeleteWorkflowRequest,
    DuplicateWorkflow200Response,
    DuplicateWorkflowRequest,
    UpdateWorkflowRequest,
    Workflow,
    WorkflowApi,
} from '@/shared/middleware/automation/configuration';
import {useMutation} from '@tanstack/react-query';

interface CreateProjectWorkflowMutationProps {
    onSuccess?: (result: CreateProjectWorkflow200Response, variables: CreateProjectWorkflowRequest) => void;
    onError?: (error: Error, variables: CreateProjectWorkflowRequest) => void;
}

export const useCreateProjectWorkflowMutation = (mutationProps?: CreateProjectWorkflowMutationProps) => {
    return useMutation({
        mutationFn: (request: CreateProjectWorkflowRequest) => {
            return new WorkflowApi().createProjectWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
};

export interface GenerateProjectWorkflowRequestI {
    environmentId: number;
    id: number;
    prompt: string;
}

interface GenerateProjectWorkflowMutationProps {
    onSuccess?: (result: CreateProjectWorkflow200Response, variables: GenerateProjectWorkflowRequestI) => void;
    onError?: (error: Error, variables: GenerateProjectWorkflowRequestI) => void;
}

export const useGenerateProjectWorkflowMutation = (mutationProps?: GenerateProjectWorkflowMutationProps) =>
    useMutation({
        mutationFn: async (request: GenerateProjectWorkflowRequestI): Promise<CreateProjectWorkflow200Response> => {
            const response = await fetch(`/api/automation/internal/projects/${request.id}/workflows/generate`, {
                body: JSON.stringify({environmentId: request.environmentId, prompt: request.prompt}),
                headers: {'Content-Type': 'application/json'},
                method: 'POST',
            });

            if (!response.ok) {
                throw new Error(`Failed to generate workflow: ${response.status} ${response.statusText}`);
            }

            return response.json();
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DeleteWorkflowMutationProps {
    onSuccess?: (result: void, variables: DeleteWorkflowRequest) => void;
    onError?: (error: Error, variables: DeleteWorkflowRequest) => void;
}

export const useDeleteWorkflowMutation = (mutationProps?: DeleteWorkflowMutationProps) =>
    useMutation({
        mutationFn: (request: DeleteWorkflowRequest) => {
            return new WorkflowApi().deleteWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface DuplicateWorkflowMutationProps {
    onSuccess?: (result: DuplicateWorkflow200Response, variables: DuplicateWorkflowRequest) => void;
    onError?: (error: Error, variables: DuplicateWorkflowRequest) => void;
}

export const useDuplicateWorkflowMutation = (mutationProps?: DuplicateWorkflowMutationProps) =>
    useMutation({
        mutationFn: (request: DuplicateWorkflowRequest) => {
            return new WorkflowApi().duplicateWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UpdateWorkflowMutationProps {
    onSuccess?: (result: Workflow, variables: UpdateWorkflowRequest) => void;
    onError?: (error: Error, variables: UpdateWorkflowRequest) => void;
}

export const useUpdateWorkflowMutation = (mutationProps?: UpdateWorkflowMutationProps) =>
    useMutation({
        mutationFn: (request: UpdateWorkflowRequest) => {
            return new WorkflowApi().updateWorkflow(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
