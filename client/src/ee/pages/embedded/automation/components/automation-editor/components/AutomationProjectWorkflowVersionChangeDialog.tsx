import WorkflowVersionChangesDialog from '@/shared/components/version-workflow-changes/WorkflowVersionChangesDialog';
import {formatVersionLabel} from '@/shared/components/version-workflow-changes/versionWorkflowChangesUtils';
import {useAutomationProjectVersionWorkflowChangeQuery} from '@/shared/middleware/graphql';

interface AutomationProjectWorkflowVersionChangeDialogProps {
    draft: boolean;
    onClose: () => void;
    projectId: string;
    projectVersion: number;
    workflowUuid: string;
}

const AutomationProjectWorkflowVersionChangeDialog = ({
    draft,
    onClose,
    projectId,
    projectVersion,
    workflowUuid,
}: AutomationProjectWorkflowVersionChangeDialogProps) => {
    const {data, isError, isLoading} = useAutomationProjectVersionWorkflowChangeQuery({
        id: projectId,
        projectVersion,
        workflowUuid,
    });

    const change = data?.automationProjectVersionWorkflowChange;

    return (
        <WorkflowVersionChangesDialog
            baseVersionLabel={change?.baseVersion == null ? undefined : formatVersionLabel(change.baseVersion, false)}
            change={change}
            isError={isError}
            isLoading={isLoading}
            onClose={onClose}
            open
            versionLabel={formatVersionLabel(change?.version ?? projectVersion, draft)}
        />
    );
};

export default AutomationProjectWorkflowVersionChangeDialog;
