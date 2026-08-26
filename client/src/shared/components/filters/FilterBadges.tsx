import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    type FilterGroupI,
    hasActiveFilters,
    isFilterGroupActive,
    resetFilterGroups,
} from '@/shared/components/filters/FilterMenu';
import {XIcon} from 'lucide-react';

interface FilterBadgesPropsI {
    groups: FilterGroupI[];
}

/**
 * The chips reporting what {@link FilterMenu} currently has applied, each clearable on its own.
 *
 * These carry the whole burden of saying what is filtered, since the menu collapses to one icon — so render
 * them somewhere that survives an empty result set. Filtering down to nothing is exactly when a user needs to
 * see, and undo, the filter that emptied the page.
 */
const FilterBadges = ({groups}: FilterBadgesPropsI) => {
    if (!hasActiveFilters(groups)) {
        return null;
    }

    return (
        <div className="flex flex-wrap items-center gap-2">
            {groups.filter(isFilterGroupActive).map((group) => (
                <Badge className="text-xs" key={group.key} styleType="primary-outline">
                    <span>
                        {group.label}: {group.options.find((option) => option.value === group.value)?.label}
                    </span>

                    <Button
                        aria-label={`Clear ${group.label.toLowerCase()} filter`}
                        className="ml-1 size-4 p-0"
                        icon={<XIcon className="size-3" />}
                        onClick={() => group.onChange(group.allValue)}
                        size="iconXxs"
                        variant="ghost"
                    />
                </Badge>
            ))}

            <Button label="Clear all" onClick={() => resetFilterGroups(groups)} size="xs" variant="link" />
        </div>
    );
};

export default FilterBadges;
