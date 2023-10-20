import {Link, useSearchParams} from 'react-router-dom';
import ConnectionItem from './ConnectionItem';
import {
    useGetConnectionsQuery,
    useGetConnectionTagsQuery,
} from '../../queries/connections';

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
        <div className="flex place-self-center px-2 sm:w-full 2xl:w-4/5">
            <ul role="list" className="w-full divide-y divide-gray-100">
                {isLoading && 'Loading...'}

                {error &&
                    !isLoading &&
                    `An error has occurred: ${error.message}`}

                {!isLoading &&
                    !error &&
                    (connections?.length === 0 ? (
                        <p>You do not have any Connection created yet.</p>
                    ) : (
                        connections.map((connection) => {
                            const integrationTagIds = connection.tags?.map(
                                (tag) => tag.id
                            );

                            return (
                                <div key={connection.id}>
                                    <Link to={``}>
                                        <li className="group my-3 rounded-md bg-white p-2 hover:bg-gray-50">
                                            <ConnectionItem
                                                key={connection.id}
                                                id={connection.id}
                                                lastModifiedDate={
                                                    connection.lastModifiedDate
                                                }
                                                name={connection.name}
                                                remainingTags={tags?.filter(
                                                    (tag) =>
                                                        !integrationTagIds?.includes(
                                                            tag.id
                                                        )
                                                )}
                                                tags={connection.tags}
                                                version={
                                                    connection.connectionVersion
                                                }
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
