import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import ApiCollectionEndpointDialog from '@/ee/pages/automation/api-platform/api-collections/components/ApiCollectionEndpointDialog';
import {ApiCollectionEndpoint} from '@/ee/shared/middleware/automation/api-platform';
import {useGetProjectDeploymentQuery} from '@/shared/queries/automation/projectDeployments.queries';
import {useGetProjectVersionWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {WorkflowIcon} from 'lucide-react';

import ApiCollectionEndpointListItem from './ApiCollectionEndpointListItem';

const ApiCollectionEndpointList = ({
    apiCollectionEndpoints,
    apiCollectionId,
    collectionVersion,
    contextPath,
    projectDeploymentId,
    projectId,
    projectVersion,
}: {
    apiCollectionEndpoints?: Array<ApiCollectionEndpoint>;
    apiCollectionId: number;
    collectionVersion: number;
    contextPath: string;
    projectId: number;
    projectDeploymentId: number;
    projectVersion: number;
}) => {
    const {data: workflows} = useGetProjectVersionWorkflowsQuery(projectId, projectVersion);

    const {data: projectDeployment} = useGetProjectDeploymentQuery(projectDeploymentId);

    return (
        <div className="border-b border-b-border/50 py-3 pl-4">
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
                                    projectDeployment &&
                                    projectDeployment.projectDeploymentWorkflows &&
                                    workflows && (
                                        <ApiCollectionEndpointListItem
                                            apiCollectionEndpoint={apiCollectionEndpoint}
                                            collectionVersion={collectionVersion}
                                            contextPath={contextPath}
                                            projectDeploymentId={projectDeploymentId}
                                            projectDeploymentWorkflow={
                                                projectDeployment.projectDeploymentWorkflows.find(
                                                    (projectDeploymentWorkflow) =>
                                                        projectDeploymentWorkflow.workflowUuid ===
                                                        apiCollectionEndpoint.workflowUuid
                                                )!
                                            }
                                            projectId={projectId}
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
                                collectionVersion={collectionVersion}
                                contextPath={contextPath}
                                projectId={projectId}
                                projectVersion={projectVersion}
                                triggerNode={<Button label="Create API Endpoint" />}
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
