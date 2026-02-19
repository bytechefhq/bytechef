import {Button} from '@/components/ui/button';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Form} from '@/components/ui/form';
import IntegrationInstanceConfigurationDialogWorkflowsStepItem from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItem';
import {
    ComponentConnection,
    IntegrationInstanceConfiguration,
    IntegrationInstanceConfigurationWorkflow,
    IntegrationInstanceConfigurationWorkflowConnection,
} from '@/ee/shared/middleware/embedded/configuration';
import {useUpdateIntegrationInstanceConfigurationWorkflowMutation} from '@/ee/shared/mutations/embedded/integrationInstanceConfigurations.mutations';
import {IntegrationInstanceConfigurationKeys} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useState} from 'react';
import {useForm} from 'react-hook-form';

interface IntegrationInstanceConfigurationEditWorkflowDialogProps {
    componentName: string;
    onClose?: () => void;
    integrationInstanceConfigurationWorkflow: IntegrationInstanceConfigurationWorkflow;
    workflow: Workflow;
}

const IntegrationInstanceConfigurationEditWorkflowDialog = ({
    componentName,
    integrationInstanceConfigurationWorkflow,
    onClose,
    workflow,
}: IntegrationInstanceConfigurationEditWorkflowDialogProps) => {
    const [isOpen, setIsOpen] = useState(true);

    const form = useForm<IntegrationInstanceConfiguration>({
        defaultValues: {
            integrationInstanceConfigurationWorkflows: [integrationInstanceConfigurationWorkflow],
        } as IntegrationInstanceConfiguration,
    });

    const {control, formState, getValues, handleSubmit, setValue} = form;

    const queryClient = useQueryClient();

    const updateIntegrationInstanceConfigurationWorkflowMutation =
        useUpdateIntegrationInstanceConfigurationWorkflowMutation({
            onSuccess: () => {
                queryClient.invalidateQueries({
                    queryKey: IntegrationInstanceConfigurationKeys.integrationInstanceConfigurations,
                });

                closeDialog();
            },
        });

    function closeDialog() {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }
    }

    function updateIntegrationInstanceConfigurationWorkflow() {
        const formData = getValues();

        if (!formData) {
            return;
        }

        updateIntegrationInstanceConfigurationWorkflowMutation.mutate(
            formData.integrationInstanceConfigurationWorkflows![0]
        );
    }

    useEffect(() => {
        let newIntegrationInstanceConfigurationWorkflowConnections: IntegrationInstanceConfigurationWorkflowConnection[] =
            [];

        const componentConnections: ComponentConnection[] = (workflow?.tasks ?? [])
            .flatMap((task) => task.connections ?? [])
            .concat((workflow?.triggers ?? []).flatMap((trigger) => trigger.connections ?? []))
            .filter((connection) => connection.componentName !== componentName);

        for (const componentConnection of componentConnections) {
            let integrationInstanceConfigurationWorkflowConnection =
                integrationInstanceConfigurationWorkflow?.connections?.find(
                    (integrationInstanceConfigurationWorkflowConnection) =>
                        integrationInstanceConfigurationWorkflowConnection.workflowNodeName ===
                            componentConnection.workflowNodeName &&
                        integrationInstanceConfigurationWorkflowConnection.workflowConnectionKey ===
                            componentConnection.key
                );

            if (!integrationInstanceConfigurationWorkflowConnection) {
                integrationInstanceConfigurationWorkflowConnection = {
                    /* eslint-disable @typescript-eslint/no-explicit-any */
                    connectionId: undefined as any,
                    workflowConnectionKey: componentConnection.key,
                    workflowNodeName: componentConnection.workflowNodeName,
                };
            }

            newIntegrationInstanceConfigurationWorkflowConnections = [
                ...newIntegrationInstanceConfigurationWorkflowConnections,
                integrationInstanceConfigurationWorkflowConnection!,
            ];
        }

        setValue(
            'integrationInstanceConfigurationWorkflows',
            [
                {
                    ...integrationInstanceConfigurationWorkflow,
                    connections: newIntegrationInstanceConfigurationWorkflowConnections,
                },
            ],
            {shouldValidate: true}
        );

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

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
            <DialogContent onInteractOutside={(event) => event.preventDefault()}>
                <Form {...form}>
                    <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                        <DialogTitle>{`Edit ${workflow?.label} Workflow`}</DialogTitle>

                        <DialogCloseButton />
                    </DialogHeader>

                    <IntegrationInstanceConfigurationDialogWorkflowsStepItem
                        componentName={componentName}
                        control={control}
                        formState={formState}
                        key={workflow.id!}
                        label="Enable"
                        setValue={setValue}
                        switchHidden={true}
                        workflow={workflow}
                        workflowIndex={0}
                    />

                    <DialogFooter>
                        <DialogClose asChild>
                            <Button variant="outline">Cancel</Button>
                        </DialogClose>

                        <Button onClick={handleSubmit(updateIntegrationInstanceConfigurationWorkflow)}>Save</Button>
                    </DialogFooter>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default IntegrationInstanceConfigurationEditWorkflowDialog;
