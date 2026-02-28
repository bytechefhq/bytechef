import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
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
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {PlusIcon} from 'lucide-react';
import {Control, UseFormSetValue} from 'react-hook-form';
import InlineSVG from 'react-inlinesvg';

type ConnectionGroupingType = {
    indices: number[];
    setValue: UseFormSetValue<ProjectDeployment>;
};

export interface ProjectDeploymentDialogWorkflowsStepItemConnectionProps {
    componentConnection: ComponentConnection;
    componentConnectionIndex: number;
    connectionGrouping?: ConnectionGroupingType;
    control: Control<ProjectDeployment>;
    workflowIndex: number;
    workflowNodeLabel?: string;
}

const ProjectDeploymentDialogWorkflowsStepItemConnection = ({
    componentConnection,
    componentConnectionIndex,
    connectionGrouping,
    control,
    workflowIndex,
    workflowNodeLabel,
}: ProjectDeploymentDialogWorkflowsStepItemConnectionProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data: componentDefinition, isPending: isComponentDefinitionPending} = useGetComponentDefinitionQuery({
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
        <FormField
            control={control}
            name={`projectDeploymentWorkflows.${workflowIndex!}.connections.${componentConnectionIndex}.connectionId`}
            render={({field}) => (
                <FormItem>
                    <FormLabel className="flex items-center space-x-1">
                        {componentDefinition?.icon && (
                            <InlineSVG className="size-4 flex-none" src={componentDefinition.icon} />
                        )}

                        <span>
                            {workflowNodeLabel ||
                                (isComponentDefinitionPending
                                    ? 'Loading…'
                                    : `${componentDefinition?.title ?? 'Component'} Connection`)}
                        </span>

                        {!connectionGrouping && (
                            <span className="text-xs text-gray-500">({componentConnection.workflowNodeName})</span>
                        )}
                    </FormLabel>

                    <Select
                        disabled={isComponentDefinitionPending}
                        onValueChange={(value) => {
                            field.onChange(Number(value));

                            if (connectionGrouping) {
                                for (const index of connectionGrouping.indices) {
                                    if (index !== componentConnectionIndex) {
                                        connectionGrouping.setValue(
                                            `projectDeploymentWorkflows.${workflowIndex}.connections.${index}.connectionId`,
                                            Number(value)
                                        );
                                    }
                                }
                            }
                        }}
                        value={field.value ? field.value.toString() : undefined}
                    >
                        <FormControl>
                            <div className="flex space-x-2">
                                <SelectTrigger>
                                    {isComponentDefinitionPending ? (
                                        <div className="flex items-center gap-2">
                                            <LoadingIcon />

                                            <span className="text-muted-foreground">Loading connection…</span>
                                        </div>
                                    ) : (
                                        <SelectValue placeholder="Choose Connection..." />
                                    )}
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
                        {isComponentDefinitionPending ? (
                            <span>Loading component details…</span>
                        ) : componentDefinition?.title ? (
                            <>
                                <span>{`Choose connection for the ${componentDefinition.title}`}</span>

                                {connectionGrouping ? (
                                    <span className="mx-1 text-xs text-gray-500">
                                        (applies to {connectionGrouping.indices.length} nodes)
                                    </span>
                                ) : (
                                    <span className="mx-1 text-xs text-gray-500">({componentConnection.key})</span>
                                )}

                                <span> component.</span>
                            </>
                        ) : (
                            <span>Choose a connection for this component.</span>
                        )}
                    </FormDescription>

                    <FormMessage />
                </FormItem>
            )}
            rules={{required: componentConnection.required}}
        />
    );
};

export default ProjectDeploymentDialogWorkflowsStepItemConnection;
