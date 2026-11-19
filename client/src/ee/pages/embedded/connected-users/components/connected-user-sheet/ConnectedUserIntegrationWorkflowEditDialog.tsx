import Button from '@/components/Button/Button';
import {Dialog, DialogBody, DialogContent, DialogFooter, DialogHeader, DialogMain} from '@/components/Dialog';
import {
    IntegrationInstance,
    IntegrationInstanceWorkflow,
    Workflow,
} from '@/ee/shared/middleware/embedded/configuration';
import {useUpdateIntegrationInstanceWorkflowMutation} from '@/ee/shared/mutations/embedded/integrationInstanceWorkflows.mutations';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {IntegrationInstanceKeys} from '@/ee/shared/queries/embedded/integrationInstances.queries';
import {WorkflowMockProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import InputConfigurationList from '@/shared/components/InputConfigurationList';
import {WorkflowInput} from '@/shared/middleware/platform/configuration';
import {useQueryClient} from '@tanstack/react-query';
import {Control, FieldValues, FormState, useForm} from 'react-hook-form';

interface WorkflowInputsFormI {
    inputs: Record<string, unknown>;
}

interface ConnectedUserIntegrationWorkflowEditDialogProps {
    integrationInstance: IntegrationInstance;
    integrationInstanceWorkflow?: IntegrationInstanceWorkflow;
    onClose: () => void;
    workflow: Workflow;
}

const ConnectedUserIntegrationWorkflowEditDialog = ({
    integrationInstance,
    integrationInstanceWorkflow,
    onClose,
    workflow,
}: ConnectedUserIntegrationWorkflowEditDialogProps) => {
    const queryClient = useQueryClient();

    const inputs = (workflow.inputs ?? []).filter((input) => !input.internalOnly) as unknown as WorkflowInput[];

    const {control, formState, handleSubmit} = useForm<WorkflowInputsFormI>({
        defaultValues: {inputs: {...(integrationInstanceWorkflow?.inputs ?? {})}},
    });

    const updateIntegrationInstanceWorkflowMutation = useUpdateIntegrationInstanceWorkflowMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({
                queryKey: ConnectedUserKeys.connectedUser(integrationInstance.connectedUserId!),
            });
            void queryClient.invalidateQueries({
                queryKey: IntegrationInstanceKeys.integrationInstance(integrationInstance.id!),
            });

            onClose();
        },
    });

    const handleSave = ({inputs: formInputs}: WorkflowInputsFormI) => {
        updateIntegrationInstanceWorkflowMutation.mutate({
            id: integrationInstance.id!,
            updateIntegrationInstanceWorkflowRequest: {
                inputs: {...(integrationInstanceWorkflow?.inputs ?? {}), ...formInputs},
            },
            workflowId: workflow.id!,
        });
    };

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open>
            <DialogContent onInteractOutside={(event) => event.preventDefault()} size="sm">
                <DialogMain>
                    <DialogHeader description={workflow.label} title="Edit Inputs" />

                    <DialogBody className="flex min-w-0 flex-col gap-4">
                        {inputs.length > 0 ? (
                            <WorkflowMockProvider>
                                <InputConfigurationList
                                    control={control as unknown as Control<FieldValues>}
                                    controlPath="inputs"
                                    formState={formState as unknown as FormState<FieldValues>}
                                    inputs={inputs}
                                    workflowId={workflow.id}
                                />
                            </WorkflowMockProvider>
                        ) : (
                            <p className="text-sm text-muted-foreground">
                                This workflow has no inputs the connected user sets. Internal-only inputs are set on the
                                integration configuration.
                            </p>
                        )}
                    </DialogBody>

                    <DialogFooter>
                        <div className="flex items-center justify-end gap-2">
                            <Button label="Cancel" onClick={onClose} type="button" variant="outline" />

                            <Button
                                disabled={inputs.length === 0 || updateIntegrationInstanceWorkflowMutation.isPending}
                                label="Save"
                                onClick={() => void handleSubmit(handleSave)()}
                                type="button"
                            />
                        </div>
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default ConnectedUserIntegrationWorkflowEditDialog;
