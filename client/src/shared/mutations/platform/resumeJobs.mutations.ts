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
                if (response.status === 410) {
                    throw new Error(
                        'This approval has expired or was already resolved, and can no longer be submitted.'
                    );
                }

                throw new Error(`Submission failed (HTTP ${response.status}).`);
            }
        },
    });
