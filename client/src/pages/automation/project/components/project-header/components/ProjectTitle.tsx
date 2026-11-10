import Badge from '@/components/Badge/Badge';
import BreadcrumbTitleSelect, {getPublishedVersion} from '@/shared/components/BreadcrumbTitleSelect';
import {Project, ProjectStatus} from '@/shared/middleware/automation/configuration';
import {useMemo} from 'react';

interface ProjectTitleProps {
    onProjectValueChange?: (projectId: number) => void;
    project: Project;
    projects?: Project[];
}

const ProjectTitle = ({onProjectValueChange, project, projects}: ProjectTitleProps) => {
    const items = useMemo(
        () =>
            projects?.map((workspaceProject) => ({
                disabled: !workspaceProject.projectWorkflowIds?.length,
                id: String(workspaceProject.id),
                name: workspaceProject.name,
                publishedVersion: getPublishedVersion(
                    workspaceProject.lastPublishedDate,
                    workspaceProject.lastProjectVersion,
                    workspaceProject.lastStatus === ProjectStatus.Published
                ),
            })),
        [projects]
    );

    return (
        <BreadcrumbTitleSelect
            ariaLabel="Project select"
            badge={
                <Badge
                    className="flex shrink-0 space-x-1 border-transparent"
                    styleType={project.lastStatus === ProjectStatus.Published ? 'success-outline' : 'secondary-filled'}
                    weight="semibold"
                >
                    <span>V{project.lastProjectVersion}</span>

                    <span>{project.lastStatus}</span>
                </Badge>
            }
            currentId={project.id !== undefined ? String(project.id) : undefined}
            items={items}
            menuLabel="Projects"
            name={project.name}
            onValueChange={onProjectValueChange && ((value) => onProjectValueChange(Number(value)))}
        />
    );
};

export default ProjectTitle;
