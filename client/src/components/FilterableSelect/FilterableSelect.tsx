import {
    Command,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
    CommandList,
    CommandSeparator,
} from '@/components/ui/command';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {CheckIcon, ChevronDownIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

export type FilterableSelectItemType = {
    label: string;
    value: string;
};

interface FilterableSelectProps {
    /** Accessible name for the trigger. Its visible content is the selected item, which on its own
     *  says nothing about what is being chosen. */
    ariaLabel: string;
    className?: string;
    emptyMessage?: string;
    items: FilterableSelectItemType[];
    onValueChange: (value: string) => void;
    /** Items rendered above the filterable list, separated from it, e.g. "Current project". */
    pinnedItems?: FilterableSelectItemType[];
    searchPlaceholder?: string;
    /** Text shown on the closed trigger. Independent of the selected item's label, so a call site
     *  can render a shorthand such as "Current project" instead of the project name. */
    triggerLabel: string;
    /** Rendered as a tooltip on the trigger, for labels too long to fit. */
    tooltip?: string;
    value: string;
}

const matchesSearch = (item: FilterableSelectItemType, search: string) => item.label.toLowerCase().includes(search);

/**
 * A single-select dropdown whose options can be narrowed by typing.
 *
 * Radix `Select` cannot host a text field — it owns keyboard input for its own jump-to-first-match
 * typeahead and never moves focus off the option list — so this is a `Popover` wrapping a `cmdk`
 * `Command`, styled to match `SelectTrigger`.
 *
 * Filtering is done here rather than by `cmdk` because `cmdk` matches on each item's `value`, which
 * is an entity id, and re-ranks items by fuzzy score as you type. Matching on `label` instead keeps
 * the list in the order the call site supplied it.
 */
const FilterableSelect = ({
    ariaLabel,
    className,
    emptyMessage = 'No results found.',
    items,
    onValueChange,
    pinnedItems = [],
    searchPlaceholder = 'Search...',
    tooltip,
    triggerLabel,
    value,
}: FilterableSelectProps) => {
    const [open, setOpen] = useState(false);
    const [search, setSearch] = useState('');

    const normalizedSearch = search.trim().toLowerCase();

    const filteredPinnedItems = useMemo(
        () => pinnedItems.filter((pinnedItem) => matchesSearch(pinnedItem, normalizedSearch)),
        [pinnedItems, normalizedSearch]
    );

    const filteredItems = useMemo(
        () => items.filter((item) => matchesSearch(item, normalizedSearch)),
        [items, normalizedSearch]
    );

    const handleOpenChange = (nextOpen: boolean) => {
        setOpen(nextOpen);

        if (!nextOpen) {
            setSearch('');
        }
    };

    const handleSelect = (selectedValue: string) => {
        setOpen(false);
        setSearch('');

        onValueChange(selectedValue);
    };

    const renderItem = (item: FilterableSelectItemType, cmdkValue: string) => (
        <CommandItem
            className="cursor-pointer rounded-none hover:bg-surface-neutral-primary-hover"
            key={cmdkValue}
            onSelect={() => handleSelect(item.value)}
            title={item.label.length > 40 ? item.label : undefined}
            value={cmdkValue}
        >
            <span className="truncate">{item.label}</span>

            {item.value === value && <CheckIcon className="ml-auto size-4" />}
        </CommandItem>
    );

    const trigger = (
        <PopoverTrigger
            aria-expanded={open}
            aria-label={ariaLabel}
            className={twMerge(
                'flex h-9 w-full items-center justify-between gap-2 rounded-md border border-stroke-neutral-secondary bg-background px-3 py-2 text-sm transition-[color,box-shadow] outline-none hover:bg-surface-neutral-primary-hover focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50',
                className
            )}
            role="combobox"
        >
            <span className="truncate">{triggerLabel}</span>

            <ChevronDownIcon className="size-4 min-w-4 opacity-50" />
        </PopoverTrigger>
    );

    return (
        <Popover onOpenChange={handleOpenChange} open={open}>
            {tooltip ? (
                <Tooltip>
                    <TooltipTrigger asChild>{trigger}</TooltipTrigger>

                    <TooltipContent>{tooltip}</TooltipContent>
                </Tooltip>
            ) : (
                trigger
            )}

            <PopoverContent align="start" className="w-(--radix-popover-trigger-width) p-0">
                <Command shouldFilter={false}>
                    <CommandInput
                        className="h-9 border-none ring-0"
                        onValueChange={setSearch}
                        placeholder={searchPlaceholder}
                        value={search}
                    />

                    <CommandList>
                        <CommandEmpty>{emptyMessage}</CommandEmpty>

                        {filteredPinnedItems.length > 0 && (
                            <CommandGroup>
                                {filteredPinnedItems.map((pinnedItem, index) =>
                                    renderItem(pinnedItem, `pinned-${index}`)
                                )}
                            </CommandGroup>
                        )}

                        {filteredPinnedItems.length > 0 && filteredItems.length > 0 && <CommandSeparator />}

                        {filteredItems.length > 0 && (
                            <CommandGroup>
                                {filteredItems.map((item, index) => renderItem(item, `item-${index}`))}
                            </CommandGroup>
                        )}
                    </CommandList>
                </Command>
            </PopoverContent>
        </Popover>
    );
};

export default FilterableSelect;
