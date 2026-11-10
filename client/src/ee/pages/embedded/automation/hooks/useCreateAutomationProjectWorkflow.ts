import {AutomationFormValuesI} from '@/ee/pages/embedded/automations/components/automation-dialog/AutomationDialog';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useCreateAutomationProjectWorkflowMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ChangeEvent, RefObject, useRef} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useNavigate} from 'react-router-dom';

interface UseCreateAutomationProjectWorkflowProps {
    bottomResizablePanelRef?: RefObject<PanelImperativeHandle | null>;
    projectId?: string;
}

export const useCreateAutomationProjectWorkflow = ({
    bottomResizablePanelRef,
    projectId,
}: UseCreateAutomationProjectWorkflowProps) => {
    const workflowFileInputRef = useRef<HTMLInputElement>(null);

    const setShowBottomPanelOpen = useWorkflowEditorStore((state) => state.setShowBottomPanelOpen);

    const createWorkflowMutation = useCreateAutomationProjectWorkflowMutation();
    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const openCreatedWorkflow = (workflowUuid: string) => {
        void queryClient.invalidateQueries({queryKey: ['automationProjects']});

        setShowBottomPanelOpen(false);

        bottomResizablePanelRef?.current?.resize(0);

        void navigate(`/embedded/automations/${workflowUuid}/editor`);
    };

    const createWorkflow = (values: AutomationFormValuesI) => {
        if (!projectId) {
            return;
        }

        const definition = JSON.stringify({
            description: values.description,
            inputs: [],
            label: values.label,
            tasks: [],
            triggers: [],
        });

        createWorkflowMutation.mutate(
            {definition, permissionExpression: values.permissionExpression, projectId},
            {
                onSuccess: (data) => openCreatedWorkflow(data.createAutomationProjectWorkflow),
            }
        );
    };

    const handleWorkflowFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];

        event.target.value = '';

        if (!file || !projectId) {
            return;
        }

        const definition = await file.text();

        createWorkflowMutation.mutate(
            {definition, projectId},
            {
                onSuccess: (data) => openCreatedWorkflow(data.createAutomationProjectWorkflow),
            }
        );
    };

    return {
        createWorkflow,
        handleWorkflowFileChange,
        workflowFileInputRef,
    };
};
