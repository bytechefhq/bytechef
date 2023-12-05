import {Button} from '@/components/ui/button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Cross1Icon} from '@radix-ui/react-icons';
import EmptyList from 'components/EmptyList';
import {LinkIcon, PlusIcon} from 'lucide-react';
import {ComponentDefinitionModel} from 'middleware/hermes/configuration';
import ConnectionDialog from 'pages/automation/connections/components/ConnectionDialog';
import {useGetConnectionsQuery} from 'queries/connections.queries';
import {useState} from 'react';

import {useConnectionNoteStore} from '../../stores/useConnectionNoteStore';

const ConnectionTab = ({componentDefinition}: {componentDefinition: ComponentDefinitionModel}) => {
    const [showEditConnectionDialog, setShowEditConnectionDialog] = useState(false);

    const {data: connections} = useGetConnectionsQuery(
        {
            componentName: componentDefinition.name,
            connectionVersion: componentDefinition.connection?.version,
        },
        !!componentDefinition.connection?.componentName
    );

    const {setShowConnectionNote, showConnectionNote} = useConnectionNoteStore();

    return (
        <div className="h-full flex-[1_1_1px] overflow-auto p-4">
            {connections?.length ? (
                <Select>
                    <div className="flex space-x-2">
                        <SelectTrigger>
                            <SelectValue placeholder="Choose Connection..." />
                        </SelectTrigger>

                        <Button
                            className="mt-auto p-2"
                            onClick={() => setShowEditConnectionDialog(true)}
                            title="Create a new connection"
                            variant="outline"
                        >
                            <PlusIcon className="h-5 w-5" />
                        </Button>
                    </div>

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
            ) : (
                <EmptyList
                    button={
                        <Button onClick={() => setShowEditConnectionDialog(true)} title="Create a new connection">
                            Create a connection
                        </Button>
                    }
                    icon={<LinkIcon className="h-6 w-6 text-gray-400" />}
                    message="You have not created any connections for this component yet."
                    title="No Connections"
                />
            )}

            {showConnectionNote && (
                <div className="mt-4 flex flex-col rounded-md bg-amber-100 p-4 text-gray-800">
                    <div className="flex items-center pb-2">
                        <span className="font-medium">Note</span>

                        <Button
                            className="ml-auto p-0"
                            onClick={() => setShowConnectionNote(false)}
                            size="icon"
                            title="Close the note"
                            variant="ghost"
                        >
                            <Cross1Icon className="h-3 w-3" />
                        </Button>
                    </div>

                    <p className="text-sm text-gray-800">The selected connection is used for testing purposes only.</p>
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
