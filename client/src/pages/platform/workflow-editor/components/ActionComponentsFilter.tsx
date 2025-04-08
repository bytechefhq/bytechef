import {Button} from '@/components/ui/button';
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
import {useCallback} from 'react';
import {twMerge} from 'tailwind-merge';

interface ActionComponentsFilterProps {
    actionComponentDefinitions: ComponentDefinitionBasic[];
    deselectAllCategories: () => void;
    filterState: {
        activeView: 'all' | 'filtered';
        selectedCategories: string[];
        searchValue: string;
        filteredCount: number;
    };
    filteredCategories: {label: string; icon?: React.ReactNode}[];
    filteredComponents: ComponentDefinitionBasic[];
    setActiveView: (view: 'all' | 'filtered') => void;
    setSearchValue: (value: string) => void;
    toggleCategory: (category: string) => void;
}

const ActionComponentsFilter = ({
    actionComponentDefinitions,
    deselectAllCategories,
    filterState,
    filteredCategories,
    filteredComponents,
    setActiveView,
    setSearchValue,
    toggleCategory,
}: ActionComponentsFilterProps) => {
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
                        'border-none bg-transparent text-xs text-content-neutral-secondary shadow-none hover:bg-transparent hover:text-content-neutral-primary',
                        (filterState.activeView === 'all' || filterState.selectedCategories.length === 0) &&
                            'bg-surface-brand-secondary text-content-brand-primary hover:bg-surface-brand-secondary hover:text-content-brand-primary disabled:opacity-100'
                    )}
                    disabled={filterState.activeView === 'all'}
                    onClick={() => setActiveView('all')}
                >
                    <span>All</span>

                    <div className="rounded-md bg-background px-2 py-1">{actionComponentDefinitions.length}</div>
                </Button>

                <Button
                    aria-label="Filtered button"
                    className={twMerge(
                        'border-none bg-transparent text-xs text-content-neutral-secondary shadow-none hover:bg-transparent hover:text-content-neutral-primary',
                        filterState.activeView === 'filtered' &&
                            'bg-surface-brand-secondary text-content-brand-primary hover:bg-surface-brand-secondary hover:text-content-brand-primary disabled:opacity-100',
                        filterState.selectedCategories.length > 0 ? 'visible' : 'invisible'
                    )}
                    disabled={filterState.activeView === 'filtered'}
                    onClick={() => setActiveView('filtered')}
                    variant={filterState.activeView === 'filtered' ? 'default' : 'ghost'}
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
                                aria-label="Filter actions"
                                className="border-none bg-transparent p-2 text-content-neutral-secondary shadow-none hover:bg-transparent hover:text-content-neutral-primary data-[state=open]:bg-surface-brand-secondary data-[state=open]:text-content-brand-primary"
                                size="icon"
                                variant="outline"
                            >
                                <ListFilterIcon />
                            </Button>
                        </TooltipTrigger>
                    </DropdownMenuTrigger>

                    <TooltipContent>Filter actions by category</TooltipContent>
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
                                    className="absolute right-1 top-2.5 mx-1 h-auto p-0 text-muted-foreground hover:bg-transparent hover:text-muted-foreground"
                                    onClick={() => setSearchValue('')}
                                    variant="ghost"
                                >
                                    <XIcon className="size-4 text-muted-foreground" />
                                </Button>
                            )}
                        </div>
                    </div>

                    <Button
                        aria-label="Deselect button"
                        className={twMerge(
                            'flex h-auto w-full justify-start rounded-md bg-transparent px-3 py-1.5 font-normal text-content-destructive shadow-none hover:bg-surface-destructive-secondary hover:text-content-destructive',
                            filterState.selectedCategories.length > 0 ? 'inline-flex' : 'hidden'
                        )}
                        onClick={() => deselectAllCategories()}
                    >
                        <CircleMinusIcon /> Clear all selected categories
                    </Button>

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

export default ActionComponentsFilter;
