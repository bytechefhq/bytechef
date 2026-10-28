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
}

const AutomationWorkflowProjectConfigurationWorkflowList = ({
    configuration,
    project,
}: AutomationWorkflowProjectConfigurationWorkflowListProps) => (
    <ul className="divide-y divide-stroke-neutral-primary">
        {configuration.workflows.map((workflow) => (
            <AutomationWorkflowProjectConfigurationWorkflowListItem
                configuration={configuration}
                key={workflow.workflowUuid}
                project={project}
                workflow={workflow}
            />
        ))}
    </ul>
);

export default AutomationWorkflowProjectConfigurationWorkflowList;
