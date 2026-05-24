import Badge from '@/components/Badge/Badge';
import {Breadcrumb, BreadcrumbItem, BreadcrumbList, BreadcrumbSeparator} from '@/components/ui/breadcrumb';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AutomationWorkflowEditorWorkflowSelect from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorWorkflowSelect';
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];

interface AutomationWorkflowEditorBreadcrumbProps {
    currentWorkflowId: string;
    onWorkflowValueChange: (workflowUuid: string) => void;
    project: AutomationWorkflowProjectType;
}

const AutomationWorkflowEditorBreadcrumb = ({
    currentWorkflowId,
    onWorkflowValueChange,
    project,
}: AutomationWorkflowEditorBreadcrumbProps) => (
    <Breadcrumb>
        <BreadcrumbList>
            <BreadcrumbItem>
                <Tooltip>
                    <TooltipTrigger asChild>
                        <div className="flex max-w-96 items-center space-x-2">
                            <h1 className="truncate">{project.name}</h1>

                            <Badge
                                className="flex space-x-1 bg-surface-neutral-primary"
                                styleType={project.published ? 'success-outline' : 'outline-outline'}
                                weight="semibold"
                            >
                                <span>V{project.published ? project.lastPublishedVersion : project.version}</span>

                                <span>{project.published ? 'PUBLISHED' : 'DRAFT'}</span>
                            </Badge>
                        </div>
                    </TooltipTrigger>

                    {project.name.length > 43 && <TooltipContent>{project.name}</TooltipContent>}
                </Tooltip>
            </BreadcrumbItem>

            <BreadcrumbSeparator />

            <BreadcrumbItem>
                <AutomationWorkflowEditorWorkflowSelect
                    currentWorkflowId={currentWorkflowId}
                    onValueChange={onWorkflowValueChange}
                    workflows={project.workflowTemplates}
                />
            </BreadcrumbItem>
        </BreadcrumbList>
    </Breadcrumb>
);

export default AutomationWorkflowEditorBreadcrumb;
