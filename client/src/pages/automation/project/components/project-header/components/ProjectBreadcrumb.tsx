import {Breadcrumb, BreadcrumbItem, BreadcrumbList, BreadcrumbSeparator} from '@/components/ui/breadcrumb';
import ProjectTitle from '@/pages/automation/project/components/project-header/components/ProjectTitle';
import WorkflowSelect from '@/pages/automation/project/components/project-header/components/WorkflowSelect';
import {Project, Workflow} from '@/shared/middleware/automation/configuration';

export interface ProjectBreadcrumbProps {
    currentWorkflow: Workflow;
    onProjectWorkflowValueChange: (projectWorkflowId: number) => void;
    project: Project;
    projectWorkflowId: number;
    projectWorkflows: Workflow[];
    showWorkflowSelect?: boolean;
}
const ProjectBreadcrumb = ({
    currentWorkflow,
    onProjectWorkflowValueChange,
    project,
    projectWorkflowId,
    projectWorkflows,
    showWorkflowSelect = true,
}: ProjectBreadcrumbProps) => (
    <Breadcrumb>
        <BreadcrumbList>
            <BreadcrumbItem>
                <ProjectTitle project={project} />
            </BreadcrumbItem>

            {showWorkflowSelect && (
                <>
                    <BreadcrumbSeparator />

                    <BreadcrumbItem>
                        <WorkflowSelect
                            currentWorkflowLabel={currentWorkflow.label}
                            onValueChange={onProjectWorkflowValueChange}
                            projectId={project.id!}
                            projectWorkflowId={projectWorkflowId}
                            projectWorkflows={projectWorkflows}
                        />
                    </BreadcrumbItem>
                </>
            )}
        </BreadcrumbList>
    </Breadcrumb>
);

export default ProjectBreadcrumb;
