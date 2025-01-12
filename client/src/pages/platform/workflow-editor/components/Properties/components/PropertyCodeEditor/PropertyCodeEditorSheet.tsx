import {Button} from '@/components/ui/button';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {Sheet, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useCopilotStore} from '@/pages/platform/copilot/stores/useCopilotStore';
import PropertyCodeEditorSheetRightPanel from '@/pages/platform/workflow-editor/components/Properties/components/PropertyCodeEditor/PropertyCodeEditorSheetRightPanel';
import {ScriptTestExecution, Workflow, WorkflowNodeScriptApi} from '@/shared/middleware/platform/configuration';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import Editor from '@monaco-editor/react';
import {PlayIcon, RefreshCwIcon, SparklesIcon, SquareIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import ReactJson from 'react-json-view';
import {twMerge} from 'tailwind-merge';

const workflowNodeScriptApi: WorkflowNodeScriptApi = new WorkflowNodeScriptApi();

interface PropertyCodeEditorSheetProps {
    language: string;
    onClose?: () => void;
    onChange: (value: string | undefined) => void;
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

    const {ai} = useApplicationInfoStore();
    const {copilotPanelOpen, setCopilotPanelOpen} = useCopilotStore();

    const ff_1570 = useFeatureFlagsStore()('ff-1570');

    const currentWorkflowTask = workflow.tasks?.find((task) => task.name === workflowNodeName);

    const handleRunClick = () => {
        setScriptIsRunning(true);

        workflowNodeScriptApi
            .testWorkflowNodeScript({
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

    useEffect(() => {
        if (value === newValue) {
            setDirty(false);
        } else {
            setDirty(true);
        }
    }, [value, newValue]);

    return (
        <>
            <Sheet modal={!copilotPanelOpen} onOpenChange={onClose} open={true}>
                <SheetContent
                    className={twMerge(
                        'flex w-11/12 flex-col gap-0 p-0 sm:max-w-screen-xl',
                        copilotPanelOpen && 'mr-[450px]'
                    )}
                    onFocusOutside={(event) => event.preventDefault()}
                    onPointerDownOutside={(event) => event.preventDefault()}
                >
                    <SheetHeader>
                        <div className="flex flex-1 items-center justify-between px-4 py-2">
                            <SheetTitle>Edit Script</SheetTitle>

                            <div className="flex items-center">
                                <div className="mr-10 flex items-center">
                                    {!scriptIsRunning && (
                                        <Tooltip>
                                            <TooltipTrigger asChild>
                                                <span tabIndex={0}>
                                                    <Button
                                                        disabled={dirty}
                                                        onClick={handleRunClick}
                                                        size="icon"
                                                        variant="ghost"
                                                    >
                                                        <PlayIcon className="h-5 text-success" />
                                                    </Button>
                                                </span>
                                            </TooltipTrigger>

                                            <TooltipContent>Run the current workflow</TooltipContent>
                                        </Tooltip>
                                    )}

                                    {scriptIsRunning && (
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

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            {ai.copilot.enabled && ff_1570 && (
                                                <Button
                                                    onClick={() =>
                                                        !copilotPanelOpen && setCopilotPanelOpen(!copilotPanelOpen)
                                                    }
                                                    size="icon"
                                                    variant="ghost"
                                                >
                                                    <SparklesIcon className="h-5" />
                                                </Button>
                                            )}
                                        </TooltipTrigger>

                                        <TooltipContent>Open Copilot panel</TooltipContent>
                                    </Tooltip>
                                </div>
                            </div>
                        </div>
                    </SheetHeader>

                    <div className="flex h-full border-y border-y-border/50">
                        <ResizablePanelGroup className="flex-1" direction="vertical">
                            <ResizablePanel defaultSize={75}>
                                <Editor
                                    defaultLanguage={language}
                                    onChange={(value) => {
                                        setNewValue(value);

                                        onChange(value);
                                    }}
                                    value={newValue}
                                />
                            </ResizablePanel>

                            <ResizableHandle />

                            <ResizablePanel defaultSize={25}>
                                <div className="relative size-full overflow-y-auto p-4">
                                    {!scriptIsRunning ? (
                                        scriptTestExecution ? (
                                            scriptTestExecution.output ? (
                                                typeof scriptTestExecution.output === 'object' ? (
                                                    <ReactJson
                                                        enableClipboard={false}
                                                        sortKeys={true}
                                                        src={scriptTestExecution.output as object}
                                                    />
                                                ) : (
                                                    <pre className="mt-2 text-xs">{scriptTestExecution.output}</pre>
                                                )
                                            ) : scriptTestExecution.error ? (
                                                <div className="space-y-4 text-sm">
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
                                workflow={workflow}
                                workflowConnections={currentWorkflowTask?.connections || []}
                                workflowNodeName={workflowNodeName}
                            />
                        </div>
                    </div>
                </SheetContent>
            </Sheet>
        </>
    );
};

export default PropertyCodeEditorSheet;
