import AutomationEditorWorkflowsListItemDropdownMenu from '@/ee/pages/embedded/automation/components/automation-editor/components/AutomationEditorWorkflowsListItemDropdownMenu';
import {useProjectsLeftSidebar} from '@/pages/automation/project/components/projects-sidebar/hooks/useProjectsLeftSidebar';
import WorkflowsSidebarListItem from '@/shared/components/workflow/WorkflowsSidebarListItem';
import {AutomationProjectsQuery} from '@/shared/middleware/graphql';

type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];
type AutomationProjectWorkflowTemplateType = AutomationProjectType['workflowTemplates'][number];

interface AutomationEditorWorkflowsListItemProps {
    currentWorkflowId: string;
    onWorkflowClick: (workflowUuid: string) => void;
    project: AutomationProjectType;
    workflow: AutomationProjectWorkflowTemplateType;
}

const AutomationEditorWorkflowsListItem = ({
    currentWorkflowId,
    onWorkflowClick,
    project,
    workflow,
}: AutomationEditorWorkflowsListItemProps) => {
    const {calculateTimeDifference} = useProjectsLeftSidebar();

    const editedTimeAgo = workflow.lastModifiedDate ? calculateTimeDifference(workflow.lastModifiedDate) : undefined;

    return (
        <WorkflowsSidebarListItem
            active={workflow.workflowUuid === currentWorkflowId}
            editedTimeAgo={editedTimeAgo}
            label={workflow.label}
            menu={
                !project.codeWorkflowProject && (
                    <AutomationEditorWorkflowsListItemDropdownMenu
                        currentWorkflowId={currentWorkflowId}
                        project={project}
                        workflow={workflow}
                    />
                )
            }
            onClick={() => onWorkflowClick(workflow.workflowUuid)}
        />
    );
};

export default AutomationEditorWorkflowsListItem;
