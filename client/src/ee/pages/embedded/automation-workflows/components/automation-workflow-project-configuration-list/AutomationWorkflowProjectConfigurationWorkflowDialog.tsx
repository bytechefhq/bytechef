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
import {
    AutomationWorkflowProjectConfigurationsQuery,
    useAutomationWorkflowProjectConfigurationWorkflowProvidableValuesQuery,
    useUpdateAutomationWorkflowProjectConfigurationWorkflowMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

import ProvidedValuesFields from '../automation-workflow-project-configuration-provided-values/ProvidedValuesFields';
import {
    type ProvidableValuesType,
    buildProvidedValuesPayload,
    getInitialProvidedConnections,
    getInitialProvidedInputs,
} from '../automation-workflow-project-configuration-provided-values/providedValues-utils';

type AutomationWorkflowProjectConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];

interface ProvidedValuesFormProps {
    configuration: AutomationWorkflowProjectConfigurationType;
    onClose: () => void;
    providableValues: ProvidableValuesType;
    workflowUuid: string;
}

const ProvidedValuesForm = ({configuration, onClose, providableValues, workflowUuid}: ProvidedValuesFormProps) => {
    const [providedConnections, setProvidedConnections] = useState(() =>
        getInitialProvidedConnections(providableValues)
    );
    const [providedInputs, setProvidedInputs] = useState(() => getInitialProvidedInputs(providableValues));

    const queryClient = useQueryClient();

    const updateWorkflowMutation = useUpdateAutomationWorkflowProjectConfigurationWorkflowMutation();

    const handleSubmit = () => {
        updateWorkflowMutation.mutate(
            {
                id: configuration.id,
                ...buildProvidedValuesPayload(providableValues, providedConnections, providedInputs),
                workflowUuid,
            },
            {
                onSuccess: () => {
                    void queryClient.invalidateQueries({queryKey: ['automationWorkflowProjectConfigurations']});
                    void queryClient.invalidateQueries({
                        queryKey: ['automationWorkflowProjectConfigurationWorkflowProvidableValues'],
                    });

                    onClose();
                },
            }
        );
    };

    return (
        <form
            className="flex min-h-0 flex-1 flex-col"
            onSubmit={(event) => {
                event.preventDefault();

                handleSubmit();
            }}
        >
            <DialogHeader
                description="Connections and inputs set here are used by every connected user and cannot be changed by them."
                title="Provided Values"
            />

            <DialogBody>
                <ProvidedValuesFields
                    configurationId={configuration.id}
                    onProvidedConnectionsChange={setProvidedConnections}
                    onProvidedInputsChange={setProvidedInputs}
                    providableValues={providableValues}
                    providedConnections={providedConnections}
                    providedInputs={providedInputs}
                    workflowUuid={workflowUuid}
                />
            </DialogBody>

            <DialogFooter>
                <DialogCancelButton />

                <Button disabled={updateWorkflowMutation.isPending} label="Save" type="submit" />
            </DialogFooter>
        </form>
    );
};

interface AutomationWorkflowProjectConfigurationWorkflowDialogProps {
    configuration: AutomationWorkflowProjectConfigurationType;
    onClose: () => void;
    workflow: AutomationWorkflowProjectConfigurationType['workflows'][number];
}

const AutomationWorkflowProjectConfigurationWorkflowDialog = ({
    configuration,
    onClose,
    workflow,
}: AutomationWorkflowProjectConfigurationWorkflowDialogProps) => {
    const {data} = useAutomationWorkflowProjectConfigurationWorkflowProvidableValuesQuery({
        id: configuration.id,
        workflowUuid: workflow.workflowUuid,
    });

    const providableValues = data?.automationWorkflowProjectConfigurationWorkflowProvidableValues;

    return (
        <Dialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open
        >
            <DialogContent aria-label="Provided Values Dialog" onInteractOutside={(event) => event.preventDefault()}>
                <DialogMain>
                    {providableValues ? (
                        <ProvidedValuesForm
                            configuration={configuration}
                            onClose={onClose}
                            providableValues={providableValues}
                            workflowUuid={workflow.workflowUuid}
                        />
                    ) : (
                        <>
                            <DialogHeader
                                description="Connections and inputs set here are used by every connected user and cannot be changed by them."
                                title="Provided Values"
                            />

                            <DialogBody>
                                <p className="text-sm text-content-neutral-secondary">Loading...</p>
                            </DialogBody>
                        </>
                    )}
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AutomationWorkflowProjectConfigurationWorkflowDialog;
