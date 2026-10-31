import WorkflowVersionChangesDialog from '@/shared/components/version-workflow-changes/WorkflowVersionChangesDialog';
import {formatVersionLabel} from '@/shared/components/version-workflow-changes/versionWorkflowChangesUtils';
import {useProjectVersionWorkflowChangeQuery} from '@/shared/middleware/graphql';

interface ProjectWorkflowVersionChangeDialogProps {
    draft: boolean;
    onClose: () => void;
    projectId: number;
    projectVersion: number;
    workflowUuid: string;
}

const ProjectWorkflowVersionChangeDialog = ({
    draft,
    onClose,
    projectId,
    projectVersion,
    workflowUuid,
}: ProjectWorkflowVersionChangeDialogProps) => {
    const {data, isLoading} = useProjectVersionWorkflowChangeQuery({
        projectId: String(projectId),
        projectVersion,
        workflowUuid,
    });

    const change = data?.projectVersionWorkflowChange;

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

export default ProjectWorkflowVersionChangeDialog;
