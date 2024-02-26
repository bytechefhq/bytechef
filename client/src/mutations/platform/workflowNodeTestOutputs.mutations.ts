import {
    DeleteWorkflowNodeTestOutputRequest,
    SaveWorkflowNodeTestOutputRequest,
    UploadWorkflowNodeSampleOutputRequest,
    WorkflowNodeTestOutputApi,
    WorkflowNodeTestOutputModel,
} from '@/middleware/platform/configuration';
import {useMutation} from '@tanstack/react-query';

interface DeleteWorkflowNodeTestOutputProps {
    onSuccess?: (result: void, variables: DeleteWorkflowNodeTestOutputRequest) => void;
    onError?: (error: Error, variables: DeleteWorkflowNodeTestOutputRequest) => void;
}

export const useDeleteWorkflowNodeTestOutputMutation = (mutationProps?: DeleteWorkflowNodeTestOutputProps) =>
    useMutation({
        mutationFn: (request: DeleteWorkflowNodeTestOutputRequest) => {
            return new WorkflowNodeTestOutputApi().deleteWorkflowNodeTestOutput(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface SaveWorkflowNodeTestOutputProps {
    onSuccess?: (result: WorkflowNodeTestOutputModel, variables: SaveWorkflowNodeTestOutputRequest) => void;
    onError?: (error: Error, variables: SaveWorkflowNodeTestOutputRequest) => void;
}

export const useSaveWorkflowNodeTestOutputMutation = (mutationProps?: SaveWorkflowNodeTestOutputProps) =>
    useMutation({
        mutationFn: (request: SaveWorkflowNodeTestOutputRequest) => {
            return new WorkflowNodeTestOutputApi().saveWorkflowNodeTestOutput(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

interface UploadSampleOutputRequestProps {
    onSuccess?: (result: WorkflowNodeTestOutputModel, variables: UploadWorkflowNodeSampleOutputRequest) => void;
    onError?: (error: Error, variables: UploadWorkflowNodeSampleOutputRequest) => void;
}

export const useUploadSampleOutputRequestMutation = (mutationProps?: UploadSampleOutputRequestProps) =>
    useMutation({
        mutationFn: (request: UploadWorkflowNodeSampleOutputRequest) => {
            return new WorkflowNodeTestOutputApi().uploadWorkflowNodeSampleOutput(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
