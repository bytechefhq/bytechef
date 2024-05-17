import {ProjectModel} from '@/middleware/automation/configuration';

const ProjectInstanceDialogBasicStepProjectLabel = ({project}: {project: ProjectModel}) => (
    <div className="flex items-center">
        <span className="mr-1 ">{project.name}</span>

        <span className="text-xs text-gray-500">{project?.tags?.map((tag) => tag.name).join(', ')}</span>
    </div>
);

export default ProjectInstanceDialogBasicStepProjectLabel;
