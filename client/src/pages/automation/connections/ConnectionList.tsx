import {LinkIcon} from '@heroicons/react/24/outline';
import {useSearchParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

import EmptyList from '../../../components/EmptyList/EmptyList';
import {
    useGetConnectionTagsQuery,
    useGetConnectionsQuery,
} from '../../../queries/connections.queries';
import ConnectionListItem from './ConnectionListItem';
import ConnectionDialog from './components/ConnectionDialog';

const ConnectionList = () => {
    const [searchParams] = useSearchParams();

    const {
        data: connections,
        error,
        isLoading,
    } = useGetConnectionsQuery({
        componentNames: searchParams.get('componentName')
            ? [searchParams.get('componentName')!]
            : undefined,
        tagIds: searchParams.get('tagId')
            ? [parseInt(searchParams.get('tagId')!)]
            : undefined,
    });

    const {data: tags} = useGetConnectionTagsQuery();

    return (
        <div
            className={twMerge(
                'w-full px-2 2xl:mx-auto 2xl:w-4/5',
                connections?.length === 0 ? 'place-self-center' : ''
            )}
        >
            <ul role="list">
                {isLoading && <span className="px-2">Loading...</span>}

                {error &&
                    !isLoading &&
                    `An error has occurred: ${error.message}`}

                {!isLoading &&
                    !error &&
                    (connections?.length === 0 ? (
                        <EmptyList
                            button={<ConnectionDialog />}
                            icon={
                                <LinkIcon className="h-12 w-12 text-gray-400" />
                            }
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
                                            key={connection.id}
                                            connection={connection}
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
                    ))}
            </ul>
        </div>
    );
};
export default ConnectionList;
