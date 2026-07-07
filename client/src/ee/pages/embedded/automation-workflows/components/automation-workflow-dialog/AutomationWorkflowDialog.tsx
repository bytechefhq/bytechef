import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
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
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(saveWorkflow)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <div className="flex flex-col space-y-1">
                                <DialogTitle>{isEditMode ? 'Edit Workflow' : 'Create Workflow'}</DialogTitle>

                                <DialogDescription>
                                    {isEditMode
                                        ? "Update the workflow's label and description."
                                        : 'Create a new workflow by filling out the form below.'}
                                </DialogDescription>
                            </div>

                            <DialogCloseButton />
                        </DialogHeader>

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

export default AutomationWorkflowDialog;
