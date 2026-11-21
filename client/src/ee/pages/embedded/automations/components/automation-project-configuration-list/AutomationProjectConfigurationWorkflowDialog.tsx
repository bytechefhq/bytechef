import WorkflowEditDialog from '@/shared/components/workflow-configuration/WorkflowEditDialog';
import {
    AutomationProjectConfigurationsQuery,
    useAutomationProjectConfigurationWorkflowProvidableValuesQuery,
    useUpdateAutomationProjectConfigurationWorkflowMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

import ProvidedValuesFields from '../automation-project-configuration-provided-values/ProvidedValuesFields';
import {
    type ProvidableValuesType,
    buildProvidedValuesPayload,
    getInitialProvidedConnections,
    getInitialProvidedInputs,
} from '../automation-project-configuration-provided-values/providedValues-utils';

type AutomationProjectConfigurationType =
    AutomationProjectConfigurationsQuery['automationProjectConfigurations'][number];

const PROVIDED_VALUES_DESCRIPTION = 'Used by every connected user, who cannot change them.';

interface ProvidedValuesFormProps {
    configuration: AutomationProjectConfigurationType;
    onClose: () => void;
    providableValues: ProvidableValuesType;
    workflowLabel: string;
    workflowUuid: string;
}

const ProvidedValuesForm = ({
    configuration,
    onClose,
    providableValues,
    workflowLabel,
    workflowUuid,
}: ProvidedValuesFormProps) => {
    const [providedConnections, setProvidedConnections] = useState(() =>
        getInitialProvidedConnections(providableValues)
    );
    const [providedInputs, setProvidedInputs] = useState(() => getInitialProvidedInputs(providableValues));

    const queryClient = useQueryClient();

    const updateWorkflowMutation = useUpdateAutomationProjectConfigurationWorkflowMutation();

    const handleSubmit = () => {
        updateWorkflowMutation.mutate(
            {
                id: configuration.id,
                ...buildProvidedValuesPayload(providableValues, providedConnections, providedInputs),
                workflowUuid,
            },
            {
                onSuccess: () => {
                    void queryClient.invalidateQueries({queryKey: ['automationProjectConfigurations']});
                    void queryClient.invalidateQueries({
                        queryKey: ['automationProjectConfigurationWorkflowProvidableValues'],
                    });

                    onClose();
                },
            }
        );
    };

    return (
        <WorkflowEditDialog
            description={PROVIDED_VALUES_DESCRIPTION}
            onClose={onClose}
            onSave={handleSubmit}
            saveDisabled={updateWorkflowMutation.isPending}
            submitOnEnter
            workflowLabel={workflowLabel}
        >
            <ProvidedValuesFields
                onProvidedConnectionsChange={setProvidedConnections}
                onProvidedInputsChange={setProvidedInputs}
                optionsScope={{configurationId: configuration.id}}
                providableValues={providableValues}
                providedConnections={providedConnections}
                providedInputs={providedInputs}
                workflowUuid={workflowUuid}
            />
        </WorkflowEditDialog>
    );
};

interface AutomationProjectConfigurationWorkflowDialogProps {
    configuration: AutomationProjectConfigurationType;
    onClose: () => void;
    workflow: AutomationProjectConfigurationType['workflows'][number];
}

const AutomationProjectConfigurationWorkflowDialog = ({
    configuration,
    onClose,
    workflow,
}: AutomationProjectConfigurationWorkflowDialogProps) => {
    const {data} = useAutomationProjectConfigurationWorkflowProvidableValuesQuery({
        id: configuration.id,
        workflowUuid: workflow.workflowUuid,
    });

    const providableValues = data?.automationProjectConfigurationWorkflowProvidableValues;

    const workflowLabel = workflow.label || workflow.workflowUuid;

    if (!providableValues) {
        return (
            <WorkflowEditDialog
                description={PROVIDED_VALUES_DESCRIPTION}
                onClose={onClose}
                onSave={() => undefined}
                saveDisabled
                workflowLabel={workflowLabel}
            >
                <p className="text-sm text-content-neutral-secondary">Loading...</p>
            </WorkflowEditDialog>
        );
    }

    return (
        <ProvidedValuesForm
            configuration={configuration}
            onClose={onClose}
            providableValues={providableValues}
            workflowLabel={workflowLabel}
            workflowUuid={workflow.workflowUuid}
        />
    );
};

export default AutomationProjectConfigurationWorkflowDialog;
