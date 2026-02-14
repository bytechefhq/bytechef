import PageLoader from '@/components/PageLoader';
import {useWorkflowLayout} from '@/pages/platform/workflow-editor/hooks/useWorkflowLayout';
import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {ReactFlowProvider} from '@xyflow/react';
import {lazy} from 'react';

import useWorkflowExecutionSheetWorkflowPanel from '../../hooks/useWorkflowExecutionSheetWorkflowPanel';

const WorkflowEditor = lazy(() => import('@/pages/platform/workflow-editor/components/WorkflowEditor'));

const WorkflowExecutionSheetWorkflowPanel = ({workflowExecution}: {workflowExecution: WorkflowExecution}) => {
    const {workflow} = workflowExecution;

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

    return (
        <div className="flex size-full flex-col" ref={rootDivRef}>
            <ReactFlowProvider>
                <PageLoader
                    errors={[componentsError, taskDispatcherDefinitionsError]}
                    loading={componentsIsLoading || taskDispatcherDefinitionsLoading || isWorkflowDetailsLoading}
                >
                    {componentDefinitions && taskDispatcherDefinitions && workflow && (
                        <WorkflowEditor
                            componentDefinitions={componentDefinitions}
                            customCanvasWidth={canvasWidth}
                            invalidateWorkflowQueries={() => {}}
                            readOnlyWorkflow={workflowDetails}
                            taskDispatcherDefinitions={taskDispatcherDefinitions}
                        />
                    )}
                </PageLoader>
            </ReactFlowProvider>
        </div>
    );
};

export default WorkflowExecutionSheetWorkflowPanel;
