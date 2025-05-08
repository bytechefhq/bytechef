import {Button} from '@/components/ui/button';
import {Checkbox} from '@/components/ui/checkbox';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {WorkflowInputType} from '@/shared/types';
import {UseFormReturn} from 'react-hook-form';

interface WorkflowInputsEditDialogProps {
    closeDialog: () => void;
    currentInputIndex?: number;
    form: UseFormReturn<WorkflowInputType, unknown, WorkflowInputType>;
    isEditDialogOpen: boolean;
    openEditDialog: (index?: number) => void;
    saveWorkflowInput: (input: WorkflowInputType) => void;
}

const WorkflowInputsEditDialog = ({
    closeDialog,
    currentInputIndex,
    form,
    isEditDialogOpen,
    openEditDialog,
    saveWorkflowInput,
}: WorkflowInputsEditDialogProps) => (
    <Dialog
        onOpenChange={(open) => {
            if (open) {
                openEditDialog(currentInputIndex);
            } else {
                closeDialog();
            }
        }}
        open={isEditDialogOpen}
    >
        <DialogContent>
            <Form {...form}>
                <form className="flex flex-col gap-4" onSubmit={form.handleSubmit(saveWorkflowInput)}>
                    <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                        <div className="flex flex-col space-y-1">
                            <DialogTitle>{`${currentInputIndex === -1 ? 'Create a new' : 'Edit'} Input`}</DialogTitle>

                            <DialogDescription>Add a new workflow input definition.</DialogDescription>
                        </div>

                        <DialogCloseButton />
                    </DialogHeader>

                    <FormField
                        control={form.control}
                        name="name"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>Name</FormLabel>

                                <FormControl>
                                    <Input {...field} readOnly={currentInputIndex !== -1} />
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
                                            <SelectValue placeholder="Select input type" />
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

                    <FormField
                        control={form.control}
                        name="testValue"
                        render={({field}) => (
                            <FormItem className="flex flex-col space-y-2">
                                <FormLabel>Test Value</FormLabel>

                                <FormControl>
                                    <Input {...field} />
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                    />

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

export default WorkflowInputsEditDialog;
