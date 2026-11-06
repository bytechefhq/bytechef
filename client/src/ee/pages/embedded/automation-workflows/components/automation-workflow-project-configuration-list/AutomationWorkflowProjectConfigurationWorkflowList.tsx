import {
    AutomationWorkflowProjectConfigurationsQuery,
    AutomationWorkflowProjectsQuery,
} from '@/shared/middleware/graphql';

import AutomationWorkflowProjectConfigurationWorkflowListItem from './AutomationWorkflowProjectConfigurationWorkflowListItem';

type AutomationWorkflowProjectConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];
type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];

interface AutomationWorkflowProjectConfigurationWorkflowListProps {
    configuration: AutomationWorkflowProjectConfigurationType;
    project: AutomationWorkflowProjectType;
    showOpenInProject?: boolean;
}

const AutomationWorkflowProjectConfigurationWorkflowList = ({
    configuration,
    project,
    showOpenInProject = false,
}: AutomationWorkflowProjectConfigurationWorkflowListProps) => (
    <div className="pt-3 pb-2">
        <h3 className="flex justify-start px-3 text-sm font-semibold text-content-neutral-tertiary uppercase">
            Workflows
        </h3>

        <ul className="divide-y divide-stroke-neutral-primary">
            {configuration.workflows.map((workflow) => (
                <AutomationWorkflowProjectConfigurationWorkflowListItem
                    configuration={configuration}
                    key={workflow.workflowUuid}
                    project={project}
                    showOpenInProject={showOpenInProject}
                    workflow={workflow}
                    workflowTemplate={project.workflowTemplates.find(
                        (workflowTemplate) => workflowTemplate.workflowUuid === workflow.workflowUuid
                    )}
                />
            ))}
        </ul>
    </div>
);

export default AutomationWorkflowProjectConfigurationWorkflowList;
