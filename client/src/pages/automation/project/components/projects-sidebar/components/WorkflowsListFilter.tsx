import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Input} from '@/components/ui/input';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {SORT_OPTIONS} from '@/shared/constants';
import {CheckIcon, ListFilterIcon, SearchIcon} from 'lucide-react';
import {forwardRef} from 'react';

interface WorkflowsListFilterProps {
    setSearchValue: (value: string) => void;
    searchValue: string;
    sortBy: string;
    setSortBy: (value: string) => void;
}

const WorkflowsListFilter = forwardRef<HTMLInputElement, WorkflowsListFilterProps>(
    ({searchValue, setSearchValue, setSortBy, sortBy}, ref) => {
        return (
            <div className="flex items-center gap-2">
                <div className="relative flex w-full rounded-md bg-background align-middle">
                    <SearchIcon className="absolute left-3 top-2.5 size-4 text-muted-foreground" />

                    <Input
                        className="h-auto border-stroke-neutral-secondary py-1.5 pl-8 shadow-none"
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
                                    className="h-auto border-stroke-neutral-secondary p-2 shadow-none hover:bg-surface-neutral-primary-hover data-[state=open]:border-stroke-brand-secondary data-[state=open]:bg-surface-brand-secondary data-[state=open]:text-content-brand-primary"
                                    variant="outline"
                                >
                                    <ListFilterIcon />
                                </Button>
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
    }
);

WorkflowsListFilter.displayName = 'WorkflowsListFilter';

export default WorkflowsListFilter;
