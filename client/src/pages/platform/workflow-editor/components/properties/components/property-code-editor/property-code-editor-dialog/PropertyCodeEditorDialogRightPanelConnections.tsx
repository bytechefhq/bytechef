import Button from '@/components/Button/Button';
import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';
import PropertyCodeEditorDialogRightPanelConnectionsLabel from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialogRightPanelConnectionsLabel';
import PropertyCodeEditorDialogRightPanelConnectionsPopover from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialogRightPanelConnectionsPopover';
import PropertyCodeEditorDialogRightPanelConnectionsSelect from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialogRightPanelConnectionsSelect';
import {usePropertyCodeEditorDialogRightPanelConnections} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/hooks/usePropertyCodeEditorDialogRightPanelConnections';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {ComponentConnection, Workflow} from '@/shared/middleware/platform/configuration';
import {LinkIcon, XIcon} from 'lucide-react';

const PropertyCodeEditorDialogRightPanelConnections = ({
    componentConnections,
    workflow,
    workflowNodeName,
}: {
    componentConnections: ComponentConnection[];
    workflow: Workflow;
    workflowNodeName: string;
}) => {
    const {
        ConnectionKeys,
        componentDefinitions,
        handleCloseConnectionNote,
        handleOnRemoveClick,
        handleOnSubmit,
        setShowNewConnectionDialog,
        showConnectionNote,
        showNewConnectionDialog,
        useCreateConnectionMutation,
        useGetConnectionTagsQuery,
        workflowTestConfigurationConnections,
    } = usePropertyCodeEditorDialogRightPanelConnections({
        componentConnections,
        workflow,
        workflowNodeName,
    });

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
                                    <PropertyCodeEditorDialogRightPanelConnectionsLabel
                                        componentConnection={workflowConnection}
                                        onRemoveClick={() => handleOnRemoveClick(workflowConnection.key)}
                                    />

                                    <PropertyCodeEditorDialogRightPanelConnectionsSelect
                                        componentConnection={workflowConnection}
                                        workflowId={workflow.id!}
                                        workflowNodeName={workflowNodeName}
                                        workflowTestConfigurationConnection={workflowTestConfigurationConnection}
                                    />
                                </fieldset>
                            );
                        })}

                        <div className="mt-3 flex justify-end">
                            <PropertyCodeEditorDialogRightPanelConnectionsPopover onSubmit={handleOnSubmit} />
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
                                    <PropertyCodeEditorDialogRightPanelConnectionsPopover
                                        onSubmit={handleOnSubmit}
                                        triggerNode={<Button label="Add Component" />}
                                    />
                                </div>
                            </div>
                        </div>

                        {showConnectionNote && (
                            <div className="mt-4 flex flex-col rounded-md bg-amber-100 p-4 text-gray-800">
                                <div className="flex items-center pb-2">
                                    <span className="font-medium">Note</span>

                                    <Button
                                        className="ml-auto hover:bg-transparent active:bg-transparent active:text-content-neutral-primary"
                                        icon={<XIcon aria-hidden="true" />}
                                        onClick={handleCloseConnectionNote}
                                        size="iconXs"
                                        title="Close the note"
                                        variant="ghost"
                                    />
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

export default PropertyCodeEditorDialogRightPanelConnections;
