import McpProjectWorkflowListItem from './McpProjectWorkflowListItem';
import {McpProjectItemType, McpProjectWorkflowItemType} from './hooks/useMcpProjectList';

interface McpProjectWorkflowListProps {
    mcpProject: McpProjectItemType;
    mcpProjectWorkflows?: Array<McpProjectWorkflowItemType | null> | null;
}

const McpProjectWorkflowList = ({mcpProject, mcpProjectWorkflows}: McpProjectWorkflowListProps) => {
    const workflows =
        mcpProjectWorkflows?.filter((workflow): workflow is NonNullable<typeof workflow> => workflow !== null) || [];

    if (workflows.length === 0) {
        return null;
    }

    const projectId = mcpProject.project?.id != null ? Number(mcpProject.project.id) : undefined;

    return (
        <div className="flex flex-col gap-1">
            {workflows.map((workflow) => (
                <McpProjectWorkflowListItem
                    key={workflow.id}
                    mcpProjectWorkflow={workflow}
                    projectDeploymentId={Number(mcpProject.projectDeploymentId)}
                    projectId={projectId}
                    projectName={mcpProject.project?.name}
                    projectVersion={mcpProject.projectVersion ?? undefined}
                />
            ))}
        </div>
    );
};

export default McpProjectWorkflowList;
