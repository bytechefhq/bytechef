import Button from '@/components/Button/Button';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {Sheet, SheetCloseButton, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import WorkflowTestConfigurationDialog from '@/pages/platform/workflow-editor/components/WorkflowTestConfigurationDialog';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {usePersistJobId} from '@/shared/hooks/usePersistJobId';
import {useWorkflowTestStream} from '@/shared/hooks/useWorkflowTestStream';
import {Workflow, WorkflowTestConfiguration} from '@/shared/middleware/platform/configuration';
import {WorkflowTestApi, WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {getTestWorkflowAttachRequest, getTestWorkflowStreamPostRequest} from '@/shared/util/testWorkflow-utils';
import {PlayIcon, RefreshCwIcon, SaveIcon, Settings2Icon, SquareIcon} from 'lucide-react';
import {Suspense, lazy, useCallback, useEffect, useState} from 'react';

const workflowTestApi = new WorkflowTestApi();

interface WorkflowCodeEditorSheetProps {
    invalidateWorkflowQueries: () => void;
    onSheetOpenClose: (open: boolean) => void;
    runDisabled: boolean;
    sheetOpen: boolean;
    testConfigurationDisabled: boolean;
    workflow: Workflow;
    workflowTestConfiguration?: WorkflowTestConfiguration;
}

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

const WorkflowCodeEditorSheet = ({
    invalidateWorkflowQueries,
    onSheetOpenClose,
    runDisabled,
    sheetOpen,
    testConfigurationDisabled,
    workflow,
    workflowTestConfiguration,
}: WorkflowCodeEditorSheetProps) => {
    const [dirty, setDirty] = useState<boolean>(false);
    const [definition, setDefinition] = useState<string>(workflow.definition!);
    const [jobId, setJobId] = useState<string | null>(null);
    const [showCloseAlertDialog, setShowCloseAlertDialog] = useState(false);
    const [showWorkflowTestConfigurationDialog, setShowWorkflowTestConfigurationDialog] = useState(false);
    const [workflowTestExecution, setWorkflowTestExecution] = useState<WorkflowTestExecution>();
    const [workflowIsRunning, setWorkflowIsRunning] = useState(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {getPersistedJobId, persistJobId} = usePersistJobId(workflow.id, currentEnvironmentId);
    const {close: closeWorkflowTestStream, setStreamRequest} = useWorkflowTestStream({
        onError: () => {
            setWorkflowTestExecution(undefined);
            setWorkflowIsRunning(false);
            setJobId(null);
        },
        onResult: (execution) => {
            setWorkflowTestExecution(execution);
            setWorkflowIsRunning(false);
            setJobId(null);
        },
        onStart: (jobId) => setJobId(jobId),
        workflowId: workflow.id!,
    });
    const {updateWorkflowMutation} = useWorkflowEditor();

    const handleStopClick = useCallback(() => {
        setWorkflowIsRunning(false);
        setStreamRequest(null);
        closeWorkflowTestStream();

        if (jobId) {
            workflowTestApi.stopWorkflowTest({jobId}).finally(() => {
                persistJobId(null);
                setJobId(null);
            });
        }
    }, [closeWorkflowTestStream, jobId, persistJobId, setStreamRequest]);

    const handleRunClick = () => {
        setWorkflowTestExecution(undefined);
        setWorkflowIsRunning(true);
        setJobId(null);
        persistJobId(null);

        if (workflow?.id) {
            const request = getTestWorkflowStreamPostRequest({
                environmentId: currentEnvironmentId,
                id: workflow.id,
            });

            setStreamRequest(request);
        }
    };

    const handleWorkflowCodeEditorSheetSave = (workflow: Workflow, definition: string) => {
        if (workflow && workflow.id) {
            try {
                JSON.parse(definition);

                updateWorkflowMutation!.mutate(
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

                            invalidateWorkflowQueries();
                        },
                    }
                );
            } catch (error) {
                console.error(`Invalid JSON: ${error}`);
            }
        }
    };

    const handleOpenOnChange = (open: boolean) => {
        if (!open && dirty) {
            setShowCloseAlertDialog(true);
        } else {
            if (onSheetOpenClose) {
                onSheetOpenClose(open);
            }
        }
    };

    useEffect(() => {
        if (!workflow.id || currentEnvironmentId === undefined) return;

        const jobId = getPersistedJobId();

        if (!jobId) {
            return;
        }

        setWorkflowIsRunning(true);
        setJobId(jobId);

        setStreamRequest(getTestWorkflowAttachRequest({jobId}));
    }, [workflow.id, currentEnvironmentId, getPersistedJobId, setWorkflowIsRunning, setJobId, setStreamRequest]);

    return (
        <Sheet onOpenChange={handleOpenOnChange} open={sheetOpen}>
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
                                    disabled={testConfigurationDisabled}
                                    icon={<Settings2Icon />}
                                    onClick={() => setShowWorkflowTestConfigurationDialog(true)}
                                    size="icon"
                                    variant="ghost"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Set the workflow test configuration</TooltipContent>
                        </Tooltip>

                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    disabled={!dirty}
                                    icon={<SaveIcon />}
                                    onClick={() => handleWorkflowCodeEditorSheetSave(workflow, definition)}
                                    size="icon"
                                    type="submit"
                                    variant="ghost"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Save current workflow</TooltipContent>
                        </Tooltip>

                        {!workflowIsRunning && (
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <span tabIndex={0}>
                                        <Button
                                            disabled={runDisabled || dirty}
                                            icon={<PlayIcon className="text-success" />}
                                            onClick={handleRunClick}
                                            size="icon"
                                            variant="ghost"
                                        />
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
                            <Button icon={<SquareIcon />} onClick={handleStopClick} size="icon" variant="destructive" />
                        )}

                        <SheetCloseButton />
                    </div>
                </SheetHeader>

                <ResizablePanelGroup className="flex-1" direction="vertical">
                    <ResizablePanel defaultSize={75}>
                        <Suspense fallback={<MonacoEditorLoader />}>
                            <MonacoEditor
                                className="size-full"
                                defaultLanguage={workflow.format?.toLowerCase() ?? 'json'}
                                onChange={(value) => {
                                    setDefinition(value as string);

                                    if (value === workflow.definition) {
                                        setDirty(false);
                                    } else {
                                        setDirty(true);
                                    }
                                }}
                                onMount={(editor) => editor.focus()}
                                options={{
                                    folding: true,
                                    foldingStrategy: 'indentation',
                                }}
                                value={workflow.definition!}
                            />
                        </Suspense>
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

                {showWorkflowTestConfigurationDialog && (
                    <WorkflowTestConfigurationDialog
                        onClose={() => setShowWorkflowTestConfigurationDialog(false)}
                        workflow={workflow}
                        workflowTestConfiguration={workflowTestConfiguration}
                    />
                )}
            </SheetContent>

            <AlertDialog open={showCloseAlertDialog}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                        <AlertDialogDescription>
                            There are unsaved changes. This action cannot be undone.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel
                            className="shadow-none"
                            onClick={() => {
                                setShowCloseAlertDialog(false);
                            }}
                        >
                            Cancel
                        </AlertDialogCancel>

                        <AlertDialogAction
                            onClick={() => {
                                setShowCloseAlertDialog(false);

                                if (onSheetOpenClose) {
                                    onSheetOpenClose(true);
                                }
                            }}
                        >
                            Close
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </Sheet>
    );
};

export default WorkflowCodeEditorSheet;
