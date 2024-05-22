import {Button} from '@/components/ui/button';
import {Dialog, DialogClose, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Form} from '@/components/ui/form';
import IntegrationInstanceConfigurationDialogWorkflowsStepItem from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationDialogWorkflowsStepItem';
import {
    IntegrationInstanceConfigurationModel,
    IntegrationInstanceConfigurationWorkflowModel,
} from '@/shared/middleware/embedded/configuration';
import {WorkflowModel} from '@/shared/middleware/platform/configuration';
import {useUpdateIntegrationInstanceConfigurationWorkflowMutation} from '@/shared/mutations/embedded/integrationInstanceConfigurations.mutations';
import {IntegrationInstanceConfigurationKeys} from '@/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {useForm} from 'react-hook-form';

interface IntegrationInstanceConfigurationEditWorkflowDialogProps {
    onClose?: () => void;
    integrationInstanceConfigurationEnabled: boolean;
    integrationInstanceConfigurationWorkflow: IntegrationInstanceConfigurationWorkflowModel;
    workflow: WorkflowModel;
}

const IntegrationInstanceConfigurationEditWorkflowDialog = ({
    integrationInstanceConfigurationEnabled,
    integrationInstanceConfigurationWorkflow,
    onClose,
    workflow,
}: IntegrationInstanceConfigurationEditWorkflowDialogProps) => {
    const [isOpen, setIsOpen] = useState(true);

    const form = useForm<IntegrationInstanceConfigurationModel>({
        defaultValues: {
            integrationInstanceConfigurationWorkflows: [integrationInstanceConfigurationWorkflow],
        } as IntegrationInstanceConfigurationModel,
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

    function updateProjectInstanceWorkflow() {
        const formData = getValues();

        if (!formData) {
            return;
        }

        updateProjectInstanceWorkflowMutation.mutate(formData.integrationInstanceConfigurationWorkflows![0]);
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
            <DialogContent onInteractOutside={(event) => event.preventDefault()}>
                <Form {...form}>
                    <DialogHeader>
                        <DialogTitle>{`Edit ${workflow?.label} Workflow`}</DialogTitle>
                    </DialogHeader>

                    <IntegrationInstanceConfigurationDialogWorkflowsStepItem
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
                            onClick={handleSubmit(updateProjectInstanceWorkflow)}
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
