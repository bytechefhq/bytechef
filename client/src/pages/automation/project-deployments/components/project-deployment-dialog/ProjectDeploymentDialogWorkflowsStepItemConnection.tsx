import Button from '@/components/Button/Button';
import {FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {useEnvironmentStore} from '@/pages/automation/stores/useEnvironmentStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import EnvironmentBadge from '@/shared/components/EnvironmentBadge';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {ComponentConnection, ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {useCreateConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetWorkspaceConnectionsQuery,
} from '@/shared/queries/automation/connections.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {PlusIcon} from 'lucide-react';
import {Control} from 'react-hook-form';
import InlineSVG from 'react-inlinesvg';

export interface ProjectDeploymentDialogWorkflowsStepItemConnectionProps {
    control: Control<ProjectDeployment>;
    componentConnection: ComponentConnection;
    componentConnectionIndex: number;
    workflowIndex: number;
}

const ProjectDeploymentDialogWorkflowsStepItemConnection = ({
    componentConnection,
    componentConnectionIndex,
    control,
    workflowIndex,
}: ProjectDeploymentDialogWorkflowsStepItemConnectionProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: componentConnection.componentName,
        componentVersion: componentConnection.componentVersion,
    });

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({});

    const {data: connections} = useGetWorkspaceConnectionsQuery(
        {
            componentName: componentConnection.componentName,
            connectionVersion: componentDefinition?.connection?.version,
            environmentId: currentEnvironmentId,
            id: currentWorkspaceId!,
        },
        !!componentDefinition
    );

    return (
        <>
            <FormField
                control={control}
                name={`projectDeploymentWorkflows.${workflowIndex!}.connections.${componentConnectionIndex}.connectionId`}
                render={({field}) => (
                    <FormItem>
                        <FormLabel className="flex items-center space-x-1">
                            {componentDefinition?.icon && (
                                <InlineSVG className="size-4 flex-none" src={componentDefinition.icon} />
                            )}

                            <span>{componentDefinition?.title} Connection</span>

                            <span className="text-xs text-gray-500">({componentConnection.workflowNodeName})</span>
                        </FormLabel>

                        <Select onValueChange={field.onChange} value={field.value ? field.value.toString() : undefined}>
                            <FormControl>
                                <div className="flex space-x-2">
                                    <SelectTrigger>
                                        <SelectValue placeholder="Choose Connection..." />
                                    </SelectTrigger>

                                    {componentDefinitions && (
                                        <ConnectionDialog
                                            componentDefinition={componentDefinition}
                                            componentDefinitions={componentDefinitions!}
                                            connectionTagsQueryKey={ConnectionKeys.connectionTags}
                                            connectionsQueryKey={ConnectionKeys.connections}
                                            onClose={() => {}}
                                            triggerNode={
                                                <Button
                                                    icon={<PlusIcon />}
                                                    size="icon"
                                                    title="Create a new connection"
                                                    type="button"
                                                    variant="outline"
                                                />
                                            }
                                            useCreateConnectionMutation={useCreateConnectionMutation}
                                            useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                                        />
                                    )}
                                </div>
                            </FormControl>

                            <SelectContent>
                                {connections &&
                                    connections.map((connection) => (
                                        <SelectItem key={connection.id} value={connection.id!.toString()}>
                                            <div className="flex items-center space-x-1">
                                                <span>{connection.name}</span>

                                                <span className="text-xs text-gray-500">
                                                    {connection?.tags?.map((tag) => tag.name).join(', ')}
                                                </span>

                                                <EnvironmentBadge environmentId={connection.environmentId!} />
                                            </div>
                                        </SelectItem>
                                    ))}
                            </SelectContent>
                        </Select>

                        <FormDescription>
                            <span>{`Choose connection for the ${componentDefinition?.title}`}</span>

                            <span className="mx-1 text-xs text-gray-500">({componentConnection.key})</span>

                            <span>component.</span>
                        </FormDescription>

                        <FormMessage />
                    </FormItem>
                )}
                rules={{required: componentConnection.required}}
            />
        </>
    );
};

export default ProjectDeploymentDialogWorkflowsStepItemConnection;
