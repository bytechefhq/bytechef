import Switch from 'components/Switch/Switch';
import {useGetProjectWorkflowsQuery} from 'queries/projects.queries';
import {Link} from 'react-router-dom';

const ProjectInstanceWorkflowList = ({projectId}: {projectId: number}) => {
    const {data: workflows} = useGetProjectWorkflowsQuery(projectId);

    return (
        <div className="border-b border-b-gray-100 px-2 py-4">
            <h3 className="mb-2 text-sm font-bold uppercase text-gray-500">
                Workflows
            </h3>

            <ul className="space-y-2">
                {workflows?.map((workflow) => (
                    <li
                        key={workflow.id}
                        className="flex items-center justify-between"
                    >
                        <Link
                            className="flex justify-start text-sm"
                            to={`/automation/projects/${projectId}/workflow/${workflow.id}`}
                        >
                            {workflow.label}
                        </Link>

                        <Switch />
                    </li>
                ))}
            </ul>
        </div>
    );
};

export default ProjectInstanceWorkflowList;
