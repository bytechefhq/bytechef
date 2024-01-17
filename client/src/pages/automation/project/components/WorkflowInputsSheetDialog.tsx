import {Button} from '@/components/ui/button';
import {Checkbox} from '@/components/ui/checkbox';
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {WorkflowInputModel, WorkflowModel} from '@/middleware/platform/configuration';
import {useUpdateWorkflowMutation} from '@/mutations/automation/workflows.mutations';
import {ProjectKeys} from '@/queries/automation/projects.queries';
import {WorkflowDefinition} from '@/types/types';
import {Cross2Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';

export interface WorkflowInputsSheetDialogProps {
    inputIndex?: number;
    onClose?: () => void;
    projectId: number;
    triggerNode?: ReactNode;
    workflow: WorkflowModel;
}

const SPACE = 4;

const WorkflowInputsSheetDialog = ({
    inputIndex = -1,
    onClose,
    projectId,
    triggerNode,
    workflow,
}: WorkflowInputsSheetDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const form = useForm<WorkflowInputModel>({
        defaultValues: {
            ...workflow.inputs![inputIndex],
        },
    });

    const {handleSubmit, reset} = form;

    const queryClient = useQueryClient();

    const updateWorkflowMutation = useUpdateWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectKeys.projectWorkflows(projectId),
            });
        },
    });

    function closeDialog() {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }

        reset();
    }

    function handleSave(input: WorkflowInputModel) {
        const workflowDefinition: WorkflowDefinition = JSON.parse(workflow.definition!);

        let inputs: WorkflowInputModel[] = workflowDefinition.inputs ?? [];

        if (inputIndex === -1) {
            inputs = [...(inputs || []), input];
        } else {
            inputs[inputIndex] = input;
        }

        updateWorkflowMutation.mutate({
            id: workflow.id!,
            workflowModel: {
                definition: JSON.stringify(
                    {
                        ...workflowDefinition,
                        inputs,
                    },
                    null,
                    SPACE
                ),
                version: workflow.version,
            },
        });

        closeDialog();
    }

    return (
        <Dialog
            onOpenChange={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }
            }}
            open={isOpen}
        >
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent>
                <Form {...form}>
                    <form onSubmit={handleSubmit(handleSave)}>
                        <DialogHeader>
                            <div className="flex items-center justify-between">
                                <DialogTitle>{`${inputIndex === -1 ? 'Edit' : 'Create'} Input`}</DialogTitle>

                                <DialogClose asChild>
                                    <Button size="icon" variant="ghost">
                                        <Cross2Icon className="size-4 opacity-70" />
                                    </Button>
                                </DialogClose>
                            </div>

                            <DialogDescription>Use this to define a workflow input.</DialogDescription>
                        </DialogHeader>

                        <div className="grid gap-4 py-4">
                            <FormField
                                control={form.control}
                                name="name"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Name</FormLabel>

                                        <FormControl>
                                            <Input {...field} />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                                rules={{required: true}}
                            />

                            <FormField
                                control={form.control}
                                name="label"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Label</FormLabel>

                                        <FormControl>
                                            <Input {...field} />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                                rules={{required: true}}
                            />

                            <FormField
                                control={form.control}
                                name="type"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Type</FormLabel>

                                        <FormControl>
                                            <Select defaultValue={field.value} onValueChange={field.onChange}>
                                                <SelectTrigger className="w-full">
                                                    <SelectValue placeholder="Select an input type" />
                                                </SelectTrigger>

                                                <SelectContent>
                                                    <SelectItem value="boolean">Boolean</SelectItem>

                                                    <SelectItem value="date">Date</SelectItem>

                                                    <SelectItem value="date_time">Date Time</SelectItem>

                                                    <SelectItem value="integer">Integer</SelectItem>

                                                    <SelectItem value="number">Number</SelectItem>

                                                    <SelectItem value="string">String</SelectItem>

                                                    <SelectItem value="time">Time</SelectItem>
                                                </SelectContent>
                                            </Select>
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                                rules={{required: true}}
                            />

                            <FormField
                                control={form.control}
                                name="required"
                                render={({field}) => (
                                    <FormItem className="flex flex-col space-y-2">
                                        <FormLabel>Required</FormLabel>

                                        <FormControl>
                                            <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button type="button" variant="outline">
                                    Cancel
                                </Button>
                            </DialogClose>

                            <Button type="submit">Save</Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowInputsSheetDialog;
