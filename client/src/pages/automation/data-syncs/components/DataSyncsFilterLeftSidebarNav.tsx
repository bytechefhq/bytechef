import LeftSidebarFilterNav from '@/shared/layout/LeftSidebarFilterNav';
import {useSearchParams} from 'react-router-dom';

export type DataSyncsFilterType = 'all' | 'scheduled';

const DATA_SYNCS_FILTER_ITEMS: {id: DataSyncsFilterType; name: string}[] = [
    {id: 'all', name: 'All Data Syncs'},
    {id: 'scheduled', name: 'Scheduled'},
];

export const getDataSyncsFilter = (searchParams: URLSearchParams): DataSyncsFilterType | undefined => {
    const dataSyncsSearchParam = searchParams.get('dataSyncs');

    return dataSyncsSearchParam === 'all' || dataSyncsSearchParam === 'scheduled' ? dataSyncsSearchParam : undefined;
};

interface DataSyncsFilterLeftSidebarNavProps {
    currentDataSyncsFilter?: DataSyncsFilterType;
}

const DataSyncsFilterLeftSidebarNav = ({currentDataSyncsFilter}: DataSyncsFilterLeftSidebarNavProps) => {
    const [searchParams] = useSearchParams();

    const getToLink = (dataSyncsFilter: DataSyncsFilterType) => {
        const nextSearchParams = new URLSearchParams(searchParams);

        nextSearchParams.delete('agents');

        if (dataSyncsFilter === currentDataSyncsFilter) {
            nextSearchParams.delete('dataSyncs');
        } else {
            nextSearchParams.set('dataSyncs', dataSyncsFilter);
        }

        const search = nextSearchParams.toString();

        return search ? `?${search}` : '';
    };

    return (
        <LeftSidebarFilterNav
            items={DATA_SYNCS_FILTER_ITEMS.map((item) => ({
                current: item.id === currentDataSyncsFilter,
                id: item.id,
                name: item.name,
                toLink: getToLink(item.id),
            }))}
            title="Data Syncs"
        />
    );
};

export default DataSyncsFilterLeftSidebarNav;
