import {PropertyType} from '@/types/projectTypes';

export default function getFilteredProperties({
    filterQuery,
    properties,
}: {
    properties: PropertyType[];
    filterQuery: string;
}) {
    return properties?.reduce((previousValue: PropertyType[], currentValue) => {
        const subProperties = getFilteredProperties({
            filterQuery,
            properties: currentValue.properties || currentValue.items || [],
        });

        if (
            currentValue.name
                ?.toLowerCase()
                .includes(filterQuery.toLowerCase()) ||
            subProperties.length
        ) {
            previousValue.push(Object.assign({}, currentValue));
        }

        return previousValue;
    }, []);
}
