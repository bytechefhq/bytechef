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

                if (response.status === 409) {
                    throw new Error('Still preparing — try again in a moment.');
                }

                if (response.status === 400) {
                    const problemDetail = await response.json().catch(() => null);

                    if (problemDetail && typeof problemDetail.detail === 'string') {
                        throw new Error(problemDetail.detail);
                    }
                }

                if (response.status === 422) {
                    throw new Error('The workflow failed, so this decision can no longer be applied.');
                }

                throw new Error(`Submission failed (HTTP ${response.status}).`);
            }
        },
    });
