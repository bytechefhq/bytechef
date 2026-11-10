import {Breadcrumb, BreadcrumbItem, BreadcrumbList, BreadcrumbSeparator} from '@/components/ui/breadcrumb';
import ProjectTitle from '@/pages/automation/project/components/project-header/components/ProjectTitle';
import ListPageBreadcrumbItem from '@/shared/components/ListPageBreadcrumbItem';
import {Project} from '@/shared/middleware/automation/configuration';
import {ReactNode} from 'react';

export interface ProjectBreadcrumbProps {
    /** The current-item switcher (e.g. ProjectItemSelect) shown after the project title, if any. */
    itemSelect?: ReactNode;
    onProjectValueChange?: (projectId: number) => void;
    project: Project;
    projects?: Project[];
}

const ProjectBreadcrumb = ({itemSelect, onProjectValueChange, project, projects}: ProjectBreadcrumbProps) => (
    <Breadcrumb className="min-w-0">
        <BreadcrumbList className="flex-nowrap">
            <ListPageBreadcrumbItem label="Projects" to="/automation/projects" />

            <BreadcrumbItem className="min-w-0">
                <ProjectTitle onProjectValueChange={onProjectValueChange} project={project} projects={projects} />
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
