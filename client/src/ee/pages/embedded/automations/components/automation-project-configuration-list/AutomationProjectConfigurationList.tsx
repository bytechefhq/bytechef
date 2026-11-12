import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import LoadingIcon from '@/components/LoadingIcon';
import PublishButton from '@/shared/components/PublishButton';
import WorkflowGroupCardSkeleton from '@/shared/components/WorkflowGroupCardSkeleton';
import {
    AutomationProjectConfigurationsQuery,
    AutomationProjectsQuery,
    useAutomationProjectConfigurationsQuery,
} from '@/shared/middleware/graphql';
import {AlertTriangleIcon, SendIcon, Settings2Icon} from 'lucide-react';

import AutomationProjectConfigurationListItem from './AutomationProjectConfigurationListItem';
import {getEnvironmentName} from './automationProjectConfigurationList-utils';

type AutomationProjectConfigurationType =
    AutomationProjectConfigurationsQuery['automationProjectConfigurations'][number];
type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];

interface AutomationProjectConfigurationListProps {
    environmentId: number;
    onCreate: () => void;
    onEdit: (configuration: AutomationProjectConfigurationType) => void;
    onPublishProject?: (projectId: string) => void;
    onUpdateVersion: (configuration: AutomationProjectConfigurationType) => void;
    project: AutomationProjectType;
    showOpenInProject?: boolean;
    showProjectName?: boolean;
}

const AutomationProjectConfigurationList = ({
    environmentId,
    onCreate,
    onEdit,
    onPublishProject,
    onUpdateVersion,
    project,
    showOpenInProject = false,
    showProjectName = false,
}: AutomationProjectConfigurationListProps) => {
    const {
        data: configurationsData,
        error: configurationsError,
        isLoading: configurationsIsLoading,
        refetch: refetchConfigurations,
    } = useAutomationProjectConfigurationsQuery({
        environmentId: String(environmentId),
        projectId: project.id,
    });

    const configurations = configurationsData?.automationProjectConfigurations ?? [];

    if (configurationsIsLoading && project.configurationCount === 0) {
        return (
            <div className="flex items-center justify-center py-6">
                <LoadingIcon />
            </div>
        );
    }

    if (configurationsIsLoading) {
        return (
            <WorkflowGroupCardSkeleton
                label={`Loading ${project.name} configurations`}
                workflowCount={project.workflowTemplates?.length ?? 1}
            />
        );
    }

    if (configurationsError && !configurationsData) {
        return (
            <EmptyState
                actions={<Button label="Retry" onClick={() => void refetchConfigurations()} variant="outline" />}
                description="The configurations of this project could not be loaded. Please try again."
                icon={<AlertTriangleIcon />}
                title="Failed to load configurations"
            />
        );
    }

    if (configurations.length === 0) {
        return project.published ? (
            <EmptyState
                actions={<Button icon={<Settings2Icon />} label="Configure" onClick={onCreate} />}
                description="Configure this project to run its workflows here."
                icon={<Settings2Icon />}
                title={`No configurations in ${getEnvironmentName(environmentId)}`}
            />
        ) : (
            <EmptyState
                actions={
                    onPublishProject && (
                        <PublishButton
                            disabledReason={
                                project.workflowTemplates?.length
                                    ? undefined
                                    : 'Add a workflow before publishing the project.'
                            }
                            onClick={() => onPublishProject(project.id)}
                        />
                    )
                }
                description="Configurations run a published, read-only version of a project. Publish this project to create its first version."
                icon={<SendIcon />}
                title="Publish the project to configure it"
            />
        );
    }

    return (
        <div className="flex flex-col gap-2">
            {configurations.map((configuration) => (
                <AutomationProjectConfigurationListItem
                    configuration={configuration}
                    configurations={configurations}
                    key={configuration.id}
                    onEdit={onEdit}
                    onUpdateVersion={onUpdateVersion}
                    project={project}
                    showOpenInProject={showOpenInProject}
                    showProjectName={showProjectName}
                />
            ))}
        </div>
    );
};

export default AutomationProjectConfigurationList;
