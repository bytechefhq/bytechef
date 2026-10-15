import WorkflowsListItemDropdownMenu from '@/pages/automation/project/components/projects-sidebar/components/WorkflowsListItemDropdownMenu';
import WorkflowsSidebarListItem from '@/shared/components/workflow/WorkflowsSidebarListItem';
import {Project, Workflow} from '@/shared/middleware/automation/configuration';
import {useMemo} from 'react';

interface WorkflowsListItemProps {
    calculateTimeDifference: (date: string) => string;
    workflow: Workflow;
    currentWorkflowId: string;
    onProjectClick: (projectId: number, projectWorkflowId: number) => void;
    findProjectIdByWorkflow: (workflow: Workflow) => number;
    project?: Project;
    setSelectedProjectId: (projectId: number) => void;
}

const WorkflowsListItem = ({
    calculateTimeDifference,
    currentWorkflowId,
    findProjectIdByWorkflow,
    onProjectClick,
    project,
    setSelectedProjectId,
    workflow,
}: WorkflowsListItemProps) => {
    const projectId = findProjectIdByWorkflow(workflow);
    const projectWorkflowId = workflow.projectWorkflowId || 0;

    const timeAgo = useMemo(
        () => calculateTimeDifference(workflow?.lastModifiedDate?.toString() || ''),
        [calculateTimeDifference, workflow?.lastModifiedDate]
    );

    const handleCardClick = () => {
        onProjectClick(projectId, projectWorkflowId);

        setSelectedProjectId(projectId);
    };

    return (
        <WorkflowsSidebarListItem
            active={workflow.id === currentWorkflowId}
            editedTimeAgo={timeAgo}
            label={workflow.label}
            menu={
                project &&
                !project.codeWorkflow && (
                    <WorkflowsListItemDropdownMenu
                        currentWorkflowId={currentWorkflowId}
                        project={project}
                        workflow={workflow}
                    />
                )
            }
            onClick={handleCardClick}
        />
    );
};

export default WorkflowsListItem;
