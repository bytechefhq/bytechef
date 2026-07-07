import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import RequiredMark from '@/components/RequiredMark';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
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
import {WorkflowInputType} from '@/shared/types';
import {RefObject, useEffect} from 'react';
import {UseFormReturn, useWatch} from 'react-hook-form';

interface WorkflowInputsEditDialogProps {
    closeDialog: () => void;
    currentInputIndex?: number;
    form: UseFormReturn<WorkflowInputType, unknown, WorkflowInputType>;
    isEditDialogOpen: boolean;
    nameInputRef: RefObject<HTMLInputElement | null>;
    openEditDialog: (index?: number) => void;
    saveWorkflowInput: (input: WorkflowInputType) => void;
}

const WorkflowInputsEditDialog = ({
    closeDialog,
    currentInputIndex,
    form,
    isEditDialogOpen,
    nameInputRef,
    openEditDialog,
    saveWorkflowInput,
}: WorkflowInputsEditDialogProps) => {
    const selectedType = useWatch({control: form.control, name: 'type'});

    const testValueInputTypeMap: Record<string, string> = {
        date: 'date',
        date_time: 'datetime-local',
        integer: 'number',
        number: 'number',
        time: 'time',
    };

    const testValueInputType = (selectedType && testValueInputTypeMap[selectedType]) ?? 'text';

    useEffect(() => {
        form.setValue('testValue', '');
    }, [form, selectedType]);

    return (
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
                                    <FormLabel>
                                        Name <RequiredMark />
                                    </FormLabel>

                                    <FormControl>
                                        <Input
                                            {...field}
                                            placeholder="Input name (will be used as a dynamic value key)"
                                            readOnly={currentInputIndex !== -1}
                                            ref={nameInputRef}
                                        />
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
                                    <FormLabel>
                                        Label <RequiredMark />
                                    </FormLabel>

                                    <FormControl>
                                        <Input {...field} placeholder="Input label" />
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
                                    <FormLabel>
                                        Type <RequiredMark />
                                    </FormLabel>

                                    <FormControl>
                                        <Select onValueChange={field.onChange} value={field.value ?? ''}>
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
                                <FormItem>
                                    <FormLabel>Required</FormLabel>

                                    <FormControl>
                                        <Select
                                            onValueChange={(value) => field.onChange(value === 'true')}
                                            value={String(field.value ?? false)}
                                        >
                                            <SelectTrigger className="w-full">
                                                <SelectValue />
                                            </SelectTrigger>

                                            <SelectContent>
                                                <SelectItem value="true">True</SelectItem>

                                                <SelectItem value="false">False</SelectItem>
                                            </SelectContent>
                                        </Select>
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="testValue"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Test Value</FormLabel>

                                    <FormControl>
                                        {selectedType === 'boolean' ? (
                                            <Select
                                                onValueChange={(value) => field.onChange(value)}
                                                value={field.value ?? ''}
                                            >
                                                <SelectTrigger className="w-full">
                                                    <SelectValue placeholder="Select value" />
                                                </SelectTrigger>

                                                <SelectContent>
                                                    <SelectItem value="true">True</SelectItem>

                                                    <SelectItem value="false">False</SelectItem>
                                                </SelectContent>
                                            </Select>
                                        ) : (
                                            <Input {...field} placeholder="Enter value" type={testValueInputType} />
                                        )}
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button label="Cancel" type="button" variant="outline" />
                            </DialogClose>

                            <Button label="Save" type="submit" />
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowInputsEditDialog;
