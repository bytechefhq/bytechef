import {ConnectionModel, TagModel} from '@/middleware/helios/connection';
import {twMerge} from 'tailwind-merge';

import ConnectionListItem from './ConnectionListItem';

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
                {connections.map((connection) => {
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
                                            !connectionTagIds?.includes(tag.id)
                                    )}
                                />
                            </div>
                        </li>
                    );
                })}
            </ul>
        </div>
    );
};
export default ConnectionList;
