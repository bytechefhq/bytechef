import {A2aServer} from '@/shared/middleware/graphql';

import A2aServerListItem from './A2aServerListItem';

interface A2aServerListProps {
    a2aServers: A2aServer[];
}

const A2aServerList = ({a2aServers}: A2aServerListProps) => {
    return (
        <div className="mx-auto flex w-full max-w-4xl flex-col gap-3 p-4">
            {a2aServers.map((a2aServer) => (
                <A2aServerListItem a2aServer={a2aServer} key={a2aServer.id} />
            ))}
        </div>
    );
};

export default A2aServerList;
