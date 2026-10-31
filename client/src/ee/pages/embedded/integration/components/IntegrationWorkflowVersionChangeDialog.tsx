import WorkflowVersionChangesDialog from '@/shared/components/version-workflow-changes/WorkflowVersionChangesDialog';
import {formatVersionLabel} from '@/shared/components/version-workflow-changes/versionWorkflowChangesUtils';
import {useIntegrationVersionWorkflowChangeQuery} from '@/shared/middleware/graphql';

interface IntegrationWorkflowVersionChangeDialogProps {
    draft: boolean;
    integrationId: number;
    integrationVersion: number;
    onClose: () => void;
    workflowUuid: string;
}

const IntegrationWorkflowVersionChangeDialog = ({
    draft,
    integrationId,
    integrationVersion,
    onClose,
    workflowUuid,
}: IntegrationWorkflowVersionChangeDialogProps) => {
    const {data, isLoading} = useIntegrationVersionWorkflowChangeQuery({
        integrationId: String(integrationId),
        integrationVersion,
        workflowUuid,
    });

    const change = data?.integrationVersionWorkflowChange;

    return (
        <WorkflowVersionChangesDialog
            baseVersionLabel={change?.baseVersion == null ? undefined : formatVersionLabel(change.baseVersion, false)}
            change={change}
            isLoading={isLoading}
            onClose={onClose}
            open
            versionLabel={formatVersionLabel(change?.version ?? integrationVersion, draft)}
        />
    );
};

export default IntegrationWorkflowVersionChangeDialog;
