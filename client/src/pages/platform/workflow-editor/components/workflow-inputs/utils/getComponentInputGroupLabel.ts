import {PropertyGroup} from '@/shared/middleware/platform/configuration';
import {PropertyAllType} from '@/shared/types';

export function getComponentInputGroupLabel(group: PropertyGroup): string {
    if (group.label) {
        return group.label;
    }

    if (group.properties?.length === 1) {
        const [property] = group.properties as PropertyAllType[];

        if (property?.label) {
            return property.label;
        }
    }

    return group.name;
}
