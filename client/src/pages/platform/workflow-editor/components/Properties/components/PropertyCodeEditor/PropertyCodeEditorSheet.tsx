import {Button} from '@/components/ui/button';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {Sheet, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {RightSidebar} from '@/layouts/RightSidebar';
import {ScriptTestExecutionModel, WorkflowModel, WorkflowNodeScriptApi} from '@/middleware/platform/configuration';
import PropertyCodeEditorSheetConnectionsSheet from '@/pages/platform/workflow-editor/components/Properties/components/PropertyCodeEditor/PropertyCodeEditorSheetConnectionsSheet';
import Editor from '@monaco-editor/react';
import * as SheetPrimitive from '@radix-ui/react-dialog';
import {Cross2Icon} from '@radix-ui/react-icons';
import {Link2Icon, PlayIcon, RefreshCwIcon, SquareIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import ReactJson from 'react-json-view';

const workflowNodeScriptApi: WorkflowNodeScriptApi = new WorkflowNodeScriptApi();

interface PropertyCodeEditorSheetProps {
    language: string;
    onClose?: () => void;
    onChange: (value: string | undefined) => void;
    value?: string;
    workflow: WorkflowModel;
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
    const [scriptTestExecution, setScriptTestExecution] = useState<ScriptTestExecutionModel | undefined>();
    const [showConnections, setShowConnections] = useState(false);

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
            <Sheet onOpenChange={onClose} open={true}>
                <SheetContent
                    className="flex w-11/12 flex-col gap-0 p-0 sm:max-w-screen-lg"
                    onFocusOutside={(event) => event.preventDefault()}
                    onPointerDownOutside={(event) => event.preventDefault()}
                >
                    <SheetHeader>
                        <SheetTitle>
                            <div className="flex flex-1 items-center justify-between px-4 py-2">
                                <div>Edit Script</div>

                                <div className="flex items-center">
                                    <div className="mr-4 flex items-center">
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
                                    </div>

                                    <SheetPrimitive.Close asChild>
                                        <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                                    </SheetPrimitive.Close>
                                </div>
                            </div>
                        </SheetTitle>
                    </SheetHeader>

                    <div className="flex h-full border">
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
                                                        <div className="font-semibold">Error</div>

                                                        <div>{scriptTestExecution.error.message}</div>
                                                    </div>

                                                    {scriptTestExecution.error.stackTrace && (
                                                        <div className="space-y-2">
                                                            <div className="font-semibold">Stack Trace</div>

                                                            <div>
                                                                {scriptTestExecution.error.stackTrace.map(
                                                                    (stackTrace, index) => (
                                                                        <div key={index}>{stackTrace}</div>
                                                                    )
                                                                )}
                                                            </div>
                                                        </div>
                                                    )}
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

                        <div className="flex border-l">
                            <RightSidebar
                                className="bg-transparent"
                                navigation={[
                                    {
                                        icon: Link2Icon,
                                        name: 'Connections',
                                        onClick: () => setShowConnections(!showConnections),
                                    },
                                ]}
                            />
                        </div>
                    </div>

                    {showConnections && (
                        <div className="w-80 border-l">
                            <PropertyCodeEditorSheetConnectionsSheet
                                onCLose={() => setShowConnections(false)}
                                workflow={workflow}
                                workflowConnections={currentWorkflowTask?.connections || []}
                                workflowNodeName={workflowNodeName}
                            />
                        </div>
                    )}
                </SheetContent>
            </Sheet>
        </>
    );
};

export default PropertyCodeEditorSheet;
