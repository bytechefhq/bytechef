import {CreateProjectWorkflowRequest} from '@/shared/middleware/automation/configuration';
import {UseMutationResult} from '@tanstack/react-query';
import {ChangeEvent} from 'react';

const handleImportWorkflow = async (
    event: ChangeEvent<HTMLInputElement>,
    projectId: number,
    importProjectWorkflowMutation: UseMutationResult<number, Error, CreateProjectWorkflowRequest, unknown>
) => {
    if (event.target.files) {
        const file = event.target.files[0];

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const definition = await (typeof (file as any).text === 'function'
            ? (file as Blob).text()
            : new Response(file).text());

        importProjectWorkflowMutation.mutate({
            id: projectId,
            workflow: {
                definition,
            },
        });
    }
};

export default handleImportWorkflow;
