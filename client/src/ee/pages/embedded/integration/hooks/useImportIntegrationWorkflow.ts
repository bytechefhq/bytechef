import {useCreateIntegrationWorkflowMutation} from '@/ee/shared/mutations/embedded/workflows.mutations';
import {IntegrationWorkflowKeys} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useQueryClient} from '@tanstack/react-query';
import {ChangeEvent, useRef} from 'react';
import {toast} from 'sonner';

export const useImportIntegrationWorkflow = (integrationId: number) => {
    const workflowFileInputRef = useRef<HTMLInputElement>(null);

    const {captureIntegrationWorkflowImported} = useAnalytics();

    const queryClient = useQueryClient();

    const importIntegrationWorkflowMutation = useCreateIntegrationWorkflowMutation({
        onSuccess: () => {
            captureIntegrationWorkflowImported();

            void queryClient.invalidateQueries({queryKey: IntegrationKeys.integration(integrationId)});
            void queryClient.invalidateQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflows(integrationId),
            });

            toast('Workflow is imported.');
        },
    });

    const handleWorkflowFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];

        if (!file) {
            return;
        }

        const definition = await file.text();

        importIntegrationWorkflowMutation.mutate({
            id: integrationId,
            workflow: {
                definition,
            },
        });

        if (workflowFileInputRef.current) {
            workflowFileInputRef.current.value = '';
        }
    };

    return {handleWorkflowFileChange, workflowFileInputRef};
};
