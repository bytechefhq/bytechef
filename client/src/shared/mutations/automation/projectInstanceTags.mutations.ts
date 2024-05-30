import {
    ProjectInstanceTagApi,
    UpdateProjectInstanceTagsRequest,
    UpdateProjectTagsRequest,
} from '@/shared/middleware/automation/configuration';
import {useMutation} from '@tanstack/react-query';

interface UpdateProjectInstanceTagsMutationProps {
    onSuccess?: (result: void, variables: UpdateProjectInstanceTagsRequest) => void;
    onError?: (error: Error, variables: UpdateProjectInstanceTagsRequest) => void;
}

export const useUpdateProjectInstanceTagsMutation = (mutationProps?: UpdateProjectInstanceTagsMutationProps) =>
    useMutation({
        mutationFn: (request: UpdateProjectTagsRequest) => {
            return new ProjectInstanceTagApi().updateProjectInstanceTags(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
