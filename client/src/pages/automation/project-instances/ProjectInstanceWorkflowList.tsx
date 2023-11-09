import {ProjectInstanceWorkflowModel} from '@/middleware/helios/configuration';
import {ComponentDefinitionBasicModel} from '@/middleware/hermes/configuration';
import ProjectInstanceWorkflowListItem from '@/pages/automation/project-instances/ProjectInstanceWorkflowListItem';
import {useGetTaskDispatcherDefinitionsQuery} from '@/queries/taskDispatcherDefinitions.queries';
import {useGetComponentDefinitionsQuery} from 'queries/componentDefinitions.queries';
import {useGetProjectWorkflowsQuery} from 'queries/projects.queries';

const ProjectInstanceWorkflowList = ({
    projectId,
    projectInstanceEnabled,
    projectInstanceId,
    projectInstanceWorkflows,
}: {
    projectId: number;
    projectInstanceId: number;
    projectInstanceEnabled: boolean;
    projectInstanceWorkflows?: Array<ProjectInstanceWorkflowModel>;
}) => {
    const {data: workflows} = useGetProjectWorkflowsQuery(projectId);

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
            <h3 className="flex justify-start pl-2 text-sm font-semibold uppercase text-gray-400">
                Workflows
            </h3>

            <ul>
                {workflows?.map((workflow) => {
                    const definitionNames = workflow.tasks?.map(
                        (task) => task.type.split('/')[0]
                    );

                    definitionNames?.forEach((definitionName) => {
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

                    const projectInstanceWorkflow =
                        projectInstanceWorkflows?.find(
                            (projectInstanceWorkflow) =>
                                projectInstanceWorkflow.workflowId ===
                                workflow?.id
                        );

                    return (
                        <li
                            className="flex items-center justify-between rounded-md p-2 hover:bg-gray-50"
                            key={workflow.id}
                        >
                            {projectInstanceWorkflow && (
                                <ProjectInstanceWorkflowListItem
                                    filteredDefinitionNames={
                                        filteredDefinitionNames
                                    }
                                    key={workflow.id}
                                    projectId={projectId}
                                    projectInstanceEnabled={
                                        projectInstanceEnabled
                                    }
                                    projectInstanceId={projectInstanceId}
                                    projectInstanceWorkflow={
                                        projectInstanceWorkflow
                                    }
                                    workflow={workflow}
                                    workflowComponentDefinitions={
                                        workflowComponentDefinitions
                                    }
                                    workflowTaskDispatcherDefinitions={
                                        workflowTaskDispatcherDefinitions
                                    }
                                />
                            )}
                        </li>
                    );
                })}
            </ul>
        </div>
    );
};

export default ProjectInstanceWorkflowList;
