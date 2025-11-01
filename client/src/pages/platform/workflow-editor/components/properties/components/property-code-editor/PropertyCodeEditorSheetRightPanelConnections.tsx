import {Button} from '@/components/ui/button';
import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';
import PropertyCodeEditorSheetRightPanelConnectionsLabel from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/PropertyCodeEditorSheetRightPanelConnectionsLabel';
import PropertyCodeEditorSheetRightPanelConnectionsPopover, {
    connectionFormSchema,
} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/PropertyCodeEditorSheetRightPanelConnectionsPopover';
import PropertyCodeEditorSheetRightPanelConnectionsSelect from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/PropertyCodeEditorSheetRightPanelConnectionsSelect';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useConnectionNoteStore} from '@/pages/platform/workflow-editor/stores/useConnectionNoteStore';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {ComponentConnection, Workflow} from '@/shared/middleware/platform/configuration';
import {useGetWorkflowTestConfigurationConnectionsQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {WorkflowDefinitionType, WorkflowTaskType} from '@/shared/types';
import {LinkIcon, XIcon} from 'lucide-react';
import {useState} from 'react';
import {z} from 'zod';
import {useShallow} from 'zustand/react/shallow';

const SPACE = 4;

const PropertyCodeEditorSheetRightPanelConnections = ({
    componentConnections,
    workflow,
    workflowNodeName,
}: {
    componentConnections: ComponentConnection[];
    workflow: Workflow;
    workflowNodeName: string;
}) => {
    const [showNewConnectionDialog, setShowNewConnectionDialog] = useState(false);

    const {setShowConnectionNote, showConnectionNote} = useConnectionNoteStore(
        useShallow((state) => ({
            setShowConnectionNote: state.setShowConnectionNote,
            showConnectionNote: state.showConnectionNote,
        }))
    );
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {
        ConnectionKeys,
        updateWorkflowMutation,
        useCreateConnectionMutation,
        useGetComponentDefinitionsQuery,
        useGetConnectionTagsQuery,
    } = useWorkflowEditor();

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({});

    const {data: workflowTestConfigurationConnections} = useGetWorkflowTestConfigurationConnectionsQuery({
        environmentId: currentEnvironmentId,
        workflowId: workflow.id!,
        workflowNodeName,
    });

    const handleOnSubmit = (values: z.infer<typeof connectionFormSchema>) => {
        if (!workflow?.definition) {
            return;
        }

        let workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow?.definition);

        const scriptWorkflowTask = workflowDefinition.tasks?.filter((task) => task.name === workflowNodeName)[0];

        if (!scriptWorkflowTask) {
            return;
        }

        workflowDefinition = {
            ...workflowDefinition,
            tasks: [
                ...workflowDefinition.tasks!.map((task) => {
                    if (task.name === workflowNodeName) {
                        return {
                            ...scriptWorkflowTask,
                            connections: {
                                ...(scriptWorkflowTask.connections ?? {}),
                                [values.name]: {
                                    componentName: values.componentName,
                                    componentVersion: values.componentVersion,
                                },
                            },
                        } as WorkflowTaskType;
                    } else {
                        return task;
                    }
                }),
            ],
        };

        updateWorkflowMutation!.mutate({
            id: workflow.id!,
            workflow: {
                definition: JSON.stringify(workflowDefinition, null, SPACE),
                version: workflow.version,
            },
        });
    };

    const handleOnRemoveClick = (workflowConnectionKey: string) => {
        if (!workflow?.definition) {
            return;
        }

        let workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow?.definition);

        const scriptWorkflowTask = workflowDefinition.tasks?.filter((task) => task.name === workflowNodeName)[0];

        if (!scriptWorkflowTask) {
            return;
        }

        delete scriptWorkflowTask.connections[workflowConnectionKey];

        workflowDefinition = {
            ...workflowDefinition,
            tasks: [
                ...workflowDefinition.tasks!.map((task) => {
                    if (task.name === workflowNodeName) {
                        return {
                            ...scriptWorkflowTask,
                            connections: {
                                ...(scriptWorkflowTask.connections ?? {}),
                            },
                        };
                    } else {
                        return task;
                    }
                }),
            ],
        };

        updateWorkflowMutation!.mutate({
            id: workflow.id!,
            workflow: {
                definition: JSON.stringify(workflowDefinition, null, SPACE),
                version: workflow.version,
            },
        });
    };

    return (
        <Card className="border-none shadow-none">
            <CardContent className="px-4">
                <CardHeader className="px-0 py-4">
                    <CardTitle>Connections</CardTitle>
                </CardHeader>

                {componentConnections?.length ? (
                    <>
                        {componentConnections.map((workflowConnection) => {
                            const workflowTestConfigurationConnection =
                                workflowTestConfigurationConnections &&
                                workflowTestConfigurationConnections.length > 0 &&
                                workflowTestConfigurationConnections
                                    ? workflowTestConfigurationConnections.filter(
                                          (workflowTestConfigurationConnection) =>
                                              workflowTestConfigurationConnection.workflowConnectionKey ===
                                              workflowConnection.key
                                      )[0]
                                    : undefined;

                            return (
                                <fieldset className="space-y-2" key={workflowConnection.key}>
                                    <PropertyCodeEditorSheetRightPanelConnectionsLabel
                                        componentConnection={workflowConnection}
                                        onRemoveClick={() => handleOnRemoveClick(workflowConnection.key)}
                                    />

                                    <PropertyCodeEditorSheetRightPanelConnectionsSelect
                                        componentConnection={workflowConnection}
                                        workflowId={workflow.id!}
                                        workflowNodeName={workflowNodeName}
                                        workflowTestConfigurationConnection={workflowTestConfigurationConnection}
                                    />
                                </fieldset>
                            );
                        })}

                        <div className="mt-3 flex justify-end">
                            <PropertyCodeEditorSheetRightPanelConnectionsPopover onSubmit={handleOnSubmit} />
                        </div>
                    </>
                ) : (
                    <div className="flex flex-1 flex-col items-center">
                        <div className="mt-16 w-full place-self-center px-2 3xl:mx-auto 3xl:w-4/5">
                            <div className="text-center">
                                <span className="mx-auto inline-block">
                                    <LinkIcon className="size-6 text-gray-400" />
                                </span>

                                <h3 className="mt-2 text-sm font-semibold">No defined components</h3>

                                <p className="mt-1 text-sm text-gray-500">
                                    You have not defined any component and its connection to use inside this script yet.
                                </p>

                                <div className="mt-6">
                                    <PropertyCodeEditorSheetRightPanelConnectionsPopover
                                        onSubmit={handleOnSubmit}
                                        triggerNode={<Button>Add Component</Button>}
                                    />
                                </div>
                            </div>
                        </div>

                        {showConnectionNote && (
                            <div className="mt-4 flex flex-col rounded-md bg-amber-100 p-4 text-gray-800">
                                <div className="flex items-center pb-2">
                                    <span className="font-medium">Note</span>

                                    <button
                                        className="ml-auto p-0"
                                        onClick={() => setShowConnectionNote(false)}
                                        title="Close the note"
                                    >
                                        <XIcon aria-hidden="true" className="size-4 cursor-pointer" />
                                    </button>
                                </div>

                                <p className="text-sm text-gray-800">
                                    The selected connections are used for testing purposes only.
                                </p>
                            </div>
                        )}
                    </div>
                )}

                {showNewConnectionDialog && componentDefinitions && (
                    <ConnectionDialog
                        componentDefinitions={componentDefinitions}
                        connectionTagsQueryKey={ConnectionKeys!.connectionTags}
                        connectionsQueryKey={ConnectionKeys!.connections}
                        onClose={() => setShowNewConnectionDialog(false)}
                        useCreateConnectionMutation={useCreateConnectionMutation}
                        useGetConnectionTagsQuery={useGetConnectionTagsQuery!}
                    />
                )}
            </CardContent>
        </Card>
    );
};

export default PropertyCodeEditorSheetRightPanelConnections;
