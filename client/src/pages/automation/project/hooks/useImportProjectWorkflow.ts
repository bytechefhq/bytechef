import {useConvertN8nToWorkflow} from '@/pages/automation/project/hooks/useConverterN8nToWorkflow';
import handleImportN8nWorkflow from '@/pages/automation/project/utils/handleImportN8nWorkflow';
import handleImportWorkflow from '@/pages/automation/project/utils/handleImportWorkflow';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useHasEnabledAiProvider} from '@/shared/hooks/useHasEnabledAiProvider';
import {useCreateProjectWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useQueryClient} from '@tanstack/react-query';
import {ChangeEvent, useRef, useState} from 'react';
import {toast} from 'sonner';

export const useImportProjectWorkflow = (projectId: number) => {
    const [isImportingN8nWorkflow, setIsImportingN8nWorkflow] = useState(false);

    const n8nWorkflowFileInputRef = useRef<HTMLInputElement>(null);
    const workflowFileInputRef = useRef<HTMLInputElement>(null);

    const {captureProjectWorkflowImported} = useAnalytics();
    const {convertN8nWorkflow} = useConvertN8nToWorkflow();
    const {hasEnabledAiProvider, isPending: isAiProviderCheckPending} = useHasEnabledAiProvider();

    const queryClient = useQueryClient();

    const importN8nWorkflowDisabled = !isAiProviderCheckPending && !hasEnabledAiProvider;

    const importProjectWorkflowMutation = useCreateProjectWorkflowMutation({
        onSuccess: () => {
            captureProjectWorkflowImported();

            queryClient.invalidateQueries({queryKey: ProjectKeys.project(projectId)});
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            if (workflowFileInputRef.current) {
                workflowFileInputRef.current.value = '';
            }

            toast('Workflow is imported.');
        },
    });

    const handleN8nWorkflowFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
        if (!event.target.files?.length) {
            return;
        }

        try {
            setIsImportingN8nWorkflow(true);

            await handleImportN8nWorkflow(event, projectId, importProjectWorkflowMutation, convertN8nWorkflow);
        } finally {
            setIsImportingN8nWorkflow(false);

            if (n8nWorkflowFileInputRef.current) {
                n8nWorkflowFileInputRef.current.value = '';
            }
        }
    };

    const handleWorkflowFileChange = (event: ChangeEvent<HTMLInputElement>) =>
        handleImportWorkflow(event, projectId, importProjectWorkflowMutation);

    return {
        handleN8nWorkflowFileChange,
        handleWorkflowFileChange,
        importN8nWorkflowDisabled,
        isImportingN8nWorkflow,
        n8nWorkflowFileInputRef,
        workflowFileInputRef,
    };
};
