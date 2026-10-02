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
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Textarea} from '@/components/ui/textarea';
import {useForm} from 'react-hook-form';

export interface AutomationWorkflowFormValuesI {
    description: string;
    label: string;
}

interface AutomationWorkflowDialogProps {
    onClose: () => void;
    onSubmit: (values: AutomationWorkflowFormValuesI) => void;
    workflow?: {description?: string | null; label?: string | null};
}

const AutomationWorkflowDialog = ({onClose, onSubmit, workflow}: AutomationWorkflowDialogProps) => {
    const isEditMode = workflow !== undefined;

    const form = useForm<AutomationWorkflowFormValuesI>({
        defaultValues: {
            description: workflow?.description ?? '',
            label: workflow?.label ?? '',
        },
    });

    const {control, handleSubmit} = form;

    const saveWorkflow = (formValues: AutomationWorkflowFormValuesI) => {
        onSubmit(formValues);
    };

    return (
        <Dialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open
        >
            <DialogContent aria-label="Workflow Dialog" onInteractOutside={(event) => event.preventDefault()}>
                <DialogMain>
                    <Form {...form}>
                        <form className="flex min-h-0 flex-1 flex-col" onSubmit={handleSubmit(saveWorkflow)}>
                            <DialogHeader
                                description={
                                    isEditMode
                                        ? "Update the workflow's label and description."
                                        : 'Create a new workflow by filling out the form below.'
                                }
                                title={isEditMode ? 'Edit Workflow' : 'Create Workflow'}
                            />

                            <DialogBody className="flex flex-col gap-4">
                                <FormField
                                    control={control}
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
                                    control={control}
                                    name="description"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel>Description</FormLabel>

                                            <FormControl>
                                                <Textarea rows={5} {...field} />
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

export default AutomationWorkflowDialog;
