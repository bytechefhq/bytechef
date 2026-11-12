import Badge from '@/components/Badge/Badge';
import {Breadcrumb, BreadcrumbItem, BreadcrumbList, BreadcrumbSeparator} from '@/components/ui/breadcrumb';
import BreadcrumbTitleSelect from '@/shared/components/BreadcrumbTitleSelect';
import ListPageBreadcrumbItem from '@/shared/components/ListPageBreadcrumbItem';
import {AutomationProjectsQuery} from '@/shared/middleware/graphql';
import {ReactNode, useMemo} from 'react';

type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];

interface AutomationEditorBreadcrumbProps {
    itemSelect?: ReactNode;
    onProjectValueChange?: (projectId: string) => void;
    project: AutomationProjectType;
    projects?: AutomationProjectType[];
}

const AutomationEditorBreadcrumb = ({
    itemSelect,
    onProjectValueChange,
    project,
    projects,
}: AutomationEditorBreadcrumbProps) => {
    const items = useMemo(
        () =>
            projects?.map((automationProject) => ({
                disabled: automationProject.workflowTemplates.length === 0,
                id: automationProject.id,
                name: automationProject.name,
                publishedVersion: automationProject.lastPublishedVersion ?? undefined,
            })),
        [projects]
    );

    const projectPublished = project.lastPublishedVersion === project.version;

    return (
        <Breadcrumb className="min-w-0">
            <BreadcrumbList className="flex-nowrap">
                <ListPageBreadcrumbItem label="Automations" to="/embedded/automations" />

                <BreadcrumbItem className="min-w-0">
                    <BreadcrumbTitleSelect
                        ariaLabel="Project select"
                        badge={
                            <Badge
                                className="flex shrink-0 space-x-1 border-transparent"
                                styleType={projectPublished ? 'success-outline' : 'secondary-filled'}
                                weight="semibold"
                            >
                                <span>V{project.version}</span>

                                <span>{projectPublished ? 'PUBLISHED' : 'DRAFT'}</span>
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

export default AutomationEditorBreadcrumb;
