import Switch from '@/components/Switch/Switch';
import {Label} from '@/components/ui/label';
import {useAutomationWorkflowProjectConfigurationWorkflowProvidableValuesQuery} from '@/shared/middleware/graphql';
import {WorkflowIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

import ProvidedValuesFields from '../automation-workflow-project-configuration-provided-values/ProvidedValuesFields';
import {
    ProvidedConnectionsType,
    ProvidedInputsType,
    buildProvidedValuesPayload,
    getInitialProvidedConnections,
    getInitialProvidedInputs,
} from '../automation-workflow-project-configuration-provided-values/providedValues-utils';
import {ConfigurationDialogWorkflowI, WorkflowDraftI} from './automationWorkflowProjectConfigurationDialog-utils';

interface WorkflowToggleRowProps {
    enabled: boolean;
    label: string;
    onEnabledChange: (enabled: boolean) => void;
    switchId: string;
}

const WorkflowToggleRow = ({enabled, label, onEnabledChange, switchId}: WorkflowToggleRowProps) => (
    <div className="flex items-center rounded-lg border border-stroke-neutral-secondary bg-surface-main p-2">
        <Label
            className={twMerge(
                'flex w-full cursor-pointer items-center gap-2 text-base font-normal',
                enabled && 'font-semibold'
            )}
            htmlFor={switchId}
        >
            <div
                className={twMerge(
                    'rounded-lg bg-surface-neutral-tertiary p-2',
                    enabled && 'bg-surface-brand-secondary'
                )}
            >
                <WorkflowIcon
                    className={twMerge(
                        'size-4 text-content-neutral-secondary transition-colors',
                        enabled && 'text-content-brand-primary'
                    )}
                />
            </div>

            {label}
        </Label>

        <Switch aria-label={label} checked={enabled} id={switchId} onCheckedChange={onEnabledChange} />
    </div>
);

interface AutomationWorkflowProjectConfigurationDialogWorkflowsStepItemProps {
    configurationId?: string;
    onWorkflowDraftChange: (workflowUuid: string, workflowDraft: WorkflowDraftI) => void;
    workflow: ConfigurationDialogWorkflowI;
    workflowDraft?: WorkflowDraftI;
}

const AutomationWorkflowProjectConfigurationDialogWorkflowsStepItem = ({
    configurationId,
    onWorkflowDraftChange,
    workflow,
    workflowDraft = {},
}: AutomationWorkflowProjectConfigurationDialogWorkflowsStepItemProps) => {
    const {data: providableValuesData} = useAutomationWorkflowProjectConfigurationWorkflowProvidableValuesQuery(
        {id: configurationId ?? '', workflowUuid: workflow.workflowUuid},
        {enabled: !!configurationId}
    );

    const providableValues = configurationId
        ? providableValuesData?.automationWorkflowProjectConfigurationWorkflowProvidableValues
        : undefined;

    const enabled = workflowDraft.enabled ?? workflow.enabled;
    const switchId = `configuration-workflow-${workflow.workflowUuid}`;

    const handleEnabledChange = (enabledValue: boolean) =>
        onWorkflowDraftChange(workflow.workflowUuid, {...workflowDraft, enabled: enabledValue});

    if (!providableValues) {
        return (
            <WorkflowToggleRow
                enabled={enabled}
                label={workflow.label}
                onEnabledChange={handleEnabledChange}
                switchId={switchId}
            />
        );
    }

    const providedConnections = workflowDraft.providedConnections ?? getInitialProvidedConnections(providableValues);
    const providedInputs = workflowDraft.providedInputs ?? getInitialProvidedInputs(providableValues);

    const handleProvidedValuesChange = (
        changedProvidedConnections: ProvidedConnectionsType,
        changedProvidedInputs: ProvidedInputsType
    ) =>
        onWorkflowDraftChange(workflow.workflowUuid, {
            ...workflowDraft,
            providedConnections: changedProvidedConnections,
            providedInputs: changedProvidedInputs,
            providedValuesPayload: buildProvidedValuesPayload(
                providableValues,
                changedProvidedConnections,
                changedProvidedInputs
            ),
        });

    return (
        <ProvidedValuesFields
            configurationId={configurationId!}
            enabled={enabled}
            label={workflow.label}
            onEnabledChange={handleEnabledChange}
            onProvidedConnectionsChange={(changedProvidedConnections) =>
                handleProvidedValuesChange(changedProvidedConnections, providedInputs)
            }
            onProvidedInputsChange={(changedProvidedInputs) =>
                handleProvidedValuesChange(providedConnections, changedProvidedInputs)
            }
            providableValues={providableValues}
            providedConnections={providedConnections}
            providedInputs={providedInputs}
            showWorkflowToggle
            workflowUuid={workflow.workflowUuid}
        />
    );
};

export default AutomationWorkflowProjectConfigurationDialogWorkflowsStepItem;
