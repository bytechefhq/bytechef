import {JobApi} from '@/shared/middleware/platform/workflow/execution/apis/JobApi';
import {useMutation} from '@tanstack/react-query';

interface JobMutationProps {
    onError?: (error: Error, variables: number) => void;
    onSuccess?: (result: void, variables: number) => void;
}

export const useStopJobMutation = (jobMutationProps: JobMutationProps) =>
    useMutation<void, Error, number>({
        mutationFn: (id: number) => {
            return new JobApi().stopJob({
                id,
            });
        },
        onError: jobMutationProps?.onError,
        onSuccess: jobMutationProps?.onSuccess,
    });
