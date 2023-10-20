import {ProjectModel} from 'middleware/automation/configuration';
import {useGetProjectWorkflowsQuery} from 'queries/projects.queries';
import {Link} from 'react-router-dom';

const ProjectWorkflowList = ({project}: {project: ProjectModel}) => {
    const {data: workflows} = useGetProjectWorkflowsQuery(project.id!);

    return (
        <div className="rounded-b-md border bg-gray-100 p-4">
            <h3 className="mb-2 text-sm font-bold uppercase text-gray-500">
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
