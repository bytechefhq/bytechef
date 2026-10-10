import FilterTitle from '@/shared/components/filters/FilterTitle';
import {Tag} from '@/shared/middleware/graphql';

import {A2aServersFilterType} from '../hooks/useA2aServers';

interface A2aServersFilterTitleProps {
    filterData: {id?: string; type: A2aServersFilterType};
    tags?: Tag[];
    uniqueProjects: {id: string; name: string}[];
}

const A2aServersFilterTitle = ({filterData, tags, uniqueProjects}: A2aServersFilterTitleProps) => {
    if (!filterData.id) {
        return <FilterTitle filters={[{label: 'Projects', value: 'All Projects'}]} />;
    }

    if (filterData.type === A2aServersFilterType.Tag) {
        const matchedTag = tags?.find((tag) => tag.id === filterData.id);

        return <FilterTitle filters={[{label: 'Tags', value: matchedTag?.name || 'Unknown'}]} />;
    }

    const matchedProject = uniqueProjects.find((project) => project.id === filterData.id);

    return <FilterTitle filters={[{label: 'Projects', value: matchedProject?.name || 'Unknown'}]} />;
};

export default A2aServersFilterTitle;
