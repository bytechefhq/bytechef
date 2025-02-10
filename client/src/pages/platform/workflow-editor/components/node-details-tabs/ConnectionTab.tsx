import {Button} from '@/components/ui/button';
import ConnectionTabConnectionSelect from '@/pages/platform/workflow-editor/components/node-details-tabs/ConnectionTabConnectionSelect';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {useConnectionQuery} from '@/shared/components/connection/providers/connectionReactQueryProvider';
import {
    ComponentConnection,
    ComponentDefinition,
    WorkflowTestConfigurationConnection,
} from '@/shared/middleware/platform/configuration';
import {Cross2Icon} from '@radix-ui/react-icons';
import EmptyList from 'components/EmptyList';
import {LinkIcon} from 'lucide-react';

import {useConnectionNoteStore} from '../../stores/useConnectionNoteStore';

type ConnectionTabPropsType = {
    componentConnections: Array<ComponentConnection>;
    componentDefinition: ComponentDefinition;
    workflowNodeName: string;
    workflowId: string;
    workflowTestConfigurationConnections?: Array<WorkflowTestConfigurationConnection>;
};

const ConnectionTab = ({
    componentConnections,
    componentDefinition,
    workflowId,
    workflowNodeName,
    workflowTestConfigurationConnections,
}: ConnectionTabPropsType) => {
    const {setShowConnectionNote, showConnectionNote} = useConnectionNoteStore();

    const {ConnectionKeys, useCreateConnectionMutation, useGetConnectionTagsQuery} = useConnectionQuery();

    return (
        <div className="flex h-full flex-col gap-6 overflow-auto p-4">
            {componentConnections?.length ? (
                componentConnections.map((componentConnection) => {
                    const workflowTestConfigurationConnection = workflowTestConfigurationConnections?.find(
                        (testConfigConnection) => testConfigConnection.workflowConnectionKey === componentConnection.key
                    );

                    return (
                        <fieldset className="space-y-2" key={componentConnection.key}>
                            <ConnectionTabConnectionSelect
                                componentConnection={componentConnection}
                                componentConnectionsCount={componentConnections.length}
                                componentDefinition={componentDefinition}
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
                <div className="flex flex-col rounded-md bg-amber-100 p-4 text-gray-800">
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
