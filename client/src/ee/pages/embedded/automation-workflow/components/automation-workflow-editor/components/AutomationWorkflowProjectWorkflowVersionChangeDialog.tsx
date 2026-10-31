import WorkflowVersionChangesDialog from '@/shared/components/version-workflow-changes/WorkflowVersionChangesDialog';
import {formatVersionLabel} from '@/shared/components/version-workflow-changes/versionWorkflowChangesUtils';
import {useAutomationWorkflowProjectVersionWorkflowChangeQuery} from '@/shared/middleware/graphql';

interface AutomationWorkflowProjectWorkflowVersionChangeDialogProps {
    draft: boolean;
    onClose: () => void;
    projectId: string;
    projectVersion: number;
    workflowUuid: string;
}

const AutomationWorkflowProjectWorkflowVersionChangeDialog = ({
    draft,
    onClose,
    projectId,
    projectVersion,
    workflowUuid,
}: AutomationWorkflowProjectWorkflowVersionChangeDialogProps) => {
    const {data, isLoading} = useAutomationWorkflowProjectVersionWorkflowChangeQuery({
        id: projectId,
        projectVersion,
        workflowUuid,
    });

    const change = data?.automationWorkflowProjectVersionWorkflowChange;

    return (
        <WorkflowVersionChangesDialog
            baseVersionLabel={change?.baseVersion == null ? undefined : formatVersionLabel(change.baseVersion, false)}
            change={change}
            isLoading={isLoading}
            onClose={onClose}
            open
            versionLabel={formatVersionLabel(change?.version ?? projectVersion, draft)}
        />
    );
};

export default AutomationWorkflowProjectWorkflowVersionChangeDialog;
