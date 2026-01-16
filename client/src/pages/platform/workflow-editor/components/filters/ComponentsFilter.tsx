import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuCheckboxItem,
    DropdownMenuContent,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Input} from '@/components/ui/input';
import {ScrollArea} from '@/components/ui/scroll-area';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {BotIcon, CircleMinusIcon, ListFilterIcon, SearchIcon, XIcon} from 'lucide-react';
import {ReactNode, useCallback} from 'react';
import {twMerge} from 'tailwind-merge';

interface FilterStateProps {
    activeView: 'all' | 'filtered';
    selectedCategories: string[];
    searchValue: string;
    filteredCount: number;
}

interface FilterCategoryProps {
    label: string;
    icon?: ReactNode;
}

interface FilterConfigProps {
    label: string;
    tooltip: string;
}

interface ComponentsFilterProps {
    componentDefinitions: ComponentDefinitionBasic[];
    deselectAllCategories: () => void;
    filterConfig: FilterConfigProps;
    filterState: FilterStateProps;
    filteredCategories: FilterCategoryProps[];
    filteredComponents: ComponentDefinitionBasic[];
    setActiveView: (view: 'all' | 'filtered') => void;
    setSearchValue: (value: string) => void;
    toggleCategory: (category: string) => void;
}

const ComponentsFilter = ({
    componentDefinitions,
    deselectAllCategories,
    filterConfig,
    filterState,
    filteredCategories,
    filteredComponents,
    setActiveView,
    setSearchValue,
    toggleCategory,
}: ComponentsFilterProps) => {
    const handleDropdownOpenChange = useCallback(
        (open: boolean) => {
            if (open) {
                setSearchValue('');
            }
        },
        [setSearchValue]
    );

    return (
        <div className="flex justify-between px-3 pb-2">
            <div>
                <Button
                    aria-label="All button"
                    className={twMerge(
                        'text-xs text-content-neutral-secondary hover:bg-transparent hover:text-content-neutral-primary',
                        (filterState.activeView === 'all' || filterState.selectedCategories.length === 0) &&
                            'bg-surface-brand-secondary text-content-brand-primary hover:bg-surface-brand-secondary hover:text-content-brand-primary disabled:opacity-100'
                    )}
                    disabled={filterState.activeView === 'all'}
                    onClick={() => setActiveView('all')}
                    variant="ghost"
                >
                    <span>All</span>

                    <div className="rounded-md bg-background px-2 py-1">{componentDefinitions.length}</div>
                </Button>

                <Button
                    aria-label="Filtered button"
                    className={twMerge(
                        'text-xs text-content-neutral-secondary hover:bg-transparent hover:text-content-neutral-primary',
                        filterState.activeView === 'filtered' &&
                            'bg-surface-brand-secondary text-content-brand-primary hover:bg-surface-brand-secondary hover:text-content-brand-primary disabled:opacity-100',
                        filterState.selectedCategories.length > 0 ? 'visible' : 'invisible'
                    )}
                    disabled={filterState.activeView === 'filtered'}
                    onClick={() => setActiveView('filtered')}
                    variant="ghost"
                >
                    <span>Filtered</span>

                    <div className="rounded-md bg-background px-2 py-1">
                        {filterState.activeView === 'filtered' ? filteredComponents.length : filterState.filteredCount}
                    </div>
                </Button>
            </div>

            <DropdownMenu modal={false} onOpenChange={handleDropdownOpenChange}>
                <Tooltip>
                    <DropdownMenuTrigger asChild>
                        <TooltipTrigger asChild>
                            <Button
                                aria-label={`Filter ${filterConfig.label}`}
                                className="data-[state=open]:bg-surface-brand-secondary data-[state=open]:text-content-brand-primary"
                                icon={<ListFilterIcon />}
                                size="iconSm"
                                variant="ghost"
                            />
                        </TooltipTrigger>
                    </DropdownMenuTrigger>

                    <TooltipContent>{filterConfig.tooltip}</TooltipContent>
                </Tooltip>

                <DropdownMenuContent align="start" className="mr-2 overflow-hidden p-1">
                    <div className="mb-1 flex items-center">
                        <div className="relative w-full rounded-md bg-background">
                            <SearchIcon className="absolute left-3 top-2.5 size-4 text-muted-foreground" />

                            <Input
                                aria-label="Find category"
                                className="pl-9 pr-7 text-sm"
                                onChange={(event) => setSearchValue(event.target.value)}
                                onKeyDown={(event) => event.stopPropagation()}
                                placeholder="Find category"
                                value={filterState.searchValue}
                            />

                            {filterState.searchValue !== '' && (
                                <Button
                                    aria-label="Clear search input"
                                    className="absolute right-1 top-2.5 mx-1 h-auto p-0 text-muted-foreground hover:bg-transparent hover:text-muted-foreground active:bg-transparent active:text-muted-foreground"
                                    icon={<XIcon />}
                                    onClick={() => setSearchValue('')}
                                    variant="ghost"
                                />
                            )}
                        </div>
                    </div>

                    <Button
                        aria-label="Deselect button"
                        className={twMerge(
                            'h-auto w-full justify-start px-3 py-1.5 font-normal opacity-100',
                            filterState.selectedCategories.length > 0 ? 'inline-flex' : 'hidden'
                        )}
                        icon={<CircleMinusIcon />}
                        label="Clear all selected categories"
                        onClick={() => deselectAllCategories()}
                        variant="destructiveGhost"
                    />

                    <ScrollArea className="h-48 overflow-y-auto pr-1">
                        {filteredCategories.length === 0 && (
                            <span className="block w-task-filter-dropdown-menu-width px-3 py-2 text-xs text-content-neutral-secondary">
                                No categories found
                            </span>
                        )}

                        {filteredCategories.map((category) => (
                            <DropdownMenuCheckboxItem
                                checked={filterState.selectedCategories.includes(category.label)}
                                className="flex w-task-filter-dropdown-menu-width cursor-pointer items-center justify-between gap-5 rounded-md px-3 hover:bg-surface-neutral-primary-hover [&>span:first-child]:static [&>span:first-child]:order-last"
                                key={category.label}
                                onCheckedChange={() => toggleCategory(category.label)}
                                onPointerLeave={(event) => event.preventDefault()}
                                onPointerMove={(event) => event.preventDefault()}
                                onSelect={(event) => event.preventDefault()}
                            >
                                <div className="flex items-center gap-2">
                                    <span className="[&>svg]:size-4">
                                        {category.icon ? category.icon : <BotIcon />}
                                    </span>

                                    <span>{category.label}</span>
                                </div>
                            </DropdownMenuCheckboxItem>
                        ))}
                    </ScrollArea>
                </DropdownMenuContent>
            </DropdownMenu>
        </div>
    );
};

export default ComponentsFilter;
