import Switch from '@/components/Switch/Switch';
import useAutomationWorkflowExecutionsSheetStore from '@/ee/pages/embedded/automation-workflows/stores/useAutomationWorkflowExecutionsSheetStore';
import {
    AutomationWorkflowProjectConfigurationsQuery,
    AutomationWorkflowProjectsQuery,
    useEnableAutomationWorkflowProjectConfigurationWorkflowMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

type AutomationWorkflowProjectConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];
type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];

interface AutomationWorkflowProjectConfigurationWorkflowListItemProps {
    configuration: AutomationWorkflowProjectConfigurationType;
    project: AutomationWorkflowProjectType;
    workflow: AutomationWorkflowProjectConfigurationType['workflows'][number];
}

const AutomationWorkflowProjectConfigurationWorkflowListItem = ({
    configuration,
    project,
    workflow,
}: AutomationWorkflowProjectConfigurationWorkflowListItemProps) => {
    const openAutomationWorkflowExecutionsSheet = useAutomationWorkflowExecutionsSheetStore(
        (state) => state.openAutomationWorkflowExecutionsSheet
    );

    const queryClient = useQueryClient();

    const enableWorkflowMutation = useEnableAutomationWorkflowProjectConfigurationWorkflowMutation();

    const workflowLabel = workflow.label || workflow.workflowUuid;

    const handleOpenExecutions = () => {
        const workflowTemplate = project.workflowTemplates.find(
            (template) => template.workflowUuid === workflow.workflowUuid
        );

        if (!workflowTemplate) {
            return;
        }

        openAutomationWorkflowExecutionsSheet({
            environmentId: Number(configuration.environmentId),
            projectName: project.name,
            workflow: {
                label: workflow.label,
                workflowId: workflowTemplate.workflowId,
                workflowUuid: workflow.workflowUuid,
            },
        });
    };

    return (
        <li className="flex items-center justify-between rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover">
            <button
                className="line-clamp-1 flex-1 text-start text-sm font-semibold"
                onClick={handleOpenExecutions}
                type="button"
            >
                {workflowLabel}
            </button>

            <Switch
                aria-label={`Enable ${workflowLabel}`}
                checked={workflow.enabled}
                disabled={enableWorkflowMutation.isPending}
                onCheckedChange={(enable) =>
                    enableWorkflowMutation.mutate(
                        {enable, id: configuration.id, workflowUuid: workflow.workflowUuid},
                        {
                            onSuccess: () =>
                                queryClient.invalidateQueries({
                                    queryKey: ['automationWorkflowProjectConfigurations'],
                                }),
                        }
                    )
                }
            />
        </li>
    );
};

export default AutomationWorkflowProjectConfigurationWorkflowListItem;
