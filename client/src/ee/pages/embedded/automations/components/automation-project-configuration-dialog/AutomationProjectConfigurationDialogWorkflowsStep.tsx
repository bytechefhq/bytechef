import LoadingDots from '@/components/LoadingDots';
import compareWorkflowLabels from '@/shared/util/compareWorkflowLabels';

import AutomationProjectConfigurationDialogWorkflowsStepItem from './AutomationProjectConfigurationDialogWorkflowsStepItem';
import {ConfigurationDialogWorkflowI, WorkflowDraftI} from './automationProjectConfigurationDialog-utils';

interface AutomationProjectConfigurationDialogWorkflowsStepProps {
    configurationId?: string;
    description?: string;
    environmentId?: string;
    isLoading: boolean;
    onWorkflowDraftChange: (workflowUuid: string, workflowDraft: WorkflowDraftI) => void;
    projectId?: string;
    projectVersion?: number;
    workflowDrafts: Record<string, WorkflowDraftI>;
    workflows: ConfigurationDialogWorkflowI[];
}

const AutomationProjectConfigurationDialogWorkflowsStep = ({
    configurationId,
    description,
    environmentId,
    isLoading,
    onWorkflowDraftChange,
    projectId,
    projectVersion,
    workflowDrafts,
    workflows,
}: AutomationProjectConfigurationDialogWorkflowsStepProps) => {
    const sortedWorkflows = [...workflows].sort((workflow, otherWorkflow) =>
        compareWorkflowLabels(workflow.label, otherWorkflow.label)
    );

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
                {description ||
                    'Connections and inputs set here are used by every connected user and cannot be changed by them.'}
            </p>

            {sortedWorkflows.length > 0 ? (
                sortedWorkflows.map((workflow) => (
                    <AutomationProjectConfigurationDialogWorkflowsStepItem
                        configurationId={configurationId}
                        environmentId={environmentId}
                        key={workflow.workflowUuid}
                        onWorkflowDraftChange={onWorkflowDraftChange}
                        projectId={projectId}
                        projectVersion={projectVersion}
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
