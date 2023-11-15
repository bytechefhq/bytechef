import {ComponentDefinitionBasicModel} from '@/middleware/hermes/configuration';
import ProjectWorkflowListItem from '@/pages/automation/projects/ProjectWorkflowListItem';
import {useGetProjectWorkflowsQuery} from '@/queries/projects.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/queries/taskDispatcherDefinitions.queries';
import {ProjectModel} from 'middleware/helios/configuration';
import {useGetComponentDefinitionsQuery} from 'queries/componentDefinitions.queries';

const ProjectWorkflowList = ({project}: {project: ProjectModel}) => {
    const {data: workflows} = useGetProjectWorkflowsQuery(project.id!);

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery();

    const {data: taskDispatcherDefinitions} =
        useGetTaskDispatcherDefinitionsQuery();

    const workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    } = {};

    const workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    } = {};

    return (
        <div className="border-b border-b-gray-100 py-3 pl-4">
            <div className="mb-1 flex items-center justify-between">
                <h3 className="flex justify-start pl-2 text-sm font-semibold uppercase text-gray-500">
                    Workflows
                </h3>
            </div>

            <ul>
                {workflows?.map((workflow) => {
                    const definitionNames = workflow.tasks?.map(
                        (task) => task.type.split('/')[0]
                    );

                    definitionNames?.map((definitionName) => {
                        if (!workflowComponentDefinitions[definitionName]) {
                            workflowComponentDefinitions[definitionName] =
                                componentDefinitions?.find(
                                    (componentDefinition) =>
                                        componentDefinition.name ===
                                        definitionName
                                );
                        }

                        if (
                            !workflowTaskDispatcherDefinitions[definitionName]
                        ) {
                            workflowTaskDispatcherDefinitions[definitionName] =
                                taskDispatcherDefinitions?.find(
                                    (taskDispatcherDefinition) =>
                                        taskDispatcherDefinition.name ===
                                        definitionName
                                );
                        }
                    });

                    const filteredDefinitionNames = definitionNames?.filter(
                        (item, index) =>
                            definitionNames?.indexOf(item) === index
                    );

                    return (
                        <li
                            className="flex items-center justify-between rounded-md p-2 hover:bg-gray-50"
                            key={workflow.id}
                        >
                            <ProjectWorkflowListItem
                                filteredDefinitionNames={
                                    filteredDefinitionNames
                                }
                                key={workflow.id}
                                project={project}
                                workflow={workflow}
                                workflowComponentDefinitions={
                                    workflowComponentDefinitions
                                }
                                workflowTaskDispatcherDefinitions={
                                    workflowTaskDispatcherDefinitions
                                }
                            />
                        </li>
                    );
                })}
            </ul>
        </div>
    );
};

export default ProjectWorkflowList;
