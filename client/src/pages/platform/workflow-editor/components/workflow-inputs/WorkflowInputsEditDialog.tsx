import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import RequiredMark from '@/components/RequiredMark';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {WorkflowInputType} from '@/shared/types';
import {RefObject, useEffect, useRef} from 'react';
import {UseFormReturn, useWatch} from 'react-hook-form';

const INPUT_NAME_PATTERN = /^[a-zA-Z_][a-zA-Z0-9_]*$/;

const INPUT_NAME_MESSAGE =
    'Name must start with a letter or underscore and contain only letters, digits and underscores';

interface WorkflowInputsEditDialogProps {
    closeDialog: () => void;
    currentInputIndex?: number;
    form: UseFormReturn<WorkflowInputType, unknown, WorkflowInputType>;
    internalOnlyVisible?: boolean;
    isEditDialogOpen: boolean;
    nameInputRef: RefObject<HTMLInputElement | null>;
    openEditDialog: (index?: number) => void;
    saveWorkflowInput: (input: WorkflowInputType) => void;
}

const WorkflowInputsEditDialog = ({
    closeDialog,
    currentInputIndex,
    form,
    internalOnlyVisible,
    isEditDialogOpen,
    nameInputRef,
    openEditDialog,
    saveWorkflowInput,
}: WorkflowInputsEditDialogProps) => {
    const previousTypeRef = useRef<string | undefined>(form.getValues('type'));

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
        if (previousTypeRef.current === selectedType) {
            return;
        }

        previousTypeRef.current = selectedType;

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
                <DialogMain>
                    <DialogHeader
                        description="Add a new workflow input definition."
                        title={`${currentInputIndex === -1 ? 'Create a new' : 'Edit'} Input`}
                    />

                    <Form {...form}>
                        <form className="flex min-h-0 flex-1 flex-col" onSubmit={form.handleSubmit(saveWorkflowInput)}>
                            <DialogBody>
                                <FormField
                                    control={form.control}
                                    name="name"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel className="gap-0">
                                                Name
                                                <RequiredMark />
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
                                    rules={{
                                        pattern: {message: INPUT_NAME_MESSAGE, value: INPUT_NAME_PATTERN},
                                        required: true,
                                    }}
                                />

                                <FormField
                                    control={form.control}
                                    name="label"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel className="gap-0">
                                                Label
                                                <RequiredMark />
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
                                            <FormLabel className="gap-0">
                                                Type
                                                <RequiredMark />
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

                                {internalOnlyVisible && (
                                    <FormField
                                        control={form.control}
                                        name="internalOnly"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Internal only</FormLabel>

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
                                )}

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
                                                    <Input
                                                        {...field}
                                                        placeholder="Enter value"
                                                        type={testValueInputType}
                                                    />
                                                )}
                                            </FormControl>

                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />
                            </DialogBody>

                            <DialogFooter>
                                <DialogCancelButton />

                                <Button label="Save" type="submit" />
                            </DialogFooter>
                        </form>
                    </Form>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowInputsEditDialog;
