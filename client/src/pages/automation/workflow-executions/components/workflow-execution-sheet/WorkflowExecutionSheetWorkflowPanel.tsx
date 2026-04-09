import PageLoader from '@/components/PageLoader';
import {useWorkflowLayout} from '@/pages/platform/workflow-editor/hooks/useWorkflowLayout';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {ReactFlowProvider} from '@xyflow/react';
import {lazy, useEffect, useState} from 'react';

import useWorkflowExecutionSheetWorkflowPanel from '../../hooks/useWorkflowExecutionSheetWorkflowPanel';

const WorkflowEditor = lazy(() => import('@/pages/platform/workflow-editor/components/WorkflowEditor'));

const WorkflowExecutionSheetWorkflowPanel = ({workflowExecution}: {workflowExecution: WorkflowExecution}) => {
    const {workflow} = workflowExecution;

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

    const {data: workflowDetails, isLoading: isWorkflowDetailsLoading} = useGetWorkflowQuery(
        workflow!.id as string,
        !!workflow?.id
    );

    useEffect(() => {
        setReady(false);

        const frameId = requestAnimationFrame(() => {
            setReady(true);
        });

        return () => {
            cancelAnimationFrame(frameId);

            useWorkflowDataStore.getState().reset();
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
                            <WorkflowEditor
                                componentDefinitions={componentDefinitions}
                                customCanvasWidth={canvasWidth}
                                readOnlyWorkflow={workflowDetails}
                                taskDispatcherDefinitions={taskDispatcherDefinitions}
                            />
                        )}
                    </PageLoader>
                </ReactFlowProvider>
            )}
        </div>
    );
};

export default WorkflowExecutionSheetWorkflowPanel;
