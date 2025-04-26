import {Button} from '@/components/ui/button';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {Sheet, SheetCloseButton, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import WorkflowTestConfigurationDialog from '@/pages/platform/workflow-editor/components/WorkflowTestConfigurationDialog';
import {useWorkflowMutation} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {Workflow, WorkflowTestConfiguration} from '@/shared/middleware/platform/configuration';
import {WorkflowTestApi, WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import Editor from '@monaco-editor/react';
import {useQueryClient} from '@tanstack/react-query';
import {PlayIcon, RefreshCwIcon, SaveIcon, Settings2Icon, SquareIcon} from 'lucide-react';
import {useState} from 'react';
import {useParams} from 'react-router-dom';

const workflowTestApi = new WorkflowTestApi();

interface WorkflowCodeEditorSheetProps {
    onSheetOpenClose: (open: boolean) => void;
    runDisabled: boolean;
    sheetOpen: boolean;
    testConfigurationDisabled: boolean;
    workflow: Workflow;
    workflowTestConfiguration?: WorkflowTestConfiguration;
}

const WorkflowCodeEditorSheet = ({
    onSheetOpenClose,
    runDisabled,
    sheetOpen,
    testConfigurationDisabled,
    workflow,
    workflowTestConfiguration,
}: WorkflowCodeEditorSheetProps) => {
    const [dirty, setDirty] = useState<boolean>(false);
    const [definition, setDefinition] = useState<string>(workflow.definition!);
    const [workflowTestExecution, setWorkflowTestExecution] = useState<WorkflowTestExecution>();
    const [workflowIsRunning, setWorkflowIsRunning] = useState(false);
    const [showWorkflowTestConfigurationDialog, setShowWorkflowTestConfigurationDialog] = useState(false);

    const {updateWorkflowMutation} = useWorkflowMutation();

    const {projectId, projectWorkflowId} = useParams();

    const queryClient = useQueryClient();

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
                    setWorkflowTestExecution(undefined);
                });
        }
    };

    const handleWorkflowCodeEditorSheetSave = (workflow: Workflow, definition: string) => {
        if (workflow && workflow.id) {
            try {
                // validate
                JSON.parse(definition);

                updateWorkflowMutation.mutate(
                    {
                        id: workflow.id,
                        workflow: {
                            definition,
                            version: workflow.version,
                        },
                    },
                    {
                        onError: () => setDirty(true),
                        onSuccess: () => {
                            setDirty(false);

                            queryClient.invalidateQueries({
                                queryKey: ProjectWorkflowKeys.projectWorkflow(+projectId!, +projectWorkflowId!),
                            });
                        },
                    }
                );
                /* eslint-disable @typescript-eslint/no-unused-vars */
            } catch (e) {
                //ignore
            }
        }
    };

    const handleOpenChange = (open: boolean) => {
        if (dirty) {
            handleWorkflowCodeEditorSheetSave(workflow, definition);
        }

        onSheetOpenClose(open);
    };

    return (
        <>
            <Sheet onOpenChange={handleOpenChange} open={sheetOpen}>
                <SheetContent
                    className="flex w-11/12 flex-col gap-0 p-0 sm:max-w-screen-lg"
                    onFocusOutside={(event) => event.preventDefault()}
                    onPointerDownOutside={(event) => event.preventDefault()}
                >
                    <SheetHeader className="flex flex-row items-center justify-between space-y-0 border-b border-b-border/50 p-3">
                        <SheetTitle>Edit Workflow</SheetTitle>

                        <div className="flex items-center gap-1">
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        className="[&_svg]:size-5"
                                        disabled={testConfigurationDisabled}
                                        onClick={() => setShowWorkflowTestConfigurationDialog(true)}
                                        size="icon"
                                        variant="ghost"
                                    >
                                        <Settings2Icon />
                                    </Button>
                                </TooltipTrigger>

                                <TooltipContent>Set the workflow test configuration</TooltipContent>
                            </Tooltip>

                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        className="[&_svg]:size-5"
                                        disabled={!dirty}
                                        onClick={() => handleWorkflowCodeEditorSheetSave(workflow, definition)}
                                        size="icon"
                                        type="submit"
                                        variant="ghost"
                                    >
                                        <SaveIcon />
                                    </Button>
                                </TooltipTrigger>

                                <TooltipContent>Save current workflow</TooltipContent>
                            </Tooltip>

                            {!workflowIsRunning && (
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <span tabIndex={0}>
                                            <Button
                                                className="[&_svg]:size-5"
                                                disabled={runDisabled || dirty}
                                                onClick={handleRunClick}
                                                size="icon"
                                                variant="ghost"
                                            >
                                                <PlayIcon className="text-success" />
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

                            <SheetCloseButton />
                        </div>
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
                                options={{
                                    folding: true,
                                    foldingStrategy: 'indentation',
                                }}
                                value={workflow.definition!}
                            />
                        </ResizablePanel>

                        <ResizableHandle className="bg-muted" />

                        <ResizablePanel defaultSize={30}>
                            {workflowIsRunning ? (
                                <div className="flex items-center gap-x-1 p-3">
                                    <span className="flex animate-spin text-gray-400">
                                        <RefreshCwIcon className="size-4" />
                                    </span>

                                    <span className="text-muted-foreground">Workflow is running...</span>
                                </div>
                            ) : (
                                <WorkflowExecutionsTestOutput
                                    resizablePanelSize={40}
                                    workflowIsRunning={workflowIsRunning}
                                    workflowTestExecution={workflowTestExecution}
                                />
                            )}
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
