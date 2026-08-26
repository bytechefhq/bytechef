import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuGroup,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {CheckIcon, SlidersHorizontalIcon, XIcon} from 'lucide-react';

export interface FilterOptionI {
    /** Omitted where the page cannot count without a second round trip — a count is never guessed. */
    count?: number;
    label: string;
    value: string;
}

export interface FilterGroupI {
    /** The value meaning "no filter on this facet". A group sitting on it is inactive and shows no badge. */
    allValue: string;
    key: string;
    label: string;
    onChange: (value: string) => void;
    options: FilterOptionI[];
    value: string;
}

interface FilterMenuPropsI {
    groups: FilterGroupI[];
    title: string;
}

export const isFilterGroupActive = (group: FilterGroupI): boolean => group.value !== group.allValue;

export const hasActiveFilters = (groups: FilterGroupI[]): boolean => groups.some(isFilterGroupActive);

export const resetFilterGroups = (groups: FilterGroupI[]): void => {
    for (const group of groups) {
        if (isFilterGroupActive(group)) {
            group.onChange(group.allValue);
        }
    }
};

/**
 * One icon button opening every facet a page filters by, grouped and labelled — the Approval Tasks idiom.
 *
 * Facets collapse into a single control rather than sitting inline as one dropdown each, which keeps a page
 * header the same width whether it filters by one facet or five. What is currently selected is reported by
 * the {@link FilterBadges} chips beside the search box, not by the trigger, so the trigger only has to say
 * whether ANY filter is on — hence the filled variant when it is.
 */
const FilterMenu = ({groups, title}: FilterMenuPropsI) => {
    const active = hasActiveFilters(groups);

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    aria-label={active ? `${title} (filters active)` : title}
                    icon={<SlidersHorizontalIcon className="size-4" />}
                    size="icon"
                    variant={active ? 'default' : 'ghost'}
                />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuLabel>{title}</DropdownMenuLabel>

                {groups.map((group) => (
                    <div key={group.key}>
                        <DropdownMenuSeparator />

                        <DropdownMenuGroup>
                            <DropdownMenuLabel className="text-xs font-normal text-muted-foreground">
                                {group.label}
                            </DropdownMenuLabel>

                            {group.options.map((option) => (
                                <DropdownMenuItem
                                    className="flex items-center justify-between"
                                    key={option.value}
                                    onClick={() => group.onChange(option.value)}
                                >
                                    <span className="truncate">{option.label}</span>

                                    {group.value === option.value && <CheckIcon className="size-4 shrink-0" />}

                                    {option.count !== undefined && (
                                        <span className="ml-auto text-xs text-muted-foreground">({option.count})</span>
                                    )}
                                </DropdownMenuItem>
                            ))}
                        </DropdownMenuGroup>
                    </div>
                ))}

                <DropdownMenuSeparator />

                <DropdownMenuItem
                    className="text-primary focus:text-primary"
                    disabled={!active}
                    onClick={() => resetFilterGroups(groups)}
                >
                    <XIcon className="mr-2 size-4" />
                    Clear all filters
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default FilterMenu;
