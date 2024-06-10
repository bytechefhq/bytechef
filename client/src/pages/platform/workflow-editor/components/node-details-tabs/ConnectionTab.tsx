import {Button} from '@/components/ui/button';
import {useConnectionQuery} from '@/pages/platform/connection/providers/connectionReactQueryProvider';
import ConnectionTabConnectionLabel from '@/pages/platform/workflow-editor/components/node-details-tabs/ConnectionTabConnectionLabel';
import ConnectionTabConnectionSelect from '@/pages/platform/workflow-editor/components/node-details-tabs/ConnectionTabConnectionSelect';
import {
    ComponentDefinitionModel,
    WorkflowConnectionModel,
    WorkflowTestConfigurationConnectionModel,
} from '@/shared/middleware/platform/configuration';
import {Cross2Icon} from '@radix-ui/react-icons';
import EmptyList from 'components/EmptyList';
import {LinkIcon} from 'lucide-react';
import ConnectionDialog from 'pages/platform/connection/components/ConnectionDialog';

import {useConnectionNoteStore} from '../../stores/useConnectionNoteStore';

const ConnectionTab = ({
    componentDefinition,
    workflowConnections,
    workflowId,
    workflowNodeName,
    workflowTestConfigurationConnections,
}: {
    componentDefinition: ComponentDefinitionModel;
    workflowConnections: WorkflowConnectionModel[];
    workflowNodeName: string;
    workflowId: string;
    workflowTestConfigurationConnections?: Array<WorkflowTestConfigurationConnectionModel>;
}) => {
    const {setShowConnectionNote, showConnectionNote} = useConnectionNoteStore();

    const {ConnectionKeys, useCreateConnectionMutation, useGetConnectionTagsQuery} = useConnectionQuery();

    return (
        <div className="flex h-full flex-col gap-4 overflow-auto p-4">
            {workflowConnections?.length ? (
                workflowConnections.map((workflowConnection) => {
                    const workflowTestConfigurationConnection =
                        workflowTestConfigurationConnections && workflowTestConfigurationConnections.length > 0
                            ? workflowTestConfigurationConnections.filter(
                                  (workflowTestConfigurationConnection) =>
                                      workflowTestConfigurationConnection.workflowConnectionKey ===
                                      workflowConnection.key
                              )[0]
                            : undefined;

                    return (
                        <fieldset className="space-y-2" key={workflowConnection.key}>
                            <ConnectionTabConnectionLabel
                                workflowConnection={workflowConnection}
                                workflowConnectionsCount={workflowConnections.length}
                            />

                            <ConnectionTabConnectionSelect
                                workflowConnection={workflowConnection}
                                workflowId={workflowId}
                                workflowNodeName={workflowNodeName}
                                workflowTestConfigurationConnection={workflowTestConfigurationConnection}
                            />
                        </fieldset>
                    );
                })
            ) : (
                <EmptyList
                    button={
                        <ConnectionDialog
                            componentDefinition={componentDefinition}
                            connectionTagsQueryKey={ConnectionKeys!.connectionTags}
                            connectionsQueryKey={ConnectionKeys!.connections}
                            triggerNode={<Button title="Create a new connection">Create a connection</Button>}
                            useCreateConnectionMutation={useCreateConnectionMutation}
                            useGetConnectionTagsQuery={useGetConnectionTagsQuery!}
                        />
                    }
                    icon={<LinkIcon className="size-6 text-gray-400" />}
                    message="You have not created any connections for this component yet."
                    title="No Connections"
                />
            )}

            {showConnectionNote && (
                <div className="mt-4 flex flex-col rounded-md bg-amber-100 p-4 text-gray-800">
                    <div className="flex items-center pb-2">
                        <span className="font-medium">Note</span>

                        <button
                            className="ml-auto p-0"
                            onClick={() => setShowConnectionNote(false)}
                            title="Close the note"
                        >
                            <Cross2Icon aria-hidden="true" className="size-4 cursor-pointer" />
                        </button>
                    </div>

                    <p className="text-sm text-gray-800">
                        The selected connections are used for testing purposes only.
                    </p>
                </div>
            )}
        </div>
    );
};

export default ConnectionTab;
