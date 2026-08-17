import {AiAgent} from '@/shared/middleware/graphql';

import AgentListItem from './AgentListItem';

interface AgentListProps {
    agents: AiAgent[];
}

const AgentList = ({agents}: AgentListProps) => {
    return (
        <div className="flex w-full flex-col gap-2 px-4 3xl:mx-auto 3xl:w-4/5">
            {agents.map((agent) => (
                <AgentListItem agent={agent} key={agent.id} />
            ))}
        </div>
    );
};

export default AgentList;
