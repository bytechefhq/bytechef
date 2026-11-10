import LoadingDots from '@/components/LoadingDots';

import AutomationProjectConfigurationDialogWorkflowsStepItem from './AutomationProjectConfigurationDialogWorkflowsStepItem';
import {ConfigurationDialogWorkflowI, WorkflowDraftI} from './automationProjectConfigurationDialog-utils';

interface AutomationProjectConfigurationDialogWorkflowsStepProps {
    configurationId?: string;
    isLoading: boolean;
    onWorkflowDraftChange: (workflowUuid: string, workflowDraft: WorkflowDraftI) => void;
    workflowDrafts: Record<string, WorkflowDraftI>;
    workflows: ConfigurationDialogWorkflowI[];
}

const AutomationProjectConfigurationDialogWorkflowsStep = ({
    configurationId,
    isLoading,
    onWorkflowDraftChange,
    workflowDrafts,
    workflows,
}: AutomationProjectConfigurationDialogWorkflowsStepProps) => {
    if (isLoading) {
        return (
            <div className="flex justify-center py-12">
                <LoadingDots />
            </div>
        );
    }

    return (
        <div className="min-h-full space-y-5">
            <p className="text-sm text-content-neutral-secondary">
                {configurationId
                    ? 'Connections and inputs set here are used by every connected user and cannot be changed by them.'
                    : 'Every workflow lets each user connect and fill. Provided connections and inputs can be set for each workflow once the configuration is created.'}
            </p>

            {workflows.length > 0 ? (
                workflows.map((workflow) => (
                    <AutomationProjectConfigurationDialogWorkflowsStepItem
                        configurationId={configurationId}
                        key={workflow.workflowUuid}
                        onWorkflowDraftChange={onWorkflowDraftChange}
                        workflow={workflow}
                        workflowDraft={workflowDrafts[workflow.workflowUuid]}
                    />
                ))
            ) : (
                <p className="text-sm text-content-neutral-secondary">This project version has no workflows.</p>
            )}
        </div>
    );
};

export default AutomationProjectConfigurationDialogWorkflowsStep;
