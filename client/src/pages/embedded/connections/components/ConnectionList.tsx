import {ConnectionModel, TagModel} from '@/middleware/automation/connection';

import ConnectionListItem from './ConnectionListItem';

const ConnectionList = ({connections, tags}: {connections: ConnectionModel[]; tags: TagModel[]}) => {
    return (
        <ul className="w-full px-2 3xl:mx-auto 3xl:w-4/5" role="list">
            {connections.map((connection) => {
                const connectionTagIds = connection.tags?.map((tag) => tag.id);

                return (
                    <ConnectionListItem
                        connection={connection}
                        key={connection.id}
                        remainingTags={tags?.filter((tag) => !connectionTagIds?.includes(tag.id))}
                    />
                );
            })}
        </ul>
    );
};

export default ConnectionList;
