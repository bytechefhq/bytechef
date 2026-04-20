import ComboBox, {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import {Skeleton} from '@/components/ui/skeleton';
import {Project} from '@/shared/middleware/automation/configuration';
import {FocusEventHandler} from 'react';

const ProjectLabel = ({project}: {project: Project}) => (
    <div className="flex items-center">
        <span className="mr-1">{project.name}</span>

        <span className="text-xs text-gray-500">{project?.tags?.map((tag) => tag.name).join(', ')}</span>
    </div>
);

interface ProjectDeploymentDialogBasicStepProjectsComboBoxProps {
    onBlur: FocusEventHandler;
    onChange: (item?: ComboBoxItemType) => void;
    projects?: Project[];
    value?: number;
}

const ProjectDeploymentDialogBasicStepProjectsComboBox = ({
    onBlur,
    onChange,
    projects,
    value,
}: ProjectDeploymentDialogBasicStepProjectsComboBoxProps) =>
    projects ? (
        <ComboBox
            emptyMessage="No published projects found. Please publish a project first."
            items={projects.map(
                (project) =>
                    ({
                        label: <ProjectLabel project={project} />,
                        name: project.name,
                        value: project.id,
                    }) as ComboBoxItemType
            )}
            name="projectId"
            onBlur={onBlur}
            onChange={onChange}
            value={value}
        />
    ) : (
        <Skeleton className="h-9 w-full" />
    );

export default ProjectDeploymentDialogBasicStepProjectsComboBox;
