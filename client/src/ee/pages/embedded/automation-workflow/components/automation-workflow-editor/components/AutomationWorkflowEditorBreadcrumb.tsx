import Badge from '@/components/Badge/Badge';
import {Breadcrumb, BreadcrumbItem, BreadcrumbList, BreadcrumbSeparator} from '@/components/ui/breadcrumb';
import BreadcrumbTitleSelect from '@/shared/components/BreadcrumbTitleSelect';
import ListPageBreadcrumbItem from '@/shared/components/ListPageBreadcrumbItem';
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {ReactNode, useMemo} from 'react';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];

interface AutomationWorkflowEditorBreadcrumbProps {
    itemSelect?: ReactNode;
    onProjectValueChange?: (projectId: string) => void;
    project: AutomationWorkflowProjectType;
    projects?: AutomationWorkflowProjectType[];
}

const AutomationWorkflowEditorBreadcrumb = ({
    itemSelect,
    onProjectValueChange,
    project,
    projects,
}: AutomationWorkflowEditorBreadcrumbProps) => {
    const items = useMemo(
        () =>
            projects?.map((automationWorkflowProject) => ({
                disabled: automationWorkflowProject.workflowTemplates.length === 0,
                id: automationWorkflowProject.id,
                name: automationWorkflowProject.name,
                publishedVersion: automationWorkflowProject.lastPublishedVersion ?? undefined,
            })),
        [projects]
    );

    return (
        <Breadcrumb className="min-w-0">
            <BreadcrumbList className="flex-nowrap">
                <ListPageBreadcrumbItem label="Automations" to="/embedded/automation-workflows" />

                <BreadcrumbItem className="min-w-0">
                    <BreadcrumbTitleSelect
                        ariaLabel="Project select"
                        badge={
                            <Badge
                                className="flex shrink-0 space-x-1 border-transparent"
                                styleType="secondary-filled"
                                weight="semibold"
                            >
                                <span>V{project.version}</span>

                                <span>DRAFT</span>
                            </Badge>
                        }
                        currentId={project.id}
                        items={items}
                        menuLabel="Projects"
                        name={project.name}
                        onValueChange={onProjectValueChange}
                    />
                </BreadcrumbItem>

                {itemSelect && (
                    <>
                        <BreadcrumbSeparator />

                        <BreadcrumbItem className="min-w-0">{itemSelect}</BreadcrumbItem>
                    </>
                )}
            </BreadcrumbList>
        </Breadcrumb>
    );
};

export default AutomationWorkflowEditorBreadcrumb;
