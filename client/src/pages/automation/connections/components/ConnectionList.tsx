import {ConnectionModel, TagModel} from '@/middleware/helios/connection';

import ConnectionListItem from './ConnectionListItem';

const ConnectionList = ({
    connections,
    tags,
}: {
    connections: ConnectionModel[];
    tags: TagModel[];
}) => {
    return (
        <ul
            className="w-full divide-y divide-gray-100 px-2 2xl:mx-auto 2xl:w-4/5"
            role="list"
        >
            {connections.map((connection) => {
                const connectionTagIds = connection.tags?.map((tag) => tag.id);

                return (
                    <ConnectionListItem
                        connection={connection}
                        key={connection.id}
                        remainingTags={tags?.filter(
                            (tag) => !connectionTagIds?.includes(tag.id)
                        )}
                    />
                );
            })}
        </ul>
    );
};
export default ConnectionList;
