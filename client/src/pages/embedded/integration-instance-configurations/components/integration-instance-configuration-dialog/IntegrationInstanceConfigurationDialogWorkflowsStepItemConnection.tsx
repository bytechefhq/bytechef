import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {IntegrationInstanceConfiguration} from '@/shared/middleware/embedded/configuration';
import {ComponentConnection} from '@/shared/middleware/platform/configuration';
import {useCreateConnectionMutation} from '@/shared/mutations/embedded/connections.mutations';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetConnectionsQuery,
} from '@/shared/queries/embedded/connections.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {PlusIcon} from '@radix-ui/react-icons';
import * as Portal from '@radix-ui/react-portal';
import {useState} from 'react';
import {Control} from 'react-hook-form';
import InlineSVG from 'react-inlinesvg';

export interface IntegrationInstanceConfigurationDialogWorkflowsStepItemConnectionProps {
    control: Control<IntegrationInstanceConfiguration>;
    componentConnection: ComponentConnection;
    componentConnectionIndex: number;
    workflowIndex: number;
}

const IntegrationInstanceConfigurationDialogWorkflowsStepItemConnection = ({
    componentConnection,
    componentConnectionIndex,
    control,
    workflowIndex,
}: IntegrationInstanceConfigurationDialogWorkflowsStepItemConnectionProps) => {
    const [showNewConnectionDialog, setShowNewConnectionDialog] = useState(false);

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: componentConnection.componentName,
        componentVersion: componentConnection.componentVersion,
    });

    const {data: connections} = useGetConnectionsQuery(
        {
            componentName: componentConnection.componentName,
            connectionVersion: componentDefinition?.connection?.version,
        },
        !!componentDefinition
    );

    return (
        <>
            <FormField
                control={control}
                name={`integrationInstanceConfigurationWorkflows.${workflowIndex!}.connections.${componentConnectionIndex}.connectionId`}
                render={({field}) => (
                    <FormItem>
                        <FormLabel className="flex items-center">
                            {componentDefinition?.icon && (
                                <InlineSVG className="size-4 flex-none" src={componentDefinition.icon} />
                            )}

                            <span className="ml-1">{componentDefinition?.title} Connection</span>

                            <span className="ml-0.5 text-xs text-gray-500">
                                {`(${componentConnection.workflowNodeName})`}
                            </span>
                        </FormLabel>

                        <Select
                            defaultValue={field.value ? field.value.toString() : undefined}
                            onValueChange={field.onChange}
                        >
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

                                                <span className="text-xs text-gray-500">
                                                    {connection?.tags?.map((tag) => tag.name).join(', ')}
                                                </span>

                                                <Badge variant="outline">{connection.environment}</Badge>
                                            </div>
                                        </SelectItem>
                                    ))}
                            </SelectContent>
                        </Select>

                        <FormDescription>
                            {`Choose connection for the ${componentDefinition?.title}`}

                            <span className="text-xs text-gray-500">({componentConnection.key})</span>

                            {` component.`}
                        </FormDescription>

                        <FormMessage />
                    </FormItem>
                )}
                rules={{required: componentConnection.required}}
            />

            <FormField
                control={control}
                defaultValue={componentConnection.key}
                name={`integrationInstanceConfigurationWorkflows.${workflowIndex!}.connections.${componentConnectionIndex}.key`}
                render={({field}) => <input type="hidden" {...field} />}
            />

            <FormField
                control={control}
                defaultValue={componentConnection.workflowNodeName}
                name={`integrationInstanceConfigurationWorkflows.${workflowIndex!}.connections.${componentConnectionIndex}.workflowNodeName`}
                render={({field}) => <input type="hidden" {...field} />}
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

export default IntegrationInstanceConfigurationDialogWorkflowsStepItemConnection;
