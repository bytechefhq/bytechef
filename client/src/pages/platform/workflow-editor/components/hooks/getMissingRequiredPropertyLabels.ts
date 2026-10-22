import {PropertyAllType} from '@/shared/types';

import {WorkflowNodeDetailsErrorI} from './getMissingRequiredConnectionErrors';

export default function getMissingRequiredPropertyLabels(
    errors: WorkflowNodeDetailsErrorI[],
    properties: PropertyAllType[]
): string[] {
    const propertyIndex = (name: string) => {
        const index = properties.findIndex((property) => property.name === name);

        return index === -1 ? properties.length : index;
    };

    return errors
        .filter((error) => error.kind === 'PROPERTY')
        .map((error) => error.name)
        .sort((firstName, secondName) => propertyIndex(firstName) - propertyIndex(secondName))
        .map((name) => properties.find((property) => property.name === name)?.label || name);
}
