import EmbeddableWorkflowEditor from '@/pages/platform/workflow-editor/EmbeddableWorkflowEditor';

interface AiHubWorkflowViewerProps {
    name: string;
    projectId: string;
    projectWorkflowId: number;
}

const AiHubWorkflowViewer = ({projectId, projectWorkflowId}: AiHubWorkflowViewerProps) => {
    return (
        <div className="flex size-full flex-col">
            {projectWorkflowId > 0 ? (
                // Keyed by the workflow so the editor RE-MOUNTS when the tab re-points to a different
                // workflow (e.g. clicking a workflow artifact from another task into an already-open tab).
                // Without the re-mount, React Flow keeps the previous viewport and the new workflow opens
                // off-center. The embedded editor opts into fitViewOnWorkflowChange (see
                // EmbeddableWorkflowEditor), so each mount fits-to-view once nodes are measured.
                <EmbeddableWorkflowEditor
                    key={`${projectId}-${projectWorkflowId}`}
                    projectId={+projectId}
                    projectWorkflowId={projectWorkflowId}
                    showPublishDeploy
                />
            ) : (
                <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
                    Workflow reference unavailable.
                </div>
            )}
        </div>
    );
};

export default AiHubWorkflowViewer;
