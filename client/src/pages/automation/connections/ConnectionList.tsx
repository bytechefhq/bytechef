import {Button} from '@/components/ui/button';
import {ConnectionModel, TagModel} from '@/middleware/helios/connection';
import {Link2Icon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

import EmptyList from '../../../components/EmptyList/EmptyList';
import ConnectionListItem from './ConnectionListItem';
import ConnectionDialog from './components/ConnectionDialog';

const ConnectionList = ({
    connections,
    tags,
}: {
    connections: ConnectionModel[];
    tags: TagModel[];
}) => {
    return (
        <div
            className={twMerge(
                'w-full px-2 2xl:mx-auto 2xl:w-4/5',
                connections?.length === 0 ? 'place-self-center' : ''
            )}
        >
            <ul role="list">
                {connections?.length === 0 ? (
                    <EmptyList
                        button={
                            <ConnectionDialog
                                triggerNode={<Button>Create Connection</Button>}
                            />
                        }
                        icon={<Link2Icon className="h-12 w-12 text-gray-400" />}
                        message="You do not have any Connections created yet."
                        title="No Connections"
                    />
                ) : (
                    connections.map((connection) => {
                        const connectionTagIds = connection.tags?.map(
                            (tag) => tag.id
                        );

                        return (
                            <li key={connection.id}>
                                <div className="group rounded-md border-b border-b-gray-100 bg-white p-2 py-3 hover:bg-gray-50">
                                    <ConnectionListItem
                                        connection={connection}
                                        key={connection.id}
                                        remainingTags={tags?.filter(
                                            (tag) =>
                                                !connectionTagIds?.includes(
                                                    tag.id
                                                )
                                        )}
                                    />
                                </div>
                            </li>
                        );
                    })
                )}
            </ul>
        </div>
    );
};
export default ConnectionList;
