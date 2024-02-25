import {Button} from '@/components/ui/button';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {Sheet, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {toast} from '@/components/ui/use-toast';
import {WorkflowModel, WorkflowTestConfigurationModel} from '@/middleware/platform/configuration';
import {useUpdateWorkflowMutation} from '@/mutations/automation/workflows.mutations';
import WorkflowTestConfigurationDialog from '@/pages/automation/project/components/WorkflowTestConfigurationDialog';
import {WorkflowKeys} from '@/queries/automation/workflows.queries';
import Editor from '@monaco-editor/react';
import * as SheetPrimitive from '@radix-ui/react-dialog';
import {Cross2Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {PlayIcon, SaveIcon, Settings2Icon, SquareIcon} from 'lucide-react';
import {useState} from 'react';

interface PropertyCodeEditorSheetProps {
    onClose?: () => void;
    onRunClick?: () => void;
    projectId?: number;
    runDisabled?: boolean;
    testConfigurationDisabled?: boolean;
    workflow: WorkflowModel;
    workflowIsRunning?: boolean;
    workflowTestConfiguration?: WorkflowTestConfigurationModel;
}

const PropertyCodeEditorSheet = ({
    onClose,
    onRunClick,
    projectId,
    runDisabled,
    testConfigurationDisabled,
    workflow,
    workflowIsRunning,
    workflowTestConfiguration,
}: PropertyCodeEditorSheetProps) => {
    const [dirty, setDirty] = useState<boolean>(false);
    const [definition, setDefinition] = useState<string>(workflow.definition!);
    const [showWorkflowTestConfigurationDialog, setShowWorkflowTestConfigurationDialog] = useState(false);

    const queryClient = useQueryClient();

    const updateWorkflowMutation = useUpdateWorkflowMutation({
        onSuccess: (workflow: WorkflowModel) => {
            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.projectWorkflows(projectId!),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });

            setDirty(false);

            toast({
                description: `The workflow ${workflow.label} is saved.`,
            });
        },
    });

    const handleWorkflowCodeEditorSheetSave = (definition: string) => {
        if (workflow && workflow.id) {
            updateWorkflowMutation.mutate({
                id: workflow.id,
                workflowModel: {
                    definition,
                    version: workflow.version,
                },
            });
        }
    };

    return (
        <>
            <Sheet onOpenChange={onClose} open={true}>
                <SheetContent
                    className="flex w-11/12 flex-col gap-2 p-0 sm:max-w-[1024px]"
                    onFocusOutside={(event) => event.preventDefault()}
                    onPointerDownOutside={(event) => event.preventDefault()}
                >
                    <SheetHeader>
                        <SheetTitle>
                            <div className="flex flex-1 items-center justify-between p-4">
                                <div>Edit Script</div>

                                <div className="flex items-center">
                                    <div className="mr-4 flex items-center space-x-0.5">
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
                                                    onClick={() => handleWorkflowCodeEditorSheetSave(definition)}
                                                    size="icon"
                                                    type="submit"
                                                    variant="ghost"
                                                >
                                                    <SaveIcon className="h-5" />
                                                </Button>
                                            </TooltipTrigger>

                                            <TooltipContent>Save current workflow</TooltipContent>
                                        </Tooltip>

                                        {!workflowIsRunning && runDisabled && (
                                            <Tooltip>
                                                <TooltipTrigger asChild>
                                                    <span tabIndex={0}>
                                                        <Button disabled={runDisabled} size="icon" variant="ghost">
                                                            <PlayIcon className="h-5 text-success" />
                                                        </Button>
                                                    </span>
                                                </TooltipTrigger>

                                                <TooltipContent>
                                                    The workflow cannot be executed. Please set all required workflow
                                                    input parameters, connections and component properties.
                                                </TooltipContent>
                                            </Tooltip>
                                        )}

                                        {!workflowIsRunning && !runDisabled && (
                                            <Tooltip>
                                                <TooltipTrigger asChild>
                                                    <span tabIndex={0}>
                                                        <Button
                                                            disabled={runDisabled}
                                                            onClick={onRunClick}
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
                            <div className="flex h-full items-center justify-center p-6">
                                <span className="font-semibold">Content</span>
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

export default PropertyCodeEditorSheet;
