import {Breadcrumb, BreadcrumbItem, BreadcrumbList, BreadcrumbSeparator} from '@/components/ui/breadcrumb';
import ProjectTitle from '@/pages/automation/project/components/project-header/components/ProjectTitle';
import WorkflowSelect from '@/pages/automation/project/components/project-header/components/WorkflowSelect';
import {Project, Workflow} from '@/shared/middleware/automation/configuration';

export interface ProjectBreadcrumbProps {
    currentWorkflowLabel: string;
    onProjectWorkflowValueChange: (projectWorkflowId: number) => void;
    project: Project;
    projectWorkflowId: number;
    projectWorkflows: Workflow[];
}
const ProjectBreadcrumb = ({
    currentWorkflowLabel,
    onProjectWorkflowValueChange,
    project,
    projectWorkflowId,
    projectWorkflows,
}: ProjectBreadcrumbProps) => {
    return (
        <Breadcrumb>
            <BreadcrumbList>
                <BreadcrumbSeparator />

                <BreadcrumbItem>
                    <ProjectTitle project={project} />
                </BreadcrumbItem>

                <BreadcrumbSeparator />

                <BreadcrumbItem>
                    <WorkflowSelect
                        currentWorkflowLabel={currentWorkflowLabel}
                        onValueChange={onProjectWorkflowValueChange}
                        projectId={project.id!}
                        projectWorkflowId={projectWorkflowId}
                        projectWorkflows={projectWorkflows}
                    />
                </BreadcrumbItem>
            </BreadcrumbList>
        </Breadcrumb>
    );
};

export default ProjectBreadcrumb;
