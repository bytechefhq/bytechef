import {FilterTitleItemI} from '@/shared/components/filters/FilterTitle';

interface McpServersFilterParamsI {
    groupLabel: string;
    groupSearchParamName: string;
    pageTitle: unknown;
    searchParams: URLSearchParams;
}

export const getMcpServersFilter = ({
    groupLabel,
    groupSearchParamName,
    pageTitle,
    searchParams,
}: McpServersFilterParamsI): FilterTitleItemI => {
    const value = typeof pageTitle === 'string' ? pageTitle : 'Unknown';

    if (searchParams.get('componentName')) {
        return {label: 'Components', value};
    }

    if (searchParams.get(groupSearchParamName)) {
        return {label: groupLabel, value};
    }

    if (searchParams.get('tagId')) {
        return {label: 'Tags', value};
    }

    return {label: 'Components', value: 'All Components'};
};
