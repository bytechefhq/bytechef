import LoadingIcon from '@/components/LoadingIcon';
import PageLoader from '@/components/PageLoader';
import useJobExecutionOverlay from '@/pages/platform/workflow-editor/hooks/useJobExecutionOverlay';
import {useWorkflowLayout} from '@/pages/platform/workflow-editor/hooks/useWorkflowLayout';
import {WorkflowExecutionOverlayContext} from '@/pages/platform/workflow-editor/providers/workflowExecutionOverlayContext';
import {preserveWorkflowDataStore} from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {useWorkflowExecutionDetailQueries} from '@/shared/components/workflow-executions/providers/workflowExecutionDetailQueriesProvider';
import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {ReactFlowProvider} from '@xyflow/react';
import {Suspense, lazy, useEffect, useState} from 'react';

import useWorkflowExecutionSheetWorkflowPanel from '../../hooks/useWorkflowExecutionSheetWorkflowPanel';

const WorkflowEditor = lazy(() => import('@/pages/platform/workflow-editor/components/WorkflowEditor'));

const WorkflowExecutionSheetWorkflowPanel = ({workflowExecution}: {workflowExecution: WorkflowExecution}) => {
    const {job, workflow} = workflowExecution;

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

    const {useGetWorkflowQuery} = useWorkflowExecutionDetailQueries();

    const {data: workflowDetails, isLoading: isWorkflowDetailsLoading} = useGetWorkflowQuery(
        workflow!.id as string,
        !!workflow?.id
    );

    const triggerName =
        workflowExecution.triggerExecution?.workflowTrigger?.name ??
        (workflowDetails && !workflowDetails.triggers?.length ? 'trigger_1' : undefined);

    const workflowExecutionOverlay = useJobExecutionOverlay(job, triggerName);

    useEffect(() => {
        setReady(false);

        const restoreWorkflowDataStore = preserveWorkflowDataStore();

        const frameId = requestAnimationFrame(() => {
            setReady(true);
        });

        return () => {
            cancelAnimationFrame(frameId);

            restoreWorkflowDataStore();
        };
    }, [workflowExecution.id]);

    return (
        <div className="flex size-full flex-col" ref={rootDivRef}>
            {ready && (
                <ReactFlowProvider>
                    <PageLoader
                        errors={[componentsError, taskDispatcherDefinitionsError]}
                        loading={componentsIsLoading || taskDispatcherDefinitionsLoading || isWorkflowDetailsLoading}
                    >
                        {componentDefinitions && taskDispatcherDefinitions && workflow && (
                            <Suspense
                                fallback={
                                    <div className="flex size-full items-center justify-center">
                                        <LoadingIcon className="size-6" />
                                    </div>
                                }
                            >
                                <WorkflowExecutionOverlayContext.Provider value={workflowExecutionOverlay}>
                                    <WorkflowEditor
                                        componentDefinitions={componentDefinitions}
                                        customCanvasWidth={canvasWidth}
                                        fitViewOnLoad
                                        readOnlyLayoutDirection="TB"
                                        readOnlyWorkflow={workflowDetails}
                                        taskDispatcherDefinitions={taskDispatcherDefinitions}
                                    />
                                </WorkflowExecutionOverlayContext.Provider>
                            </Suspense>
                        )}
                    </PageLoader>
                </ReactFlowProvider>
            )}
        </div>
    );
};

export default WorkflowExecutionSheetWorkflowPanel;
