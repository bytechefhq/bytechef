import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {SORT_OPTIONS} from '@/shared/constants';
import {CheckIcon, ListFilterIcon, SearchIcon} from 'lucide-react';
import {forwardRef} from 'react';

interface AutomationWorkflowEditorWorkflowsFilterProps {
    searchValue: string;
    setSearchValue: (value: string) => void;
    setSortBy: (value: string) => void;
    sortBy: string;
}

const AutomationWorkflowEditorWorkflowsFilter = forwardRef<
    HTMLInputElement,
    AutomationWorkflowEditorWorkflowsFilterProps
>(({searchValue, setSearchValue, setSortBy, sortBy}, ref) => {
    return (
        <div className="flex items-center gap-2">
            <div className="relative flex w-full rounded-md bg-background align-middle">
                <SearchIcon className="absolute top-2.5 left-3 size-4 text-muted-foreground" />

                <Input
                    className="border-stroke-neutral-secondary py-1.5 pl-8 shadow-none"
                    onChange={(event) => setSearchValue(event.target.value)}
                    placeholder="Search workflows"
                    ref={ref}
                    value={searchValue}
                />
            </div>

            <DropdownMenu>
                <Tooltip>
                    <DropdownMenuTrigger asChild>
                        <TooltipTrigger asChild>
                            <Button
                                aria-label="Sort by"
                                className="data-[state=open]:border-stroke-brand-secondary data-[state=open]:bg-surface-brand-secondary data-[state=open]:text-content-brand-primary"
                                icon={<ListFilterIcon />}
                                size="icon"
                                variant="outline"
                            />
                        </TooltipTrigger>
                    </DropdownMenuTrigger>

                    <TooltipContent>Sort workflows</TooltipContent>
                </Tooltip>

                <DropdownMenuContent align="end">
                    {SORT_OPTIONS.map((option) => (
                        <DropdownMenuItem
                            className="flex cursor-pointer items-center justify-between rounded-none hover:bg-surface-neutral-primary-hover"
                            key={option.value}
                            onClick={() => setSortBy(option.value)}
                        >
                            {option.label}

                            {sortBy === option.value && <CheckIcon className="size-4" />}
                        </DropdownMenuItem>
                    ))}
                </DropdownMenuContent>
            </DropdownMenu>
        </div>
    );
});

AutomationWorkflowEditorWorkflowsFilter.displayName = 'AutomationWorkflowEditorWorkflowsFilter';

export default AutomationWorkflowEditorWorkflowsFilter;
