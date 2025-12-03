import Button from '@/components/Button/Button';
import LoadingDots from '@/components/LoadingDots';
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
import PropertyCodeEditorSheetRightPanel from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/PropertyCodeEditorSheetRightPanel';
import {getTask} from '@/pages/platform/workflow-editor/utils/getTask';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import CopilotButton from '@/shared/components/copilot/CopilotButton';
import {Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {ScriptTestExecution, Workflow, WorkflowNodeScriptApi} from '@/shared/middleware/platform/configuration';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {PlayIcon, RefreshCwIcon, SaveIcon, SquareIcon} from 'lucide-react';
import {Suspense, lazy, useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));
const ReactJson = lazy(() => import('react-json-view'));

const workflowNodeScriptApi: WorkflowNodeScriptApi = new WorkflowNodeScriptApi();

interface PropertyCodeEditorSheetProps {
    language: string;
    onChange: (value: string | undefined) => void;
    onClose?: () => void;
    value?: string;
    workflow: Workflow;
    workflowNodeName: string;
}

const PropertyCodeEditorSheet = ({
    language,
    onChange,
    onClose,
    value,
    workflow,
    workflowNodeName,
}: PropertyCodeEditorSheetProps) => {
    const [dirty, setDirty] = useState<boolean>(false);
    const [newValue, setNewValue] = useState<string | undefined>(value);
    const [scriptIsRunning, setScriptIsRunning] = useState(false);
    const [scriptTestExecution, setScriptTestExecution] = useState<ScriptTestExecution | undefined>();
    const [showCloseAlertDialog, setShowCloseAlertDialog] = useState(false);

    const copilotPanelOpen = useCopilotStore((state) => state.copilotPanelOpen);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const currentWorkflowTask = getTask({
        tasks: workflow.tasks || [],
        workflowNodeName: workflowNodeName,
    });

    const handleRunClick = () => {
        setScriptIsRunning(true);

        workflowNodeScriptApi
            .testWorkflowNodeScript({
                environmentId: currentEnvironmentId!,
                id: workflow!.id!,
                workflowNodeName,
            })
            .then((scriptTestExecution) => {
                setScriptTestExecution(scriptTestExecution);
                setScriptIsRunning(false);
            })
            .catch(() => {
                setScriptIsRunning(false);
            });
    };

    const handleOpenOnChange = (open: boolean) => {
        if (!open && dirty) {
            setShowCloseAlertDialog(true);
        } else {
            if (onClose) {
                onClose();
            }
        }
    };

    useEffect(() => {
        if (value === newValue) {
            setDirty(false);
        } else {
            setDirty(true);
        }
    }, [value, newValue]);

    return (
        <>
            <Sheet modal={!copilotPanelOpen} onOpenChange={handleOpenOnChange} open={true}>
                <SheetContent
                    className={twMerge('flex flex-col gap-0 p-0 sm:max-w-[1200px]', copilotPanelOpen && 'mr-[460px]')}
                    onFocusOutside={(event) => event.preventDefault()}
                    onPointerDownOutside={(event) => event.preventDefault()}
                >
                    <SheetHeader className="flex flex-row items-center justify-between space-y-0 border-b border-b-border/50 p-3">
                        <SheetTitle>Edit Script</SheetTitle>

                        <div className="flex items-center gap-1">
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        disabled={!dirty}
                                        icon={<SaveIcon />}
                                        onClick={() => onChange(newValue)}
                                        size="icon"
                                        type="submit"
                                        variant="ghost"
                                    />
                                </TooltipTrigger>

                                <TooltipContent>Save current workflow</TooltipContent>
                            </Tooltip>

                            {!scriptIsRunning && (
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <span tabIndex={0}>
                                            <Button
                                                disabled={dirty}
                                                icon={<PlayIcon className="text-success" />}
                                                onClick={handleRunClick}
                                                size="icon"
                                                variant="ghost"
                                            />
                                        </span>
                                    </TooltipTrigger>

                                    <TooltipContent>Run the current workflow</TooltipContent>
                                </Tooltip>
                            )}

                            {scriptIsRunning && (
                                <Button
                                    icon={<SquareIcon />}
                                    onClick={() => {
                                        // TODO
                                    }}
                                    size="icon"
                                    variant="destructive"
                                />
                            )}

                            <CopilotButton parameters={{language}} source={Source.CODE_EDITOR} />

                            <SheetCloseButton />
                        </div>
                    </SheetHeader>

                    <div className="flex h-full">
                        <ResizablePanelGroup className="flex-1" direction="vertical">
                            <ResizablePanel defaultSize={75}>
                                <Suspense fallback={<MonacoEditorLoader />}>
                                    <MonacoEditor
                                        className="size-full"
                                        defaultLanguage={language}
                                        onChange={(value) => {
                                            setNewValue(value);
                                        }}
                                        onMount={(editor) => {
                                            editor.focus();
                                        }}
                                        value={newValue}
                                    />
                                </Suspense>
                            </ResizablePanel>

                            <ResizableHandle className="bg-muted" />

                            <ResizablePanel defaultSize={25}>
                                <div className="relative size-full overflow-y-auto p-4 text-sm">
                                    {!scriptIsRunning ? (
                                        scriptTestExecution ? (
                                            scriptTestExecution.output ? (
                                                typeof scriptTestExecution.output === 'object' ? (
                                                    <Suspense fallback={<LoadingDots />}>
                                                        <ReactJson
                                                            enableClipboard={false}
                                                            sortKeys={true}
                                                            src={scriptTestExecution.output as object}
                                                        />
                                                    </Suspense>
                                                ) : (
                                                    <pre className="mt-2 text-xs">{scriptTestExecution.output}</pre>
                                                )
                                            ) : scriptTestExecution.error ? (
                                                <div className="space-y-4">
                                                    <div className="space-y-2">
                                                        <div className="font-semibold text-destructive">Error</div>

                                                        <div>{scriptTestExecution.error.message}</div>
                                                    </div>
                                                </div>
                                            ) : (
                                                <span className="text-muted-foreground">No defined output.</span>
                                            )
                                        ) : (
                                            <div className="flex items-center gap-x-1 text-muted-foreground">
                                                <span>The script has not yet been executed.</span>
                                            </div>
                                        )
                                    ) : (
                                        <div className="flex items-center gap-x-1">
                                            <span className="flex animate-spin text-gray-400">
                                                <RefreshCwIcon className="size-4" />
                                            </span>

                                            <span className="text-muted-foreground">Script is running...</span>
                                        </div>
                                    )}
                                </div>
                            </ResizablePanel>
                        </ResizablePanelGroup>

                        <div className="flex border-l border-l-border/50">
                            <PropertyCodeEditorSheetRightPanel
                                componentConnections={currentWorkflowTask?.connections || []}
                                workflow={workflow}
                                workflowNodeName={workflowNodeName}
                            />
                        </div>
                    </div>
                </SheetContent>
            </Sheet>

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

                                if (onClose) {
                                    onClose();
                                }
                            }}
                        >
                            Close
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </>
    );
};

export default PropertyCodeEditorSheet;
