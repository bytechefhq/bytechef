import Button from '@/components/Button/Button';
import Switch from '@/components/Switch/Switch';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Form} from '@/components/ui/form';
import {Label} from '@/components/ui/label';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import getWorkflowComponentConnections from '@/pages/automation/project-deployments/components/project-deployment-dialog/projectDeploymentDialog-utils';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import ConnectionConfigurationList from '@/shared/components/ConnectionConfigurationList';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {
    ComponentConnection,
    Workflow,
    WorkflowInput,
    WorkflowTestConfiguration,
    WorkflowTestConfigurationConnection,
} from '@/shared/middleware/platform/configuration';
import {useSaveWorkflowTestConfigurationMutation} from '@/shared/mutations/platform/workflowTestConfigurations.mutations';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {PropertyAllType} from '@/shared/types';
import * as Portal from '@radix-ui/react-portal';
import {useQueryClient} from '@tanstack/react-query';
import {FileInputIcon, InfoIcon, Link2Icon} from 'lucide-react';
import {useState} from 'react';
import {Control, FieldValues, useForm} from 'react-hook-form';

interface WorkflowTestConfigurationDialogProps {
    onClose: () => void;
    workflow: Workflow;
    workflowTestConfiguration?: WorkflowTestConfiguration;
}

const WorkflowTestConfigurationDialog = ({
    onClose,
    workflow,
    workflowTestConfiguration,
}: WorkflowTestConfigurationDialogProps) => {
    const [showNewConnectionDialog, setShowNewConnectionDialog] = useState(false);
    const [componentConnection, setComponentConnection] = useState<ComponentConnection | undefined>();
    const [connectionsGrouped, setConnectionsGrouped] = useState(false);

    const connectionDialogAllowed = useWorkflowNodeDetailsPanelStore((state) => state.connectionDialogAllowed);

    const {
        ConnectionKeys,
        useCreateConnectionMutation,
        useGetComponentDefinitionsQuery,
        useGetConnectionTagsQuery,
        useGetConnectionsQuery,
    } = useWorkflowEditor();

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({});

    const componentConnections = getWorkflowComponentConnections(workflow);

    const workflowTestConfigurationConnections = componentConnections.map((componentConnection) => {
        const workflowTestConfigurationConnection = (workflowTestConfiguration?.connections ?? []).find(
            (curWorkflowTestConfigurationConfiguration) =>
                curWorkflowTestConfigurationConfiguration.workflowNodeName === componentConnection.workflowNodeName &&
                curWorkflowTestConfigurationConfiguration.workflowConnectionKey === componentConnection.key
        );

        return (
            workflowTestConfigurationConnection ??
            ({
                workflowConnectionKey: componentConnection.key,
                workflowNodeName: componentConnection.workflowNodeName,
            } as WorkflowTestConfigurationConnection)
        );
    });

    const form = useForm<WorkflowTestConfiguration>({
        defaultValues: {
            ...workflowTestConfiguration,
            connections: workflowTestConfigurationConnections,
        },
    });

    const {control, formState, handleSubmit, setValue} = form;

    const inputs: WorkflowInput[] = workflow.inputs ?? [];

    const {data: connections} = useGetConnectionsQuery!({});

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: componentDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: componentConnection?.componentName!,
            componentVersion: componentConnection?.componentVersion!,
        },
        !!componentConnection
    );

    const queryClient = useQueryClient();

    const saveWorkflowTestConfigurationMutation = useSaveWorkflowTestConfigurationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations});

            onClose();
        },
    });

    function saveWorkflowTestConfiguration(workflowTestConfiguration: WorkflowTestConfiguration) {
        workflowTestConfiguration = {
            ...workflowTestConfiguration,
            connections: workflowTestConfiguration.connections?.filter((connection) => connection.connectionId),
        };

        saveWorkflowTestConfigurationMutation.mutate({
            workflowId: workflow.id!,
            workflowTestConfiguration,
        });
    }

    return (
        <Dialog onOpenChange={onClose} open={true}>
            <DialogContent
                className="max-w-workflow-test-configuration-dialog-width gap-0 p-0"
                onInteractOutside={(event) => event.preventDefault()}
            >
                <DialogHeader className="flex flex-row items-center justify-between space-y-0 p-6">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Workflow Test Configuration</DialogTitle>

                        <DialogDescription>
                            Set workflow input, trigger output values and test connections.
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <Form {...form}>
                    <Tabs
                        className="flex max-h-workflow-test-configuration-dialog-height flex-col gap-3 overflow-y-auto"
                        defaultValue="connections"
                    >
                        <TabsList className="mx-6 flex">
                            <TabsTrigger className="flex w-full gap-2" value="connections">
                                <Link2Icon className="size-4" />
                                Connections
                            </TabsTrigger>

                            <TabsTrigger className="flex w-full gap-2" value="inputs">
                                <FileInputIcon className="size-4" />
                                Inputs
                            </TabsTrigger>
                        </TabsList>

                        <form onSubmit={handleSubmit((values) => saveWorkflowTestConfiguration(values))}>
                            <TabsContent className="px-6" value="connections">
                                {connections && componentConnections && componentConnections.length > 0 && (
                                    <ConnectionConfigurationList
                                        componentConnections={componentConnections}
                                        connectionDialogAllowed={connectionDialogAllowed}
                                        connections={connections}
                                        connectionsGrouped={connectionsGrouped}
                                        control={control as unknown as Control<FieldValues>}
                                        handleConnectionDialogOpen={(componentConnection) => {
                                            setComponentConnection(componentConnection);

                                            setShowNewConnectionDialog(true);
                                        }}
                                        handleConnectionIdChange={(index, connectionId) =>
                                            setValue(`connections.${index}.connectionId`, connectionId)
                                        }
                                        workflow={workflow}
                                    />
                                )}
                            </TabsContent>

                            <TabsContent className="px-6" value="inputs">
                                {inputs && inputs.length > 0 && (
                                    <Properties
                                        control={control}
                                        controlPath="inputs"
                                        formState={formState}
                                        properties={inputs.map((input) => {
                                            if (input.type === 'string') {
                                                return {
                                                    controlType: 'TEXT',
                                                    type: 'STRING',
                                                    ...input,
                                                } as PropertyAllType;
                                            } else if (input.type === 'number') {
                                                return {
                                                    type: 'NUMBER',
                                                    ...input,
                                                } as PropertyAllType;
                                            } else {
                                                return {
                                                    controlType: 'SELECT',
                                                    type: 'BOOLEAN',
                                                    ...input,
                                                } as PropertyAllType;
                                            }
                                        })}
                                    />
                                )}
                            </TabsContent>
                        </form>
                    </Tabs>
                </Form>

                <DialogFooter className="flex items-center p-6">
                    <div className="mr-auto flex items-center gap-2">
                        {componentConnections.length > 1 && (
                            <>
                                <Switch
                                    checked={connectionsGrouped}
                                    label="Group Connections"
                                    onCheckedChange={setConnectionsGrouped}
                                />

                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <InfoIcon className="size-4 cursor-default text-content-onsurface-secondary" />
                                    </TooltipTrigger>

                                    <TooltipContent>Connections grouped by their app.</TooltipContent>
                                </Tooltip>
                            </>
                        )}
                    </div>

                    <DialogClose asChild>
                        <Button label="Cancel" type="button" variant="outline" />
                    </DialogClose>

                    <Button label="Save" type="submit" />
                </DialogFooter>

                {showNewConnectionDialog && componentDefinitions && (
                    <Portal.Root>
                        <ConnectionDialog
                            componentDefinition={componentDefinition}
                            componentDefinitions={componentDefinitions}
                            connectionTagsQueryKey={ConnectionKeys!.connectionTags}
                            connectionsQueryKey={ConnectionKeys!.connections}
                            onClose={() => setShowNewConnectionDialog(false)}
                            useCreateConnectionMutation={useCreateConnectionMutation}
                            useGetConnectionTagsQuery={useGetConnectionTagsQuery!}
                        />
                    </Portal.Root>
                )}
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowTestConfigurationDialog;
