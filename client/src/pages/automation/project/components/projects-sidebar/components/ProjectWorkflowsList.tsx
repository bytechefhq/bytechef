import {Badge} from '@/components/ui/badge';
import {Separator} from '@/components/ui/separator';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import WorkflowsListItem from '@/pages/automation/project/components/projects-sidebar/components/WorkflowsListItem';
import {Project, Workflow} from '@/shared/middleware/automation/configuration';
import {useMemo} from 'react';

interface ProjectWorkflowsListProps {
    calculateTimeDifference: (date: string) => string;
    project: Project;
    filteredWorkflowsList: Workflow[];
    currentWorkflowId: string;
    findProjectIdByWorkflow: (workflow: Workflow) => number;
    onProjectClick: (projectId: number, projectWorkflowId: number) => void;
    setSelectedProjectId: (projectId: number) => void;
}

const ProjectWorkflowsList = ({
    calculateTimeDifference,
    currentWorkflowId,
    filteredWorkflowsList,
    findProjectIdByWorkflow,
    onProjectClick,
    project,
    setSelectedProjectId,
}: ProjectWorkflowsListProps) => {
    const projectWorkflows = useMemo(
        () =>
            filteredWorkflowsList.filter(
                (workflow) => project.id !== undefined && findProjectIdByWorkflow(workflow) === project.id
            ),
        [filteredWorkflowsList, findProjectIdByWorkflow, project.id]
    );

    if (projectWorkflows.length === 0) {
        return null;
    }

    return (
        <>
            <li className="max-w-full pb-2 last:pb-0">
                <div className="flex w-80 items-center gap-1">
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <span className="inline-block w-56 overflow-hidden truncate rounded-md px-1 py-2 text-lg font-semibold">
                                {project.name}
                            </span>
                        </TooltipTrigger>

                        {project.name && project.name.length > 25 && (
                            <TooltipContent className="max-w-80">{project.name}</TooltipContent>
                        )}
                    </Tooltip>

                    {project.lastPublishedDate && project.lastVersion ? (
                        <Badge className="flex space-x-1" variant="success">
                            <span>V{project.lastVersion - 1}</span>

                            <span>PUBLISHED</span>
                        </Badge>
                    ) : (
                        <Badge className="flex space-x-1" variant="secondary">
                            <span>V{project.lastVersion}</span>

                            <span>{project.lastStatus}</span>
                        </Badge>
                    )}
                </div>

                <ul className="flex flex-col items-center gap-2">
                    {projectWorkflows.map((workflow) => (
                        <WorkflowsListItem
                            calculateTimeDifference={calculateTimeDifference}
                            currentWorkflowId={currentWorkflowId}
                            findProjectIdByWorkflow={findProjectIdByWorkflow}
                            key={workflow.id}
                            onProjectClick={onProjectClick}
                            setSelectedProjectId={setSelectedProjectId}
                            workflow={workflow}
                        />
                    ))}
                </ul>
            </li>

            <Separator />
        </>
    );
};

export default ProjectWorkflowsList;
