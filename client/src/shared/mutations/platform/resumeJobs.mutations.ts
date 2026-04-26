import {useMutation} from '@tanstack/react-query';

export interface ResumeJobVariablesI {
    approved: boolean;
    data: Record<string, unknown>;
    id: string;
}

export const useResumeJobMutation = () =>
    useMutation<void, Error, ResumeJobVariablesI>({
        mutationFn: async ({approved, data, id}) => {
            const response = await fetch(`/job/resume/${id}`, {
                body: JSON.stringify({...data, approved}),
                headers: {'Content-Type': 'application/json'},
                method: 'POST',
            });

            if (!response.ok) {
                throw new Error(`Submission failed: ${response.statusText}`);
            }
        },
    });
