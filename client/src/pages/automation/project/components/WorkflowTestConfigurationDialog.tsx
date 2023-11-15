import Properties from '@/components/Properties/Properties';
import {Button} from '@/components/ui/button';
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Form} from '@/components/ui/form';
import {WorkflowModel} from '@/middleware/helios/configuration';
import {PropertyType} from '@/types/projectTypes';
import {Cross2Icon} from '@radix-ui/react-icons';
import {useForm} from 'react-hook-form';

interface WorkflowTestConfigurationDialogProps {
    onClose: () => void;
    onWorkflowRun: (inputs: {[key: string]: object}) => void;
    workflow: WorkflowModel;
}

const WorkflowTestConfigurationDialog = ({
    onClose,
    onWorkflowRun,
    workflow,
}: WorkflowTestConfigurationDialogProps) => {
    const form = useForm<{inputs: {[key: string]: object}}>();

    const {formState, handleSubmit, register} = form;

    return (
        <Dialog onOpenChange={onClose} open={true}>
            <DialogContent className="sm:max-w-[425px]">
                <Form {...form}>
                    <form
                        onSubmit={handleSubmit((values) =>
                            onWorkflowRun(values.inputs)
                        )}
                    >
                        <DialogHeader>
                            <div className="flex items-center justify-between">
                                <DialogTitle>Configure workflow</DialogTitle>

                                <DialogClose asChild>
                                    <Button size="icon" variant="ghost">
                                        <Cross2Icon className="h-4 w-4 opacity-70" />
                                    </Button>
                                </DialogClose>
                            </div>

                            <DialogDescription>
                                Set workflow input values. Click save when you
                                are done.
                            </DialogDescription>
                        </DialogHeader>

                        <div className="grid gap-4 py-4">
                            {workflow.inputs && (
                                <Properties
                                    formState={formState}
                                    path="inputs"
                                    properties={workflow.inputs.map((input) => {
                                        if (input.type === 'string') {
                                            return {
                                                controlType: 'TEXT',
                                                type: 'STRING',
                                                ...input,
                                            } as PropertyType;
                                        } else if (input.type === 'number') {
                                            return {
                                                type: 'NUMBER',
                                                ...input,
                                            } as PropertyType;
                                        } else {
                                            return {
                                                controlType: 'SELECT',
                                                type: 'BOOLEAN',
                                                ...input,
                                            } as PropertyType;
                                        }
                                    })}
                                    register={register}
                                />
                            )}
                        </div>

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button type="button" variant="outline">
                                    Cancel
                                </Button>
                            </DialogClose>

                            <Button type="submit">Run workflow</Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowTestConfigurationDialog;
