import {A2aServer, Tag} from '@/shared/middleware/graphql';

import A2aServerListItem from './A2aServerListItem';

interface A2aServerListProps {
    a2aServers: A2aServer[];
    tags?: Tag[];
}

const A2aServerList = ({a2aServers, tags}: A2aServerListProps) => {
    return (
        <div className="w-full self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
            {a2aServers.map((a2aServer) => (
                <A2aServerListItem a2aServer={a2aServer} key={a2aServer.id} tags={tags} />
            ))}
        </div>
    );
};

export default A2aServerList;
