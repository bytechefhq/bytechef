import {
    ProjectDeploymentTagApi,
    UpdateProjectDeploymentTagsRequest,
    UpdateProjectTagsRequest,
} from '@/shared/middleware/automation/configuration';
import {useMutation} from '@tanstack/react-query';

interface UpdateProjectDeploymentTagsMutationProps {
    onSuccess?: (result: void, variables: UpdateProjectDeploymentTagsRequest) => void;
    onError?: (error: Error, variables: UpdateProjectDeploymentTagsRequest) => void;
}

export const useUpdateProjectDeploymentTagsMutation = (mutationProps?: UpdateProjectDeploymentTagsMutationProps) =>
    useMutation({
        mutationFn: (request: UpdateProjectTagsRequest) => {
            return new ProjectDeploymentTagApi().updateProjectDeploymentTags(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
