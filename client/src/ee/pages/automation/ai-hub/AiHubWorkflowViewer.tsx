import EmbeddableWorkflowEditor from '@/pages/platform/workflow-editor/EmbeddableWorkflowEditor';
import {Workflow, WorkflowApi} from '@/shared/middleware/automation/configuration';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {useQuery} from '@tanstack/react-query';

interface AiHubWorkflowViewerProps {
    projectId: string;
    projectWorkflowId: number;
}

const ViewerMessage = ({message}: {message: string}) => (
    <div className="flex h-full items-center justify-center text-sm text-muted-foreground">{message}</div>
);

const AiHubWorkflowViewer = ({projectId, projectWorkflowId}: AiHubWorkflowViewerProps) => {
    const hasWorkflowReference = projectWorkflowId > 0;

    const {isError: isWorkflowMissing, isPending: isWorkflowPending} = useQuery<Workflow, Error>({
        enabled: hasWorkflowReference,
        queryFn: () => new WorkflowApi().getProjectWorkflow({projectWorkflowId}),
        queryKey: ProjectWorkflowKeys.projectWorkflow(+projectId, projectWorkflowId),
        refetchOnWindowFocus: false,
        retry: false,
    });

    return (
        <div className="flex size-full flex-col">
            {!hasWorkflowReference && <ViewerMessage message="Workflow reference unavailable." />}

            {hasWorkflowReference && isWorkflowMissing && (
                <ViewerMessage message="This workflow no longer exists. It may have been deleted." />
            )}

            {hasWorkflowReference && !isWorkflowMissing && isWorkflowPending && (
                <ViewerMessage message="Loading workflow..." />
            )}

            {hasWorkflowReference && !isWorkflowMissing && !isWorkflowPending && (
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
