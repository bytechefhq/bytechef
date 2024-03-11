import {Button} from '@/components/ui/button';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {Sheet, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {WorkflowModel, WorkflowTestConfigurationModel} from '@/middleware/platform/configuration';
import {WorkflowTestApi, WorkflowTestExecutionModel} from '@/middleware/platform/workflow/test';
import {useUpdateWorkflowMutation} from '@/mutations/automation/workflows.mutations';
import WorkflowTestConfigurationDialog from '@/pages/automation/project/components/WorkflowTestConfigurationDialog';
import WorkflowExecutionDetailsAccordion from '@/pages/automation/workflow-executions/components/WorkflowExecutionDetailsAccordion';
import {WorkflowKeys} from '@/queries/automation/workflows.queries';
import Editor from '@monaco-editor/react';
import * as SheetPrimitive from '@radix-ui/react-dialog';
import {Cross2Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {PlayIcon, RefreshCwIcon, SaveIcon, Settings2Icon, SquareIcon} from 'lucide-react';
import {useState} from 'react';

const workflowTestApi = new WorkflowTestApi();

interface WorkflowCodeEditorSheetProps {
    onClose: () => void;
    runDisabled: boolean;
    testConfigurationDisabled: boolean;
    workflow: WorkflowModel;
    workflowTestConfiguration?: WorkflowTestConfigurationModel;
}

const WorkflowCodeEditorSheet = ({
    onClose,
    runDisabled,
    testConfigurationDisabled,
    workflow,
    workflowTestConfiguration,
}: WorkflowCodeEditorSheetProps) => {
    const [dirty, setDirty] = useState<boolean>(false);
    const [definition, setDefinition] = useState<string>(workflow.definition!);
    const [showWorkflowTestConfigurationDialog, setShowWorkflowTestConfigurationDialog] = useState(false);
    const [workflowTestExecution, setWorkflowTestExecution] = useState<WorkflowTestExecutionModel>();
    const [workflowIsRunning, setWorkflowIsRunning] = useState(false);

    const queryClient = useQueryClient();

    const updateWorkflowMutation = useUpdateWorkflowMutation({
        onError: () => {
            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });

            setDirty(true);
        },
        onSuccess: (workflow: WorkflowModel) => {
            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });

            setDirty(false);
        },
    });

    const handleRunClick = () => {
        setWorkflowTestExecution(undefined);
        setWorkflowIsRunning(true);

        if (workflow?.id) {
            workflowTestApi
                .testWorkflow({
                    id: workflow?.id,
                })
                .then((workflowTestExecution) => {
                    setWorkflowTestExecution(workflowTestExecution);
                    setWorkflowIsRunning(false);
                })
                .catch(() => {
                    setWorkflowIsRunning(false);

                    queryClient.invalidateQueries({
                        queryKey: WorkflowKeys.workflow(workflow.id!),
                    });
                });
        }
    };

    const handleWorkflowCodeEditorSheetSave = (workflow: WorkflowModel, definition: string) => {
        if (workflow && workflow.id) {
            try {
                // validate
                JSON.parse(definition);

                updateWorkflowMutation.mutate({
                    id: workflow.id,
                    workflowModel: {
                        definition,
                        version: workflow.version,
                    },
                });
            } catch (e) {
                //ignore
            }
        }
    };

    const handleOpenChange = () => {
        if (dirty) {
            handleWorkflowCodeEditorSheetSave(workflow, definition);
        }

        onClose();
    };

    return (
        <>
            <Sheet onOpenChange={handleOpenChange} open={true}>
                <SheetContent
                    className="flex w-11/12 flex-col gap-2 p-0 sm:max-w-screen-lg"
                    onFocusOutside={(event) => event.preventDefault()}
                    onPointerDownOutside={(event) => event.preventDefault()}
                >
                    <SheetHeader>
                        <SheetTitle>
                            <div className="flex flex-1 items-center justify-between p-4">
                                <div>Edit Workflow</div>

                                <div className="flex items-center">
                                    <div className="mr-4 flex items-center">
                                        <Tooltip>
                                            <TooltipTrigger asChild>
                                                <Button
                                                    disabled={testConfigurationDisabled}
                                                    onClick={() => setShowWorkflowTestConfigurationDialog(true)}
                                                    variant="ghost"
                                                >
                                                    <Settings2Icon className="mr-1 h-5" /> Test Configuration
                                                </Button>
                                            </TooltipTrigger>

                                            <TooltipContent>Set the workflow test configuration</TooltipContent>
                                        </Tooltip>

                                        <Tooltip>
                                            <TooltipTrigger asChild>
                                                <Button
                                                    disabled={!dirty}
                                                    onClick={() =>
                                                        handleWorkflowCodeEditorSheetSave(workflow, definition)
                                                    }
                                                    size="icon"
                                                    type="submit"
                                                    variant="ghost"
                                                >
                                                    <SaveIcon className="h-5" />
                                                </Button>
                                            </TooltipTrigger>

                                            <TooltipContent>Save current workflow</TooltipContent>
                                        </Tooltip>

                                        {!workflowIsRunning && (
                                            <Tooltip>
                                                <TooltipTrigger asChild>
                                                    <span tabIndex={0}>
                                                        <Button
                                                            disabled={runDisabled || dirty}
                                                            onClick={handleRunClick}
                                                            size="icon"
                                                            variant="ghost"
                                                        >
                                                            <PlayIcon className="h-5 text-success" />
                                                        </Button>
                                                    </span>
                                                </TooltipTrigger>

                                                <TooltipContent>
                                                    {runDisabled
                                                        ? `The workflow cannot be executed. Please set all required workflow input parameters, connections and component properties.`
                                                        : `Run the current workflow`}
                                                </TooltipContent>
                                            </Tooltip>
                                        )}

                                        {workflowIsRunning && (
                                            <Button
                                                onClick={() => {
                                                    // TODO
                                                }}
                                                size="icon"
                                                variant="destructive"
                                            >
                                                <SquareIcon className="h-5" />
                                            </Button>
                                        )}
                                    </div>

                                    <SheetPrimitive.Close asChild>
                                        <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                                    </SheetPrimitive.Close>
                                </div>
                            </div>
                        </SheetTitle>
                    </SheetHeader>

                    <ResizablePanelGroup className="flex-1" direction="vertical">
                        <ResizablePanel defaultSize={75}>
                            <Editor
                                defaultLanguage={workflow.format?.toLowerCase()}
                                onChange={(value) => {
                                    setDefinition(value as string);

                                    if (value === workflow.definition) {
                                        setDirty(false);
                                    } else {
                                        setDirty(true);
                                    }
                                }}
                                value={workflow.definition!}
                            />
                        </ResizablePanel>

                        <ResizableHandle withHandle />

                        <ResizablePanel defaultSize={25}>
                            <div className="relative size-full overflow-y-auto p-4">
                                {!workflowIsRunning ? (
                                    workflowTestExecution?.job ? (
                                        <WorkflowExecutionDetailsAccordion job={workflowTestExecution.job} />
                                    ) : (
                                        <div className="flex items-center gap-x-1 p-3 text-muted-foreground">
                                            <span>Workflow has not yet been executed.</span>
                                        </div>
                                    )
                                ) : (
                                    <div className="flex items-center gap-x-1 p-3">
                                        <span className="flex animate-spin text-gray-400">
                                            <RefreshCwIcon className="size-4" />
                                        </span>

                                        <span className="text-muted-foreground">Workflow is running...</span>
                                    </div>
                                )}
                            </div>
                        </ResizablePanel>
                    </ResizablePanelGroup>
                </SheetContent>
            </Sheet>

            {showWorkflowTestConfigurationDialog && (
                <WorkflowTestConfigurationDialog
                    onClose={() => setShowWorkflowTestConfigurationDialog(false)}
                    workflow={workflow}
                    workflowTestConfiguration={workflowTestConfiguration}
                />
            )}
        </>
    );
};

export default WorkflowCodeEditorSheet;
