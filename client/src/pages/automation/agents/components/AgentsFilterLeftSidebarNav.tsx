import LeftSidebarFilterNav from '@/shared/layout/LeftSidebarFilterNav';
import {useSearchParams} from 'react-router-dom';

export type AgentsFilterType = 'all' | 'scheduled';

const AGENTS_FILTER_ITEMS: {id: AgentsFilterType; name: string}[] = [
    {id: 'all', name: 'All Agents'},
    {id: 'scheduled', name: 'Scheduled'},
];

export const getAgentsFilter = (searchParams: URLSearchParams): AgentsFilterType | undefined => {
    const agentsSearchParam = searchParams.get('agents');

    return agentsSearchParam === 'all' || agentsSearchParam === 'scheduled' ? agentsSearchParam : undefined;
};

interface AgentsFilterLeftSidebarNavProps {
    currentAgentsFilter?: AgentsFilterType;
}

const AgentsFilterLeftSidebarNav = ({currentAgentsFilter}: AgentsFilterLeftSidebarNavProps) => {
    const [searchParams] = useSearchParams();

    const getToLink = (agentsFilter: AgentsFilterType) => {
        const nextSearchParams = new URLSearchParams(searchParams);

        nextSearchParams.delete('dataSyncs');

        if (agentsFilter === currentAgentsFilter) {
            nextSearchParams.delete('agents');
        } else {
            nextSearchParams.set('agents', agentsFilter);
        }

        const search = nextSearchParams.toString();

        return search ? `?${search}` : '';
    };

    return (
        <LeftSidebarFilterNav
            items={AGENTS_FILTER_ITEMS.map((item) => ({
                current: item.id === currentAgentsFilter,
                id: item.id,
                name: item.name,
                toLink: getToLink(item.id),
            }))}
            title="Agents"
        />
    );
};

export default AgentsFilterLeftSidebarNav;
