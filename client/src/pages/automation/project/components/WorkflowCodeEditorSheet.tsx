import {Button} from '@/components/ui/button';
import {Sheet, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {WorkflowModel} from '@/middleware/helios/configuration';
import Editor from '@monaco-editor/react';
import * as SheetPrimitive from '@radix-ui/react-dialog';
import {Cross2Icon} from '@radix-ui/react-icons';
import {PlayIcon, SaveIcon, SquareIcon} from 'lucide-react';
import {useState} from 'react';

interface WorkflowExecutionDetailsSheetProps {
    onClose: () => void;
    onRunClick: () => void;
    onSave: (definition: string) => void;
    workflow: WorkflowModel;
    workflowIsRunning: boolean;
}

const WorkflowCodeEditorSheet = ({
    onClose,
    onRunClick,
    onSave,
    workflow,
    workflowIsRunning,
}: WorkflowExecutionDetailsSheetProps) => {
    const [definition, setDefinition] = useState<string>(workflow.definition!);

    return (
        <Sheet modal={false} onOpenChange={onClose} open={true}>
            <SheetContent
                className="flex w-11/12 flex-col gap-2 p-4 sm:max-w-[800px]"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <SheetHeader>
                    <SheetTitle>
                        <div className="flex flex-1 items-center justify-between">
                            <div>Edit Workflow</div>

                            <div className="flex items-center">
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <Button
                                            onClick={() => onSave(definition)}
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
                                            <Button
                                                className="text-success hover:bg-secondary hover:text-success"
                                                onClick={onRunClick}
                                                size="icon"
                                                variant="ghost"
                                            >
                                                <PlayIcon className="h-5" />
                                            </Button>
                                        </TooltipTrigger>

                                        <TooltipContent>Run current workflow</TooltipContent>
                                    </Tooltip>
                                )}

                                {workflowIsRunning && (
                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <Button
                                                onClick={() => {
                                                    // TODO
                                                }}
                                                size="icon"
                                                variant="destructive"
                                            >
                                                <SquareIcon className="h-5" />
                                            </Button>
                                        </TooltipTrigger>

                                        <TooltipContent>Stop current workflow</TooltipContent>
                                    </Tooltip>
                                )}

                                <SheetPrimitive.Close asChild>
                                    <Button size="icon" variant="ghost">
                                        <Cross2Icon className="h-4 w-4 opacity-70" />
                                    </Button>
                                </SheetPrimitive.Close>
                            </div>
                        </div>
                    </SheetTitle>
                </SheetHeader>

                <div className="relative mt-4 flex-1">
                    <div className="absolute inset-0">
                        <Editor
                            defaultLanguage={workflow.format?.toLowerCase()}
                            defaultValue={definition}
                            onChange={(value) => setDefinition(value as string)}
                        />
                    </div>
                </div>
            </SheetContent>
        </Sheet>
    );
};

export default WorkflowCodeEditorSheet;
