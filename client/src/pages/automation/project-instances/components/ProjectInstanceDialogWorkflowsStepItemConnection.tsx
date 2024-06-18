import {Button} from '@/components/ui/button';
import {FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ConnectionDialog from '@/pages/platform/connection/components/ConnectionDialog';
import {ProjectInstanceModel, WorkflowConnectionModel} from '@/shared/middleware/automation/configuration';
import {useCreateConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetWorkspaceConnectionsQuery,
} from '@/shared/queries/automation/connections.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {PlusIcon} from '@radix-ui/react-icons';
import * as Portal from '@radix-ui/react-portal';
import {useState} from 'react';
import {Control} from 'react-hook-form';
import InlineSVG from 'react-inlinesvg';

export interface ProjectInstanceDialogWorkflowsStepItemConnectionProps {
    control: Control<ProjectInstanceModel>;
    workflowConnection: WorkflowConnectionModel;
    workflowConnectionIndex: number;
    workflowIndex: number;
}

const ProjectInstanceDialogWorkflowsStepItemConnection = ({
    control,
    workflowConnection,
    workflowConnectionIndex,
    workflowIndex,
}: ProjectInstanceDialogWorkflowsStepItemConnectionProps) => {
    const [showNewConnectionDialog, setShowNewConnectionDialog] = useState(false);

    const {currentWorkspaceId} = useWorkspaceStore();

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: workflowConnection.componentName,
        componentVersion: workflowConnection.componentVersion,
    });

    const {data: connections} = useGetWorkspaceConnectionsQuery(
        {
            componentName: workflowConnection.componentName,
            connectionVersion: componentDefinition?.connection?.version,
            id: currentWorkspaceId!,
        },
        !!componentDefinition
    );

    return (
        <>
            <FormField
                control={control}
                name={`projectInstanceWorkflows.${workflowIndex!}.connections.${workflowConnectionIndex}.connectionId`}
                render={({field}) => (
                    <FormItem>
                        <FormLabel className="flex items-center">
                            {componentDefinition?.icon && (
                                <InlineSVG className="size-4 flex-none" src={componentDefinition.icon} />
                            )}

                            <span className="ml-1">{componentDefinition?.title} Connection</span>

                            <span className="ml-0.5 text-xs text-gray-500">
                                {`(${workflowConnection.workflowNodeName})`}
                            </span>
                        </FormLabel>

                        <Select onValueChange={field.onChange} value={field.value ? field.value.toString() : ''}>
                            <FormControl>
                                <div className="flex space-x-2">
                                    <SelectTrigger>
                                        <SelectValue placeholder="Choose Connection..." />
                                    </SelectTrigger>

                                    <Button
                                        className="mt-auto p-2"
                                        onClick={() => setShowNewConnectionDialog(true)}
                                        title="Create a new connection"
                                        type="button"
                                        variant="outline"
                                    >
                                        <PlusIcon className="size-5" />
                                    </Button>
                                </div>
                            </FormControl>

                            <SelectContent>
                                {connections &&
                                    connections.map((connection) => (
                                        <SelectItem key={connection.id} value={connection.id!.toString()}>
                                            <div className="flex items-center space-x-1">
                                                <span>{connection.name}</span>

                                                <span className="text-xs text-gray-500">{connection.environment}</span>

                                                <span className="text-xs text-gray-500">
                                                    {connection?.tags?.map((tag) => tag.name).join(', ')}
                                                </span>
                                            </div>
                                        </SelectItem>
                                    ))}
                            </SelectContent>
                        </Select>

                        <FormDescription>
                            {`Choose connection for the ${componentDefinition?.title}`}

                            <span className="text-xs text-gray-500">({workflowConnection.key})</span>

                            {` component.`}
                        </FormDescription>

                        <FormMessage />
                    </FormItem>
                )}
                rules={{required: workflowConnection.required}}
            />
            {showNewConnectionDialog && (
                <Portal.Root>
                    <ConnectionDialog
                        componentDefinition={componentDefinition}
                        connectionTagsQueryKey={ConnectionKeys.connectionTags}
                        connectionsQueryKey={ConnectionKeys.connections}
                        onClose={() => setShowNewConnectionDialog(false)}
                        useCreateConnectionMutation={useCreateConnectionMutation}
                        useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                    />
                </Portal.Root>
            )}
        </>
    );
};

export default ProjectInstanceDialogWorkflowsStepItemConnection;
