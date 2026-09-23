import {AutomationWorkflowFormValuesI} from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-dialog/AutomationWorkflowDialog';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useCreateAutomationWorkflowProjectWorkflowMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ChangeEvent, RefObject, useRef} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useNavigate} from 'react-router-dom';

interface UseCreateAutomationWorkflowProjectWorkflowProps {
    bottomResizablePanelRef?: RefObject<PanelImperativeHandle | null>;
    projectId?: string;
}

export const useCreateAutomationWorkflowProjectWorkflow = ({
    bottomResizablePanelRef,
    projectId,
}: UseCreateAutomationWorkflowProjectWorkflowProps) => {
    const workflowFileInputRef = useRef<HTMLInputElement>(null);

    const setShowBottomPanelOpen = useWorkflowEditorStore((state) => state.setShowBottomPanelOpen);

    const createWorkflowMutation = useCreateAutomationWorkflowProjectWorkflowMutation();
    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const openCreatedWorkflow = (workflowUuid: string) => {
        void queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});

        setShowBottomPanelOpen(false);

        bottomResizablePanelRef?.current?.resize(0);

        void navigate(`/embedded/automation-workflows/${workflowUuid}/editor`);
    };

    const createWorkflow = (values: AutomationWorkflowFormValuesI) => {
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
                onSuccess: (data) => openCreatedWorkflow(data.createAutomationWorkflowProjectWorkflow),
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
                onSuccess: (data) => openCreatedWorkflow(data.createAutomationWorkflowProjectWorkflow),
            }
        );
    };

    return {
        createWorkflow,
        handleWorkflowFileChange,
        workflowFileInputRef,
    };
};
