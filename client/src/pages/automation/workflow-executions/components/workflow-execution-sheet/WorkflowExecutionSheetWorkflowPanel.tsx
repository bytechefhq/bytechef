import PageLoader from '@/components/PageLoader';
import {Badge} from '@/components/ui/badge';
import {SheetCloseButton, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import WorkflowEditor from '@/pages/platform/workflow-editor/components/WorkflowEditor';
import {useWorkflowLayout} from '@/pages/platform/workflow-editor/hooks/useWorkflowLayout';
import {WorkflowMutationProvider} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {useUpdateWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import useUpdatePlatformWorkflowMutation from '@/shared/mutations/platform/workflows.mutations';
import {WorkflowKeys, useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {ReactFlowProvider} from '@xyflow/react';
import {useEffect, useRef, useState} from 'react';

const WorkflowExecutionSheetWorkflowPanel = ({workflowExecution}: {workflowExecution: WorkflowExecution}) => {
    const {project, projectDeployment, workflow} = workflowExecution;
    const [canvasWidth, setCanvasWidth] = useState(670);

    const rootDivRef = useRef<HTMLDivElement>(null);

    const {
        componentDefinitions,
        componentsError,
        componentsIsLoading,
        taskDispatcherDefinitions,
        taskDispatcherDefinitionsError,
        taskDispatcherDefinitionsLoading,
    } = useWorkflowLayout();

    const updateWorkflowEditorMutation = useUpdatePlatformWorkflowMutation({
        useUpdateWorkflowMutation,
        workflowId: workflow?.id as string,
        workflowKeys: WorkflowKeys,
    });

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
                        {project?.name}/{workflow?.label}/
                    </span>

                    <Badge variant="secondary">{projectDeployment?.environment}</Badge>
                </SheetTitle>

                <SheetCloseButton />
            </SheetHeader>

            <WorkflowMutationProvider
                value={{
                    updateWorkflowMutation: updateWorkflowEditorMutation,
                }}
            >
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
            </WorkflowMutationProvider>
        </div>
    );
};

export default WorkflowExecutionSheetWorkflowPanel;
