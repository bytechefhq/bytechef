import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import LoadingIcon from '@/components/LoadingIcon';
import WorkflowGroupCardSkeleton from '@/shared/components/WorkflowGroupCardSkeleton';
import {
    AutomationProjectConfigurationsQuery,
    AutomationProjectsQuery,
    useAutomationProjectConfigurationsQuery,
    useDeleteAutomationProjectConfigurationMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {SlidersHorizontalIcon} from 'lucide-react';

import AutomationProjectConfigurationListItem from './AutomationProjectConfigurationListItem';

type AutomationProjectConfigurationType =
    AutomationProjectConfigurationsQuery['automationProjectConfigurations'][number];
type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];

interface AutomationProjectConfigurationListProps {
    environmentId: number;
    onCreate: () => void;
    onEdit: (configuration: AutomationProjectConfigurationType) => void;
    onUpdateVersion: (configuration: AutomationProjectConfigurationType) => void;
    project: AutomationProjectType;
    showOpenInProject?: boolean;
    showProjectName?: boolean;
}

const AutomationProjectConfigurationList = ({
    environmentId,
    onCreate,
    onEdit,
    onUpdateVersion,
    project,
    showOpenInProject = false,
    showProjectName = false,
}: AutomationProjectConfigurationListProps) => {
    const queryClient = useQueryClient();

    const {data: configurationsData, isLoading: configurationsIsLoading} = useAutomationProjectConfigurationsQuery({
        environmentId: String(environmentId),
        projectId: project.id,
    });

    const deleteConfigurationMutation = useDeleteAutomationProjectConfigurationMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['automationProjectConfigurations']});
            void queryClient.invalidateQueries({queryKey: ['automationProjects']});
        },
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

    if (configurations.length === 0) {
        return (
            <div className="flex justify-center py-8">
                <EmptyList
                    button={project.published && <Button label="Create Configuration" onClick={onCreate} />}
                    icon={<SlidersHorizontalIcon className="size-24 text-stroke-neutral-tertiary" />}
                    message={
                        project.published
                            ? 'Create a configuration to run a published version of this project in an environment.'
                            : 'Configurations run a published version of a project. Publish this project to configure it.'
                    }
                    title={project.published ? 'No Configurations' : 'Publish the project to configure it'}
                />
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-2">
            {configurations.map((configuration) => (
                <AutomationProjectConfigurationListItem
                    configuration={configuration}
                    configurations={configurations}
                    key={configuration.id}
                    onDelete={(configurationToDelete) =>
                        deleteConfigurationMutation.mutate({id: configurationToDelete.id})
                    }
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
