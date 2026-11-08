import {Breadcrumb, BreadcrumbItem, BreadcrumbList, BreadcrumbSeparator} from '@/components/ui/breadcrumb';
import ProjectTitle from '@/pages/automation/project/components/project-header/components/ProjectTitle';
import {Project} from '@/shared/middleware/automation/configuration';
import {ReactNode} from 'react';

export interface ProjectBreadcrumbProps {
    /** The current-item switcher (e.g. ProjectItemSelect) shown after the project title, if any. */
    itemSelect?: ReactNode;
    project: Project;
}

const ProjectBreadcrumb = ({itemSelect, project}: ProjectBreadcrumbProps) => (
    <Breadcrumb className="min-w-0">
        <BreadcrumbList className="flex-nowrap">
            <BreadcrumbItem className="min-w-0">
                <ProjectTitle project={project} />
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

export default ProjectBreadcrumb;
