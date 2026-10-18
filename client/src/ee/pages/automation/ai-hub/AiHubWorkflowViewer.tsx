import EmbeddableWorkflowEditor from '@/pages/platform/workflow-editor/EmbeddableWorkflowEditor';
import {ResponseError} from '@/shared/middleware/automation/configuration';
import {useGetProjectWorkflowQuery} from '@/shared/queries/automation/projectWorkflows.queries';

interface AiHubWorkflowViewerProps {
    projectId: string;
    projectWorkflowId: number;
}

const WORKFLOW_NOT_FOUND_STATUSES = [400, 404];

const isWorkflowNotFoundError = (error: Error | null): boolean =>
    error instanceof ResponseError && WORKFLOW_NOT_FOUND_STATUSES.includes(error.response.status);

const ViewerMessage = ({message}: {message: string}) => (
    <div className="flex h-full items-center justify-center text-sm text-muted-foreground">{message}</div>
);

/**
 * The workflow tab's body. Mounts the embedded editor once the project workflow has loaded and keeps it mounted when
 * a later refetch fails transiently. Shows that the workflow no longer exists when the server reports it as not found,
 * or when it could never be loaded. The editor is keyed by the workflow, so it re-mounts when an open tab re-points to
 * another workflow of the same project.
 */
const AiHubWorkflowViewer = ({projectId, projectWorkflowId}: AiHubWorkflowViewerProps) => {
    const hasWorkflowReference = projectWorkflowId > 0;

    const {data: workflow, error: workflowError} = useGetProjectWorkflowQuery(
        +projectId,
        projectWorkflowId,
        hasWorkflowReference,
        undefined,
        {refetchOnWindowFocus: false, retry: false}
    );

    const isWorkflowMissing =
        workflowError != null && (workflow === undefined || isWorkflowNotFoundError(workflowError));

    return (
        <div className="flex size-full flex-col">
            {!hasWorkflowReference && <ViewerMessage message="Workflow reference unavailable." />}

            {hasWorkflowReference && isWorkflowMissing && (
                <ViewerMessage message="This workflow no longer exists. It may have been deleted." />
            )}

            {hasWorkflowReference && !isWorkflowMissing && workflow === undefined && (
                <ViewerMessage message="Loading workflow..." />
            )}

            {hasWorkflowReference && !isWorkflowMissing && workflow !== undefined && (
                <EmbeddableWorkflowEditor
                    key={`${projectId}-${projectWorkflowId}`}
                    projectId={+projectId}
                    projectWorkflowId={projectWorkflowId}
                    showPublishDeploy
                />
            )}
        </div>
    );
};

export default AiHubWorkflowViewer;
