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
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import getWorkflowComponentConnections from '@/pages/automation/project-deployments/components/project-deployment-dialog/projectDeploymentDialog-utils';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import ConnectionConfigurationList from '@/shared/components/ConnectionConfigurationList';
import InputConfigurationList from '@/shared/components/InputConfigurationList';
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
import {synchronizeGroupedConnections} from '@/shared/util/synchronizeGroupedConnections';
import * as Portal from '@radix-ui/react-portal';
import {useQueryClient} from '@tanstack/react-query';
import {FileInputIcon, InfoIcon, Link2Icon} from 'lucide-react';
import {useState} from 'react';
import {Control, FieldValues, useForm, useWatch} from 'react-hook-form';
import {useShallow} from 'zustand/react/shallow';

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

    const {setShowWorkflowCodeEditorSheet, setShowWorkflowInputsSheet} = useWorkflowEditorStore(
        useShallow((state) => ({
            setShowWorkflowCodeEditorSheet: state.setShowWorkflowCodeEditorSheet,
            setShowWorkflowInputsSheet: state.setShowWorkflowInputsSheet,
        }))
    );

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

    const watchedConnections = useWatch({control, name: 'connections'});

    const handleConnectionsGroupedChange = (grouped: boolean) => {
        setConnectionsGrouped(grouped);

        if (!grouped) {
            return;
        }

        synchronizeGroupedConnections({
            componentConnections,
            getConnectionId: (index) => watchedConnections?.[index]?.connectionId,
            setConnectionId: (index, connectionId) =>
                setValue(`connections.${index}.connectionId`, connectionId as number, {shouldDirty: true}),
        });
    };

    const handleOpenInputs = () => {
        onClose();

        setShowWorkflowCodeEditorSheet(false);

        setShowWorkflowInputsSheet(true);
    };

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
                        className="max-h-workflow-test-configuration-dialog-height max-w-workflow-test-configuration-dialog-width gap-2.5"
                        defaultValue="connections"
                    >
                        <TabsList className="mx-6 flex">
                            <TabsTrigger className="flex w-full data-[state=active]:shadow-none" value="connections">
                                <Link2Icon className="mr-2 size-4" />

                                <span>Connections</span>

                                <span className="ml-1">({componentConnections.length})</span>
                            </TabsTrigger>

                            <TabsTrigger className="flex w-full data-[state=active]:shadow-none" value="inputs">
                                <FileInputIcon className="mr-2 size-4" />

                                <span>Inputs</span>

                                <span className="ml-1">({workflow.inputs?.length})</span>
                            </TabsTrigger>
                        </TabsList>

                        <form
                            id="workflow-test-configuration-form"
                            onSubmit={handleSubmit((values) => saveWorkflowTestConfiguration(values))}
                        >
                            <TabsContent className="mt-2 px-6 py-2.5" value="connections">
                                <ConnectionConfigurationList
                                    componentConnections={componentConnections}
                                    connectionDialogAllowed={connectionDialogAllowed}
                                    connections={connections}
                                    connectionsGrouped={connectionsGrouped}
                                    control={control as unknown as Control<FieldValues>}
                                    getCurrentConnectionId={(index) => watchedConnections?.[index]?.connectionId}
                                    handleConnectionDialogOpen={(componentConnection) => {
                                        setComponentConnection(componentConnection);

                                        setShowNewConnectionDialog(true);
                                    }}
                                    handleConnectionIdChange={(index, connectionId) =>
                                        setValue(`connections.${index}.connectionId`, connectionId, {
                                            shouldDirty: true,
                                        })
                                    }
                                    workflow={workflow}
                                />
                            </TabsContent>

                            <TabsContent className="mt-2 px-6 py-2.5" value="inputs">
                                <InputConfigurationList
                                    control={control as unknown as Control<FieldValues>}
                                    controlPath="inputs"
                                    formState={formState}
                                    inputs={inputs}
                                    onOpenInputs={handleOpenInputs}
                                />
                            </TabsContent>
                        </form>
                    </Tabs>
                </Form>

                <DialogFooter className="flex items-center p-6">
                    {componentConnections.length > 1 && (
                        <div className="mr-auto flex items-center gap-2">
                            <Switch
                                checked={connectionsGrouped}
                                label="Group Connections"
                                onCheckedChange={handleConnectionsGroupedChange}
                            />

                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <InfoIcon className="size-4 cursor-default text-content-onsurface-secondary" />
                                </TooltipTrigger>

                                <TooltipContent>Connections grouped by their component.</TooltipContent>
                            </Tooltip>
                        </div>
                    )}

                    <DialogClose asChild>
                        <Button label="Cancel" type="button" variant="outline" />
                    </DialogClose>

                    <Tooltip>
                        <TooltipTrigger asChild>
                            <div>
                                <Button
                                    disabled={!formState.isDirty || saveWorkflowTestConfigurationMutation.isPending}
                                    form="workflow-test-configuration-form"
                                    label="Save"
                                    type="submit"
                                />
                            </div>
                        </TooltipTrigger>

                        <TooltipContent>
                            {!formState.isDirty ? 'Nothing to save.' : 'Save workflow test configuration.'}
                        </TooltipContent>
                    </Tooltip>
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
