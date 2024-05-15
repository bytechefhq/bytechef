import {Button} from '@/components/ui/button';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {Sheet, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {WorkflowModel, WorkflowTestConfigurationModel} from '@/middleware/platform/configuration';
import {WorkflowTestApi, WorkflowTestExecutionModel} from '@/middleware/platform/workflow/test';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import WorkflowTestConfigurationDialog from '@/pages/platform/workflow-editor/components/WorkflowTestConfigurationDialog';
import {useWorkflowMutation} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import Editor from '@monaco-editor/react';
import * as SheetPrimitive from '@radix-ui/react-dialog';
import {Cross2Icon} from '@radix-ui/react-icons';
import {PlayIcon, RefreshCwIcon, SaveIcon, Settings2Icon, SquareIcon} from 'lucide-react';
import {useState} from 'react';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';

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
    const [workflowTestExecution, setWorkflowTestExecution] = useState<WorkflowTestExecutionModel>();
    const [workflowIsRunning, setWorkflowIsRunning] = useState(false);

    const {updateWorkflowMutation} = useWorkflowMutation();

    const {setWorkflowTestConfigurationDialogOpen, workflowTestConfigurationDialogOpen} = useWorkflowEditorStore();

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

    const handleWorkflowCodeEditorSheetSave = (workflow: WorkflowModel, definition: string) => {
        if (workflow && workflow.id) {
            try {
                // validate
                JSON.parse(definition);

                updateWorkflowMutation.mutate(
                    {
                        id: workflow.id,
                        workflowModel: {
                            definition,
                            version: workflow.version,
                        },
                    },
                    {
                        onError: () => setDirty(true),
                        onSuccess: () => setDirty(false),
                    }
                );
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
            <Sheet onOpenChange={handleOpenChange} open>
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
                                                    onClick={() => setWorkflowTestConfigurationDialogOpen(true)}
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

                        <ResizableHandle />

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

            {workflowTestConfigurationDialogOpen && (
                <WorkflowTestConfigurationDialog
                    onClose={() => setWorkflowTestConfigurationDialogOpen(false)}
                    workflow={workflow}
                    workflowTestConfiguration={workflowTestConfiguration}
                />
            )}
        </>
    );
};

export default WorkflowCodeEditorSheet;
