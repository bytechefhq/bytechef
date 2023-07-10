import {Switch} from '@/components/ui/switch';
import {useGetComponentDefinitionsQuery} from 'queries/componentDefinitions.queries';
import {useGetProjectWorkflowsQuery} from 'queries/projects.queries';
import InlineSVG from 'react-inlinesvg';
import {Link} from 'react-router-dom';

const ProjectInstanceWorkflowList = ({projectId}: {projectId: number}) => {
    const {data: workflows} = useGetProjectWorkflowsQuery(projectId);

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery();

    const componentIcons: {[key: string]: string} = {};

    return (
        <div className="border-b border-b-gray-100 px-2 py-4">
            <h3 className="mb-2 text-sm font-bold uppercase text-gray-600">
                Workflows
            </h3>

            <ul className="space-y-2">
                {workflows?.map((workflow) => {
                    const componentNames = workflow.tasks?.map(
                        (task) => task.type.split('/')[0]
                    );

			        componentNames?.map((componentName) => {
			            if (!componentIcons[componentName]) {
			                componentIcons[componentName] =
			                    componentDefinitions?.find(
			                        (componentDefinition) =>
			                            componentDefinition.name === componentName
			                    )?.icon ?? '';
			            }
			        });

                    return (
                        <li
                            key={workflow.id}
                            className="flex items-center justify-between"
                        >
                            <Link
                                className="flex justify-start text-sm"
                                to={`/automation/projects/${projectId}/workflow/${workflow.id}`}
                            >
                                {workflow.label}

                                <div className="ml-6 flex">
                                    {componentNames?.map((componentName) => (
                                        <InlineSVG
                                            className="mr-1 h-5 w-5 flex-none"
                                            key={componentName}
                                            src={componentIcons[componentName]}
                                        />
                                    ))}
                                </div>
                            </Link>

                            <Switch />
                        </li>
                    );
                })}
            </ul>
        </div>
    );
};

export default ProjectInstanceWorkflowList;
