import {
    SaveWorkflowNodeTestOutputRequest,
    UploadWorkflowNodeSampleOutputRequest,
    WorkflowNodeTestOutputApi,
    WorkflowNodeTestOutputModel,
} from '@/middleware/platform/configuration';
import {useMutation} from '@tanstack/react-query';

type SaveWorkflowNodeTestOutputProps = {
    onSuccess?: (result: WorkflowNodeTestOutputModel, variables: SaveWorkflowNodeTestOutputRequest) => void;
    onError?: (error: Error, variables: SaveWorkflowNodeTestOutputRequest) => void;
};

export const useSaveWorkflowNodeTestOutputMutation = (mutationProps?: SaveWorkflowNodeTestOutputProps) =>
    useMutation({
        mutationFn: (request: SaveWorkflowNodeTestOutputRequest) => {
            return new WorkflowNodeTestOutputApi().saveWorkflowNodeTestOutput(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });

type UploadSampleOutputRequestProps = {
    onSuccess?: (result: WorkflowNodeTestOutputModel, variables: UploadWorkflowNodeSampleOutputRequest) => void;
    onError?: (error: Error, variables: UploadWorkflowNodeSampleOutputRequest) => void;
};

export const useUploadSampleOutputRequestMutation = (mutationProps?: UploadSampleOutputRequestProps) =>
    useMutation({
        mutationFn: (request: UploadWorkflowNodeSampleOutputRequest) => {
            return new WorkflowNodeTestOutputApi().uploadWorkflowNodeSampleOutput(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
