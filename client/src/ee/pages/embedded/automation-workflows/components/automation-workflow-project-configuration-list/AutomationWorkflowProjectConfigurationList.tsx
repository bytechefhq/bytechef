import EmptyList from '@/components/EmptyList';
import {
    AutomationWorkflowProjectConfigurationsQuery,
    AutomationWorkflowProjectsQuery,
    useAutomationWorkflowProjectConfigurationsQuery,
    useDeleteAutomationWorkflowProjectConfigurationMutation,
} from '@/shared/middleware/graphql';
import {useGetEnvironmentsQuery} from '@/shared/queries/platform/environments.queries';
import {useQueryClient} from '@tanstack/react-query';
import {SlidersHorizontalIcon} from 'lucide-react';

import AutomationWorkflowProjectConfigurationListItem from './AutomationWorkflowProjectConfigurationListItem';

type AutomationWorkflowProjectConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];
type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];

interface AutomationWorkflowProjectConfigurationListProps {
    onEdit: (configuration: AutomationWorkflowProjectConfigurationType) => void;
    project: AutomationWorkflowProjectType;
}

const AutomationWorkflowProjectConfigurationList = ({
    onEdit,
    project,
}: AutomationWorkflowProjectConfigurationListProps) => {
    const queryClient = useQueryClient();

    const {data: environments} = useGetEnvironmentsQuery();
    const {data: configurationsData} = useAutomationWorkflowProjectConfigurationsQuery({projectId: project.id});

    const deleteConfigurationMutation = useDeleteAutomationWorkflowProjectConfigurationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['automationWorkflowProjectConfigurations']});
            queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});
        },
    });

    const configurations = configurationsData?.automationWorkflowProjectConfigurations ?? [];

    if (configurations.length === 0) {
        return (
            <div className="flex justify-center py-8">
                <EmptyList
                    icon={<SlidersHorizontalIcon className="size-24 text-stroke-neutral-tertiary" />}
                    message="Create a configuration to run a published version of this project in an environment."
                    title="No Configurations"
                />
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-2 px-3">
            {configurations.map((configuration) => (
                <AutomationWorkflowProjectConfigurationListItem
                    configuration={configuration}
                    environments={environments ?? []}
                    key={configuration.id}
                    onDelete={(configurationToDelete) =>
                        deleteConfigurationMutation.mutate({id: configurationToDelete.id})
                    }
                    onEdit={onEdit}
                    project={project}
                />
            ))}
        </div>
    );
};

export default AutomationWorkflowProjectConfigurationList;
