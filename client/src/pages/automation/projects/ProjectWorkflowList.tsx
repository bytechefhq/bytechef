import {ProjectModel} from 'middleware/automation/configuration';
import {useGetProjectWorkflowsQuery} from 'queries/projects.queries';
import {Link} from 'react-router-dom';

const ProjectWorkflowList = ({project}: {project: ProjectModel}) => {
    const {data: workflows} = useGetProjectWorkflowsQuery(project.id!);

    return (
        <ul className="pb-4 pl-2 pr-4">
            {workflows?.map((workflow) => (
                <li key={workflow.id} className="mb-2">
                    <Link
                        className="text-base text-gray-900"
                        to={`/automation/projects/${project.id}/workflow/${workflow.id}`}
                    >
                        {workflow.label}
                    </Link>
                </li>
            ))}
        </ul>
    );
};

export default ProjectWorkflowList;
