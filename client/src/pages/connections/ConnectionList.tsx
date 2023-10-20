import {Link, useSearchParams} from 'react-router-dom';
import ConnectionItem from './ConnectionItem';
import {
    useGetConnectionsQuery,
    useGetConnectionTagsQuery,
} from '../../queries/connections';
import EmptyList from '../../components/EmptyList/EmptyList';
import {LinkIcon} from '@heroicons/react/24/outline';
import ConnectionModal from './ConnectionModal';
import {twMerge} from 'tailwind-merge';

const ConnectionList = () => {
    const [searchParams] = useSearchParams();

    const {
        isLoading,
        error,
        data: connections,
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
                'flex place-self-center px-2 sm:w-full 2xl:w-4/5',
                connections?.length === 0 ? 'h-full items-center' : ''
            )}
        >
            <ul role="list" className="w-full divide-y divide-gray-100">
                {isLoading && 'Loading...'}

                {error &&
                    !isLoading &&
                    `An error has occurred: ${error.message}`}

                {!isLoading &&
                    !error &&
                    (connections?.length === 0 ? (
                        <EmptyList
                            button={<ConnectionModal />}
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
                                <div key={connection.id}>
                                    <Link to={``}>
                                        <li className="group my-3 rounded-md bg-white p-2 hover:bg-gray-50">
                                            <ConnectionItem
                                                key={connection.id}
                                                connection={connection}
                                                remainingTags={tags?.filter(
                                                    (tag) =>
                                                        !connectionTagIds?.includes(
                                                            tag.id
                                                        )
                                                )}
                                            />
                                        </li>
                                    </Link>
                                </div>
                            );
                        })
                    ))}
            </ul>
        </div>
    );
};
export default ConnectionList;
