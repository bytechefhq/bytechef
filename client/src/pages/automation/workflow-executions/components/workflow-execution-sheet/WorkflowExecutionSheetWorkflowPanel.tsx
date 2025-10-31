import PageLoader from '@/components/PageLoader';
import {SheetCloseButton, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import WorkflowEditor from '@/pages/platform/workflow-editor/components/WorkflowEditor';
import {useWorkflowLayout} from '@/pages/platform/workflow-editor/hooks/useWorkflowLayout';
import {DEFAULT_CANVAS_WIDTH} from '@/shared/constants';
import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {ReactFlowProvider} from '@xyflow/react';
import {useEffect, useRef, useState} from 'react';

const WorkflowExecutionSheetWorkflowPanel = ({workflowExecution}: {workflowExecution: WorkflowExecution}) => {
    const {project, projectDeployment, workflow} = workflowExecution;
    const [canvasWidth, setCanvasWidth] = useState(DEFAULT_CANVAS_WIDTH);

    const rootDivRef = useRef<HTMLDivElement>(null);

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
        if (!rootDivRef.current) {
            return;
        }

        const updateWidth = () => {
            if (rootDivRef.current) {
                setCanvasWidth(rootDivRef.current.clientWidth);
            }
        };

        updateWidth();

        const resizeObserver = new ResizeObserver(updateWidth);

        resizeObserver.observe(rootDivRef.current);

        return () => resizeObserver.disconnect();
    }, []);

    return (
        <div className="flex size-full flex-col" ref={rootDivRef}>
            <SheetHeader className="flex flex-row items-center justify-between space-y-0 p-3">
                <SheetTitle>
                    <span>
                        {project?.name} / {workflow?.label}
                    </span>
                </SheetTitle>

                <SheetCloseButton />
            </SheetHeader>

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
