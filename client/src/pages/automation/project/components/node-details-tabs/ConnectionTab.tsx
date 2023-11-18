import {Cross1Icon} from '@radix-ui/react-icons';
import Button from 'components/Button/Button';
import EmptyList from 'components/EmptyList/EmptyList';
import Select from 'components/Select/Select';
import {LinkIcon, PlusIcon} from 'lucide-react';
import {ComponentDefinitionModel} from 'middleware/hermes/configuration';
import ConnectionDialog from 'pages/automation/connections/components/ConnectionDialog';
import {useGetConnectionsQuery} from 'queries/connections.queries';
import {useState} from 'react';

import {useConnectionNoteStore} from '../../stores/useConnectionNoteStore';

const ConnectionTab = ({
    componentDefinition,
}: {
    componentDefinition: ComponentDefinitionModel;
}) => {
    const [showEditConnectionDialog, setShowEditConnectionDialog] =
        useState(false);

    const {data: connections} = useGetConnectionsQuery(
        {
            componentName: componentDefinition.name,
            connectionVersion: componentDefinition.connection?.version,
        },
        !!componentDefinition.connection?.componentName
    );

    const {setShowConnectionNote, showConnectionNote} =
        useConnectionNoteStore();

    return (
        <div className="h-full flex-[1_1_1px] overflow-auto p-4">
            {connections?.length ? (
                <div className="flex space-x-2">
                    <Select
                        contentClassName="max-w-select-trigger-width max-h-select-content-available-height-1/2"
                        label="Connections"
                        options={connections.map((connection) => ({
                            label: connection.name,
                            value: connection.id!.toString(),
                        }))}
                        placeholder="Choose Connection..."
                        triggerClassName="w-full bg-gray-100"
                    />

                    <Button
                        className="mt-auto p-2"
                        displayType="secondary"
                        icon={<PlusIcon className="h-5 w-5" />}
                        onClick={() => setShowEditConnectionDialog(true)}
                        title="Create a new connection"
                    />
                </div>
            ) : (
                <EmptyList
                    button={
                        <Button
                            label="Create a connection"
                            onClick={() => setShowEditConnectionDialog(true)}
                            title="Create a new connection"
                        />
                    }
                    icon={<LinkIcon className="h-6 w-6 text-gray-400" />}
                    message="You have not created any connections for this component yet."
                    title="No Connections"
                />
            )}

            {showConnectionNote && (
                <div className="mt-4 flex flex-col rounded-md bg-amber-100 p-4 text-gray-800">
                    <div className="flex pb-2">
                        <span className="font-medium">Note</span>

                        <Button
                            className="ml-auto p-0"
                            displayType="icon"
                            icon={<Cross1Icon className="ml-auto h-5 w-5" />}
                            onClick={() => setShowConnectionNote(false)}
                            title="Close the note"
                        />
                    </div>

                    <p className="text-sm text-gray-800">
                        The selected connection is used for testing purposes
                        only.
                    </p>
                </div>
            )}

            {showEditConnectionDialog && (
                <ConnectionDialog
                    componentDefinition={componentDefinition}
                    onClose={() => setShowEditConnectionDialog(false)}
                />
            )}
        </div>
    );
};

export default ConnectionTab;
