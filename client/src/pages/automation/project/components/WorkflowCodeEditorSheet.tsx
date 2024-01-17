import {Button} from '@/components/ui/button';
import {Sheet, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {toast} from '@/components/ui/use-toast';
import {WorkflowModel} from '@/middleware/automation/configuration';
import {useUpdateWorkflowMutation} from '@/mutations/workflows.mutations';
import {ProjectKeys} from '@/queries/projects.queries';
import Editor from '@monaco-editor/react';
import * as SheetPrimitive from '@radix-ui/react-dialog';
import {Cross2Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {SaveIcon} from 'lucide-react';
import {useState} from 'react';

interface WorkflowExecutionDetailsSheetProps {
    onClose: () => void;
    projectId: number;
    workflow: WorkflowModel;
}

const WorkflowCodeEditorSheet = ({onClose, projectId, workflow}: WorkflowExecutionDetailsSheetProps) => {
    const [dirty, setDirty] = useState<boolean>(false);
    const [definition, setDefinition] = useState<string>(workflow.definition!);

    const queryClient = useQueryClient();

    const updateWorkflowMutation = useUpdateWorkflowMutation({
        onSuccess: (workflow: WorkflowModel) => {
            queryClient.invalidateQueries({
                queryKey: ProjectKeys.projectWorkflows(projectId!),
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

                            <div className="flex items-center space-x-1">
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <Button
                                            disabled={!dirty}
                                            onClick={() => handleWorkflowCodeEditorSheetSave(definition)}
                                            size="icon"
                                            type="submit"
                                            variant="ghost"
                                        >
                                            <div className="relative">
                                                <SaveIcon className="h-5" />
                                            </div>
                                        </Button>
                                    </TooltipTrigger>

                                    <TooltipContent>Save current workflow</TooltipContent>
                                </Tooltip>

                                <SheetPrimitive.Close asChild>
                                    <Cross2Icon className="h-4 w-4 cursor-pointer opacity-70" />
                                </SheetPrimitive.Close>
                            </div>
                        </div>
                    </SheetTitle>
                </SheetHeader>

                <div className="relative mt-4 flex-1">
                    <div className="absolute inset-0">
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
                    </div>
                </div>
            </SheetContent>
        </Sheet>
    );
};

export default WorkflowCodeEditorSheet;
