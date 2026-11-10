import AutomationWorkflowEditorWorkflowsListItemDropdownMenu from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorWorkflowsListItemDropdownMenu';
import {useProjectsLeftSidebar} from '@/pages/automation/project/components/projects-sidebar/hooks/useProjectsLeftSidebar';
import WorkflowsSidebarListItem from '@/shared/components/workflow/WorkflowsSidebarListItem';
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];
type AutomationWorkflowProjectWorkflowTemplateType = AutomationWorkflowProjectType['workflowTemplates'][number];

interface AutomationWorkflowEditorWorkflowsListItemProps {
    currentWorkflowId: string;
    onWorkflowClick: (workflowUuid: string) => void;
    project: AutomationWorkflowProjectType;
    workflow: AutomationWorkflowProjectWorkflowTemplateType;
}

const AutomationWorkflowEditorWorkflowsListItem = ({
    currentWorkflowId,
    onWorkflowClick,
    project,
    workflow,
}: AutomationWorkflowEditorWorkflowsListItemProps) => {
    const {calculateTimeDifference} = useProjectsLeftSidebar();

    const editedTimeAgo = workflow.lastModifiedDate ? calculateTimeDifference(workflow.lastModifiedDate) : undefined;

    return (
        <WorkflowsSidebarListItem
            active={workflow.workflowUuid === currentWorkflowId}
            editedTimeAgo={editedTimeAgo}
            label={workflow.label}
            menu={
                <AutomationWorkflowEditorWorkflowsListItemDropdownMenu
                    currentWorkflowId={currentWorkflowId}
                    project={project}
                    workflow={workflow}
                />
            }
            onClick={() => onWorkflowClick(workflow.workflowUuid)}
        />
    );
};

export default AutomationWorkflowEditorWorkflowsListItem;
