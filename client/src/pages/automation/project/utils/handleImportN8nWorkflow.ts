import {
    CreateProjectWorkflow200Response,
    CreateProjectWorkflowRequest,
} from '@/shared/middleware/automation/configuration';
import {UseMutationResult} from '@tanstack/react-query';
import {ChangeEvent} from 'react';

const handleImportN8nWorkflow = async (
    event: ChangeEvent<HTMLInputElement>,
    projectId: number,
    importProjectWorkflowMutation: UseMutationResult<
        CreateProjectWorkflow200Response,
        Error,
        CreateProjectWorkflowRequest,
        unknown
    >,
    convertN8nWorkflow: (input: string) => Promise<string>
) => {
    if (event.target.files) {
        const file = event.target.files[0];

        /* eslint-disable @typescript-eslint/no-explicit-any */
        const json = await (typeof (file as any).text === 'function'
            ? (file as Blob).text()
            : new Response(file).text());

        const convertedWorkflow = await convertN8nWorkflow(json);

        importProjectWorkflowMutation.mutate({
            id: projectId,
            workflow: {definition: convertedWorkflow},
        });
    }
};

export default handleImportN8nWorkflow;
