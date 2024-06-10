import {Badge} from '@/components/ui/badge';
import {ProjectModel, ProjectStatusModel} from '@/shared/middleware/automation/configuration';

const ProjectHeaderProjectVersionBadge = ({project}: {project: ProjectModel}) => (
    <Badge className="flex space-x-1" variant={project.status === ProjectStatusModel.Published ? 'success' : 'outline'}>
        <span>V{project.projectVersion}</span>

        <span>{project.status === ProjectStatusModel.Published ? `Published` : 'Draft'}</span>
    </Badge>
);

export default ProjectHeaderProjectVersionBadge;
