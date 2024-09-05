import {PropertyAllType} from '@/shared/types';

export default function getFilteredProperties({
    filterQuery,
    properties,
}: {
    properties: Array<PropertyAllType>;
    filterQuery: string;
}) {
    return properties?.reduce((previousValue: Array<PropertyAllType>, currentValue) => {
        const subProperties = getFilteredProperties({
            filterQuery,
            properties: currentValue.properties || currentValue.items || [],
        });

        if (currentValue.name?.toLowerCase().includes(filterQuery.toLowerCase()) || subProperties.length) {
            previousValue.push(Object.assign({}, currentValue));
        }

        return previousValue;
    }, []);
}
