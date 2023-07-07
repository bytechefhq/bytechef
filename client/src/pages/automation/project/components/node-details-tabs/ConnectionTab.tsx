import {LinkIcon, PlusIcon, XMarkIcon} from '@heroicons/react/24/outline';
import Button from 'components/Button/Button';
import EmptyList from 'components/EmptyList/EmptyList';
import Select from 'components/Select/Select';
import {ComponentDefinitionModel} from 'middleware/core/workflow/configuration';
import ConnectionDialog from 'pages/automation/connections/components/ConnectionDialog';
import {useGetConnectionsQuery} from 'queries/connections.queries';
import {useState} from 'react';

import {useConnectionNoteStore} from '../../stores/useNodeDetailsDialogStore';

const ConnectionTab = ({component}: {component: ComponentDefinitionModel}) => {
    const [showEditConnectionDialog, setShowEditConnectionDialog] =
        useState(false);

    const {data: connections} = useGetConnectionsQuery(
        {
            componentName: component.name,
            connectionVersion: component.connection?.version,
        },
        !!component.connection?.componentName
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
                        displayType="secondary"
                        className="mt-auto p-2"
                        icon={<PlusIcon className="h-5 w-5" />}
                        onClick={() => setShowEditConnectionDialog(true)}
                        title="Create a new connection"
                    />
                </div>
            ) : (
                <div className="p-4">
                    <EmptyList
                        button={
                            <Button
                                label="Create a connection"
                                onClick={() =>
                                    setShowEditConnectionDialog(true)
                                }
                                title="Create a new connection"
                            />
                        }
                        icon={<LinkIcon className="h-6 w-6 text-gray-400" />}
                        title="No Connections"
                        message="You have not created any connections for this component yet."
                    />
                </div>
            )}

            {showConnectionNote && (
                <div className="mt-4 flex flex-col rounded-md bg-amber-100 p-4 text-gray-800">
                    <div className="flex pb-2">
                        <span className="font-medium">Note</span>

                        <Button
                            className="ml-auto p-0"
                            displayType="icon"
                            icon={<XMarkIcon className="ml-auto h-5 w-5" />}
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
                    component={component}
                    showTrigger={false}
                    visible
                    onClose={() => setShowEditConnectionDialog(false)}
                />
            )}
        </div>
    );
};

export default ConnectionTab;
