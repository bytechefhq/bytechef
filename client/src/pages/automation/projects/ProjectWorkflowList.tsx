import {ProjectModel} from 'middleware/automation/configuration';
import {useGetProjectWorkflowsQuery} from 'queries/projects.queries';
import {Link} from 'react-router-dom';

const ProjectWorkflowList = ({project}: {project: ProjectModel}) => {
    const {data: workflows} = useGetProjectWorkflowsQuery(project.id!);

    return (
        <div className="border-b border-b-gray-100 px-2 py-4">
            <h3 className="mb-2 text-sm font-semibold uppercase text-gray-600">
                Workflows
            </h3>

            <ul className="space-y-2">
                {workflows?.map((workflow) => (
                    <li key={workflow.id}>
                        <Link
                            className="text-sm"
                            to={`/automation/projects/${project.id}/workflow/${workflow.id}`}
                        >
                            {workflow.label}
                        </Link>
                    </li>
                ))}
            </ul>
        </div>
    );
};

export default ProjectWorkflowList;
