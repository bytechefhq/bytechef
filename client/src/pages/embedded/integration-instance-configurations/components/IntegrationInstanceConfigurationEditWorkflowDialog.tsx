import {Button} from '@/components/ui/button';
import {Dialog, DialogClose, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Form} from '@/components/ui/form';
import IntegrationInstanceConfigurationDialogWorkflowsStepItem from '@/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItem';
import {
    IntegrationInstanceConfiguration,
    IntegrationInstanceConfigurationWorkflow,
    IntegrationInstanceConfigurationWorkflowConnection,
    WorkflowConnection,
} from '@/shared/middleware/embedded/configuration';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {useUpdateIntegrationInstanceConfigurationWorkflowMutation} from '@/shared/mutations/embedded/integrationInstanceConfigurations.mutations';
import {IntegrationInstanceConfigurationKeys} from '@/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useState} from 'react';
import {useForm} from 'react-hook-form';

interface IntegrationInstanceConfigurationEditWorkflowDialogProps {
    componentName: string;
    onClose?: () => void;
    integrationInstanceConfigurationEnabled: boolean;
    integrationInstanceConfigurationWorkflow: IntegrationInstanceConfigurationWorkflow;
    workflow: Workflow;
}

const IntegrationInstanceConfigurationEditWorkflowDialog = ({
    componentName,
    integrationInstanceConfigurationEnabled,
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

    const updateProjectInstanceWorkflowMutation = useUpdateIntegrationInstanceConfigurationWorkflowMutation({
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

        updateProjectInstanceWorkflowMutation.mutate(formData.integrationInstanceConfigurationWorkflows![0]);
    }

    useEffect(() => {
        let newIntegrationInstanceConfigurationWorkflowConnections: IntegrationInstanceConfigurationWorkflowConnection[] =
            [];

        const workflowConnections: WorkflowConnection[] = (workflow?.tasks ?? [])
            .flatMap((task) => task.connections ?? [])
            .concat((workflow?.triggers ?? []).flatMap((trigger) => trigger.connections ?? []));

        for (const workflowConnection of workflowConnections) {
            let integrationInstanceConfigurationWorkflowConnection =
                integrationInstanceConfigurationWorkflow?.connections?.find(
                    (integrationInstanceConfigurationWorkflowConnection) =>
                        integrationInstanceConfigurationWorkflowConnection.workflowNodeName ===
                            workflowConnection.workflowNodeName &&
                        integrationInstanceConfigurationWorkflowConnection.key === workflowConnection.key
                );

            if (!integrationInstanceConfigurationWorkflowConnection) {
                integrationInstanceConfigurationWorkflowConnection = {
                    /* eslint-disable @typescript-eslint/no-explicit-any */
                    connectionId: undefined as any,
                    key: workflowConnection.key,
                    workflowNodeName: workflowConnection.workflowNodeName,
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
                    <DialogHeader>
                        <DialogTitle>{`Edit ${workflow?.label} Workflow`}</DialogTitle>
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

                        <Button
                            disabled={integrationInstanceConfigurationEnabled}
                            onClick={handleSubmit(updateIntegrationInstanceConfigurationWorkflow)}
                        >
                            Save
                        </Button>
                    </DialogFooter>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default IntegrationInstanceConfigurationEditWorkflowDialog;
