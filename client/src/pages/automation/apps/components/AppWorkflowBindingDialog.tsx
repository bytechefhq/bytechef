import Button from '@/components/Button/Button';
import ComboBox from '@/components/ComboBox/ComboBox';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
    DialogTrigger,
} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {App, useAppBindableWorkflowsQuery, useBindAppWorkflowMutation} from '@/shared/middleware/graphql';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    bindingTarget: z.string().min(1, 'Workflow is required'),
    workflowKey: z
        .string()
        .regex(
            /^[a-z][a-z0-9_-]{0,63}$/,
            'Workflow key must start with a lowercase letter and contain only lowercase letters, digits, hyphens and underscores (max 64 characters)'
        ),
});

type AppWorkflowBindingFormType = z.infer<typeof formSchema>;

interface AppWorkflowBindingDialogProps {
    app: App;
    triggerNode?: ReactNode;
}

const AppWorkflowBindingDialog = ({app, triggerNode}: AppWorkflowBindingDialogProps) => {
    const [isOpen, setIsOpen] = useState(false);

    const queryClient = useQueryClient();

    const form = useForm<AppWorkflowBindingFormType>({
        defaultValues: {
            bindingTarget: '',
            workflowKey: '',
        },
        resolver: zodResolver(formSchema),
    });

    const {control, handleSubmit, reset} = form;

    const {data: bindableWorkflowsData} = useAppBindableWorkflowsQuery({appId: app.id}, {enabled: isOpen});

    const bindableWorkflows = useMemo(
        () => bindableWorkflowsData?.appBindableWorkflows ?? [],
        [bindableWorkflowsData?.appBindableWorkflows]
    );

    const bindAppWorkflowMutation = useBindAppWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['appWorkflows']});

            closeDialog();
        },
    });

    const workflowItems = useMemo(
        () =>
            bindableWorkflows.map((workflow) => ({
                label: `${workflow.workflowName} — ${workflow.projectName} (${workflow.projectDeploymentName})`,
                value: `${workflow.projectDeploymentId}:${workflow.workflowUuid}`,
            })),
        [bindableWorkflows]
    );

    function closeDialog() {
        reset();

        setIsOpen(false);
    }

    function saveBinding(values: AppWorkflowBindingFormType) {
        const separatorIndex = values.bindingTarget.indexOf(':');

        bindAppWorkflowMutation.mutate({
            appId: app.id,
            input: {
                projectDeploymentId: values.bindingTarget.substring(0, separatorIndex),
                workflowKey: values.workflowKey,
                workflowUuid: values.bindingTarget.substring(separatorIndex + 1),
            },
        });
    }

    return (
        <Dialog
            onOpenChange={(nextOpen) => {
                if (nextOpen) {
                    setIsOpen(true);
                } else {
                    closeDialog();
                }
            }}
            open={isOpen}
        >
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent aria-describedby={undefined} size="md">
                <DialogMain>
                    <DialogHeader title="Bind Workflow" />

                    <Form {...form}>
                        <form className="flex min-h-0 flex-1 flex-col" onSubmit={handleSubmit(saveBinding)}>
                            <DialogBody>
                                <fieldset className="space-y-4 border-0 p-0">
                                    <FormField
                                        control={control}
                                        name="workflowKey"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Workflow key</FormLabel>

                                                <FormControl>
                                                    <Input placeholder="approve_refund" {...field} />
                                                </FormControl>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />

                                    <FormField
                                        control={control}
                                        name="bindingTarget"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Workflow</FormLabel>

                                                <FormControl>
                                                    <ComboBox
                                                        ariaLabel="Workflow"
                                                        emptyMessage="No Call-workflow workflow is deployed in this app's workspace and environment."
                                                        items={workflowItems}
                                                        onChange={(item) => field.onChange(item?.value ?? '')}
                                                        value={field.value}
                                                    />
                                                </FormControl>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />
                                </fieldset>
                            </DialogBody>

                            <DialogFooter>
                                <DialogCancelButton />

                                <Button disabled={bindAppWorkflowMutation.isPending} label="Bind" type="submit" />
                            </DialogFooter>
                        </form>
                    </Form>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AppWorkflowBindingDialog;
