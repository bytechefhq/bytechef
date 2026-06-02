import {PropertyGroup} from '@/shared/middleware/platform/configuration';
import {PropertyAllType} from '@/shared/types';

/**
 * Resolves the display label for a component input group. Explicit groups carry their own label; a
 * lone-property group (the `.inputs(property)` form) has no group label, so fall back to the single
 * property's label before the group name.
 */
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
