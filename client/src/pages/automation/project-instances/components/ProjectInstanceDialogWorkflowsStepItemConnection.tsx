import {Button} from '@/components/ui/button';
import {FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {ProjectInstanceModel, WorkflowConnectionModel} from '@/middleware/automation/configuration';
import ConnectionDialog from '@/pages/automation/connections/components/ConnectionDialog';
import {useGetConnectionsQuery} from '@/queries/automation/connections.queries';
import {useGetComponentDefinitionQuery} from '@/queries/platform/componentDefinitions.queries';
import * as Portal from '@radix-ui/react-portal';
import {PlusIcon} from 'lucide-react';
import {useState} from 'react';
import {Control} from 'react-hook-form';

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

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: workflowConnection.componentName,
        componentVersion: workflowConnection.componentVersion,
    });
    const {data: connections} = useGetConnectionsQuery(
        {
            componentName: workflowConnection.componentName,
            connectionVersion: componentDefinition?.connection?.version,
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
                        <FormLabel>
                            {`${componentDefinition?.title} `}

                            <span className="text-xs text-gray-500">({workflowConnection.key})</span>
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
                                            <div className="flex items-center">
                                                <span className="mr-1 ">{connection.name}</span>

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

            <FormField
                control={control}
                defaultValue={workflowConnection.key}
                name={`projectInstanceWorkflows.${workflowIndex!}.connections.${workflowConnectionIndex}.key`}
                render={({field}) => <input type="hidden" {...field} />}
            />

            <FormField
                control={control}
                defaultValue={workflowConnection.operationName}
                name={`projectInstanceWorkflows.${workflowIndex!}.connections.${workflowConnectionIndex}.operationName`}
                render={({field}) => <input type="hidden" {...field} />}
            />

            {showNewConnectionDialog && (
                <Portal.Root>
                    <ConnectionDialog
                        componentDefinition={componentDefinition}
                        onClose={() => setShowNewConnectionDialog(false)}
                    />
                </Portal.Root>
            )}
        </>
    );
};

export default ProjectInstanceDialogWorkflowsStepItemConnection;
