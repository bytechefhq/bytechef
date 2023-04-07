import {PlusIcon, XMarkIcon} from '@heroicons/react/24/outline';
import Button from 'components/Button/Button';
import Select from 'components/Select/Select';
import {ComponentDefinitionModel} from 'middleware/definition-registry';
import ConnectionDialog from 'pages/automation/connections/components/ConnectionDialog';
import {useGetConnectionsQuery} from 'queries/connections.queries';
import {useState} from 'react';

import {useConnectionNoteStore} from '../../stores/useNodeDetailsDialogStore';

const ConnectionTab = ({component}: {component: ComponentDefinitionModel}) => {
    const [showEditConnectionDialog, setShowEditConnectionDialog] =
        useState(false);

    const {data: connections} = useGetConnectionsQuery({
        componentNames: [component.connection!.name!],
    });

    const {showConnectionNote, setShowConnectionNote} =
        useConnectionNoteStore();

    return (
        <>
            {!!connections?.length && (
                <div className="flex space-x-2">
                    <Select
                        contentClassName="max-w-select-trigger-width max-h-select-content-available-height-1/2"
                        label="Connections"
                        options={connections.map((connection) => ({
                            label: connection.name,
                            value: connection.id!.toString(),
                        }))}
                        triggerClassName="w-full bg-gray-100"
                    />

                    <Button
                        className="mt-auto bg-blue-500 p-2"
                        icon={<PlusIcon className="h-5 w-5" />}
                        onClick={() => setShowEditConnectionDialog(true)}
                        title="Create a new connection"
                    />
                </div>
            )}

            {showConnectionNote && (
                <div className="mt-4 flex flex-col rounded-md border-2 border-amber-200 bg-amber-100 p-4 text-gray-800">
                    <div className="flex pb-2">
                        <span className="font-medium ">Note</span>

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
        </>
    );
};

export default ConnectionTab;
