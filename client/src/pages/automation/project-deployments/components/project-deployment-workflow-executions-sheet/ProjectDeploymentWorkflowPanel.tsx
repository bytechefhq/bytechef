import LoadingIcon from '@/components/LoadingIcon';
import PageLoader from '@/components/PageLoader';
import useWorkflowExecutionSheetWorkflowPanel from '@/pages/automation/workflow-executions/hooks/useWorkflowExecutionSheetWorkflowPanel';
import {useWorkflowLayout} from '@/pages/platform/workflow-editor/hooks/useWorkflowLayout';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {Workflow} from '@/shared/middleware/automation/configuration';
import {ReactFlowProvider} from '@xyflow/react';
import {Suspense, lazy, useEffect, useState} from 'react';

const WorkflowEditor = lazy(() => import('@/pages/platform/workflow-editor/components/WorkflowEditor'));

interface ProjectDeploymentWorkflowPanelProps {
    workflow: Workflow;
}

const ProjectDeploymentWorkflowPanel = ({workflow}: ProjectDeploymentWorkflowPanelProps) => {
    const [ready, setReady] = useState(false);

    const {canvasWidth, rootDivRef} = useWorkflowExecutionSheetWorkflowPanel();

    const {
        componentDefinitions,
        componentsError,
        componentsIsLoading,
        taskDispatcherDefinitions,
        taskDispatcherDefinitionsError,
        taskDispatcherDefinitionsLoading,
    } = useWorkflowLayout();

    useEffect(() => {
        const frameId = requestAnimationFrame(() => {
            setReady(true);
        });

        return () => {
            cancelAnimationFrame(frameId);

            useWorkflowDataStore.getState().reset();
        };
    }, []);

    return (
        <div className="flex min-h-0 flex-1 p-3">
            <div className="flex size-full flex-col overflow-hidden rounded-md" ref={rootDivRef}>
                {ready && (
                    <ReactFlowProvider>
                        <PageLoader
                            errors={[componentsError, taskDispatcherDefinitionsError]}
                            loading={componentsIsLoading || taskDispatcherDefinitionsLoading}
                        >
                            {componentDefinitions && taskDispatcherDefinitions && (
                                <Suspense
                                    fallback={
                                        <div className="flex size-full items-center justify-center">
                                            <LoadingIcon className="size-6" />
                                        </div>
                                    }
                                >
                                    <WorkflowEditor
                                        componentDefinitions={componentDefinitions}
                                        customCanvasWidth={canvasWidth}
                                        fitViewOnLoad
                                        readOnlyWorkflow={workflow}
                                        taskDispatcherDefinitions={taskDispatcherDefinitions}
                                    />
                                </Suspense>
                            )}
                        </PageLoader>
                    </ReactFlowProvider>
                )}
            </div>
        </div>
    );
};

export default ProjectDeploymentWorkflowPanel;
