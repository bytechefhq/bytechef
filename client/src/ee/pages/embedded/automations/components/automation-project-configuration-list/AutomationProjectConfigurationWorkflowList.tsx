import {AutomationProjectConfigurationsQuery, AutomationProjectsQuery} from '@/shared/middleware/graphql';

import AutomationProjectConfigurationWorkflowListItem from './AutomationProjectConfigurationWorkflowListItem';

type AutomationProjectConfigurationType =
    AutomationProjectConfigurationsQuery['automationProjectConfigurations'][number];
type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];

interface AutomationProjectConfigurationWorkflowListProps {
    configuration: AutomationProjectConfigurationType;
    project: AutomationProjectType;
    showOpenInProject?: boolean;
}

const AutomationProjectConfigurationWorkflowList = ({
    configuration,
    project,
    showOpenInProject = false,
}: AutomationProjectConfigurationWorkflowListProps) => (
    <div className="pt-3 pb-2">
        <h3 className="flex justify-start px-3 text-sm font-semibold text-content-neutral-tertiary uppercase">
            Workflows
        </h3>

        <ul className="divide-y divide-stroke-neutral-primary">
            {configuration.workflows.map((workflow) => (
                <AutomationProjectConfigurationWorkflowListItem
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

export default AutomationProjectConfigurationWorkflowList;
