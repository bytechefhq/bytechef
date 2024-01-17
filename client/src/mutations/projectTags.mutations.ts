import {ProjectTagApi, UpdateProjectTagsRequest} from '@/middleware/automation/configuration';
import {useMutation} from '@tanstack/react-query';

type UpdateProjectTagsMutationProps = {
    onSuccess?: (result: void, variables: UpdateProjectTagsRequest) => void;
    onError?: (error: Error, variables: UpdateProjectTagsRequest) => void;
};

export const useUpdateProjectTagsMutation = (mutationProps?: UpdateProjectTagsMutationProps) =>
    useMutation({
        mutationFn: (request: UpdateProjectTagsRequest) => {
            return new ProjectTagApi().updateProjectTags(request);
        },
        onError: mutationProps?.onError,
        onSuccess: mutationProps?.onSuccess,
    });
