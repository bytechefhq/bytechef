import PageLoader from '@/components/PageLoader';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {ReactFlowProvider} from '@xyflow/react';
import {Suspense, lazy, useCallback, useEffect, useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';

const WorkflowEditor = lazy(() => import('@/pages/platform/workflow-editor/components/WorkflowEditor'));

const WorkflowTemplatePreview = ({workflow}: {workflow: Workflow}) => {
    const [canvasWidth, setCanvasWidth] = useState(0);
    const [fittedToCanvas, setFittedToCanvas] = useState(false);

    const canvasRef = useRef<HTMLDivElement>(null);

    const {
        data: componentDefinitions,
        error: componentDefinitionsError,
        isLoading: componentDefinitionsLoading,
    } = useGetComponentDefinitionsQuery({
        actionDefinitions: true,
        clusterElementDefinitions: true,
        triggerDefinitions: true,
    });

    const {
        data: taskDispatcherDefinitions,
        error: taskDispatcherDefinitionsError,
        isLoading: taskDispatcherDefinitionsLoading,
    } = useGetTaskDispatcherDefinitionsQuery();

    const handleFitView = useCallback(() => setFittedToCanvas(true), []);

    useEffect(() => {
        const canvas = canvasRef.current;

        if (!canvas) {
            return;
        }

        const resizeObserver = new ResizeObserver(([entry]) => setCanvasWidth(entry.contentRect.width));

        resizeObserver.observe(canvas);

        return () => resizeObserver.disconnect();
    }, []);

    return (
        <div className="size-full" ref={canvasRef}>
            <PageLoader
                errors={[componentDefinitionsError, taskDispatcherDefinitionsError]}
                loading={componentDefinitionsLoading || taskDispatcherDefinitionsLoading}
            >
                {componentDefinitions && taskDispatcherDefinitions && canvasWidth > 0 && (
                    <div className={twMerge('size-full', fittedToCanvas ? 'opacity-100' : 'opacity-0')}>
                        <WorkflowReadOnlyProvider value={{useGetComponentDefinitionsQuery}}>
                            <ReactFlowProvider>
                                <Suspense>
                                    <WorkflowEditor
                                        className="bg-surface-main"
                                        componentDefinitions={componentDefinitions}
                                        customCanvasWidth={canvasWidth}
                                        onFitView={handleFitView}
                                        preview
                                        readOnlyWorkflow={workflow}
                                        taskDispatcherDefinitions={taskDispatcherDefinitions}
                                    />
                                </Suspense>
                            </ReactFlowProvider>
                        </WorkflowReadOnlyProvider>
                    </div>
                )}
            </PageLoader>
        </div>
    );
};

export default WorkflowTemplatePreview;
