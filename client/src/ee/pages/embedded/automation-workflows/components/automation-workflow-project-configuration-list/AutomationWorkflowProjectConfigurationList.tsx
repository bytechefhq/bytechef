import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import LoadingIcon from '@/components/LoadingIcon';
import {Skeleton} from '@/components/ui/skeleton';
import {
    AutomationWorkflowProjectConfigurationsQuery,
    AutomationWorkflowProjectsQuery,
    useAutomationWorkflowProjectConfigurationsQuery,
    useDeleteAutomationWorkflowProjectConfigurationMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {SlidersHorizontalIcon} from 'lucide-react';

import AutomationWorkflowProjectConfigurationListItem from './AutomationWorkflowProjectConfigurationListItem';

type AutomationWorkflowProjectConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];
type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];

interface AutomationWorkflowProjectConfigurationListProps {
    environmentId: number;
    onCreate: () => void;
    onEdit: (configuration: AutomationWorkflowProjectConfigurationType) => void;
    onUpdateVersion: (configuration: AutomationWorkflowProjectConfigurationType) => void;
    project: AutomationWorkflowProjectType;
    showProjectName?: boolean;
}

const AutomationWorkflowProjectConfigurationList = ({
    environmentId,
    onCreate,
    onEdit,
    onUpdateVersion,
    project,
    showProjectName = false,
}: AutomationWorkflowProjectConfigurationListProps) => {
    const queryClient = useQueryClient();

    const {data: configurationsData, isLoading: configurationsIsLoading} =
        useAutomationWorkflowProjectConfigurationsQuery({
            environmentId: String(environmentId),
            projectId: project.id,
        });

    const deleteConfigurationMutation = useDeleteAutomationWorkflowProjectConfigurationMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['automationWorkflowProjectConfigurations']});
            void queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});
        },
    });

    const configurations = configurationsData?.automationWorkflowProjectConfigurations ?? [];

    if (configurationsIsLoading && project.configurationCount === 0) {
        return (
            <div className="flex items-center justify-center py-6">
                <LoadingIcon />
            </div>
        );
    }

    if (configurationsIsLoading) {
        return (
            <ul aria-busy="true" aria-label={`Loading ${project.name} configurations`} className="flex flex-col gap-2">
                {Array.from({length: Math.min(project.configurationCount, 3)}, (_, index) => (
                    <li className="flex items-center justify-between rounded border border-border/50 p-3" key={index}>
                        <div className="space-y-2">
                            <Skeleton className="h-5 w-40" />

                            <Skeleton className="h-4 w-24" />
                        </div>

                        <div className="flex items-center gap-4">
                            <Skeleton className="h-6 w-9 rounded-full" />

                            <Skeleton className="h-6 w-11 rounded-full" />
                        </div>
                    </li>
                ))}
            </ul>
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
                <AutomationWorkflowProjectConfigurationListItem
                    configuration={configuration}
                    configurations={configurations}
                    key={configuration.id}
                    onDelete={(configurationToDelete) =>
                        deleteConfigurationMutation.mutate({id: configurationToDelete.id})
                    }
                    onEdit={onEdit}
                    onUpdateVersion={onUpdateVersion}
                    project={project}
                    showProjectName={showProjectName}
                />
            ))}
        </div>
    );
};

export default AutomationWorkflowProjectConfigurationList;
