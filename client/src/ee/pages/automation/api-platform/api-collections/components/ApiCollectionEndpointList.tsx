import EmptyList from '@/components/EmptyList';
import {Button} from '@/components/ui/button';
import ApiCollectionEndpointDialog from '@/ee/pages/automation/api-platform/api-collections/components/ApiCollectionEndpointDialog';
import {ApiCollectionEndpoint} from '@/middleware/automation/api-platform';
import {useGetProjectInstanceQuery} from '@/shared/queries/automation/projectInstances.queries';
import {useGetProjectVersionWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {WorkflowIcon} from 'lucide-react';

import ApiCollectionEndpointListItem from './ApiCollectionEndpointListItem';

const ApiCollectionEndpointList = ({
    apiCollectionEndpoints,
    apiCollectionId,
    projectId,
    projectInstanceId,
    projectVersion,
}: {
    apiCollectionId: number;
    apiCollectionEndpoints?: Array<ApiCollectionEndpoint>;
    projectId: number;
    projectInstanceId: number;
    projectVersion: number;
}) => {
    const {data: workflows} = useGetProjectVersionWorkflowsQuery(projectId, projectVersion);

    const {data: projectInstance} = useGetProjectInstanceQuery(projectInstanceId);

    return (
        <div className="border-b border-b-gray-100 py-3 pl-4">
            {apiCollectionEndpoints && apiCollectionEndpoints.length > 0 ? (
                <>
                    <div className="mb-1 flex items-center justify-between">
                        <h3 className="flex justify-start pl-2 text-sm font-semibold uppercase text-gray-400">
                            Endpoints
                        </h3>
                    </div>

                    <ul className="divide-y divide-gray-100">
                        {apiCollectionEndpoints?.map((apiCollectionEndpoint) => (
                            <li
                                className="flex items-center justify-between rounded-md p-2 hover:bg-gray-50"
                                key={apiCollectionEndpoint.id}
                            >
                                {apiCollectionEndpoint &&
                                    projectInstance &&
                                    projectInstance.projectInstanceWorkflows &&
                                    workflows && (
                                        <ApiCollectionEndpointListItem
                                            apiCollectionEndpoint={apiCollectionEndpoint}
                                            projectId={projectId}
                                            projectInstanceId={projectInstanceId}
                                            projectInstanceWorkflow={
                                                projectInstance.projectInstanceWorkflows.find(
                                                    (projectInstanceWorkflow) =>
                                                        projectInstanceWorkflow.workflowReferenceCode ===
                                                        apiCollectionEndpoint.workflowReferenceCode
                                                )!
                                            }
                                            projectVersion={projectVersion}
                                            workflows={workflows}
                                        />
                                    )}
                            </li>
                        ))}
                    </ul>
                </>
            ) : (
                <div className="flex justify-center py-8">
                    <EmptyList
                        button={
                            <ApiCollectionEndpointDialog
                                apiCollectionId={apiCollectionId}
                                projectId={projectId}
                                projectVersion={projectVersion}
                                triggerNode={<Button>Create API Endpoint</Button>}
                            />
                        }
                        icon={<WorkflowIcon className="size-24 text-gray-300" />}
                        message="Get started by creating an API endpoint."
                        title="No API Endpoints"
                    />
                </div>
            )}
        </div>
    );
};

export default ApiCollectionEndpointList;
