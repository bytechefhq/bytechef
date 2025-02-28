import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Input} from '@/components/ui/input';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {CheckIcon, ListFilterIcon, SearchIcon} from 'lucide-react';

const WorkflowsListFilter = ({
    searchValue,
    setSearchValue,
    setSortBy,
    sortBy,
}: {
    setSearchValue: (value: string) => void;
    searchValue: string;
    sortBy: string;
    setSortBy: (value: string) => void;
}) => {
    const sortOptions = [
        {
            label: 'Last edited',
            value: 'last-edited',
        },
        {
            label: 'Date created',
            value: 'date-created',
        },
        {
            label: 'A-Z',
            value: 'alphabetical',
        },
        {
            label: 'Z-A',
            value: 'reverse-alphabetical',
        },
    ];

    return (
        <div className="flex w-80 items-center gap-2">
            <div className="relative w-full rounded-md bg-background">
                <SearchIcon className="absolute left-3 top-2.5 size-4 text-muted-foreground" />

                <Input
                    className="border-stroke-neutral-secondary pl-8 shadow-none"
                    onChange={(event) => setSearchValue(event.target.value)}
                    placeholder="Search workflows"
                    value={searchValue}
                />
            </div>

            <DropdownMenu>
                <Tooltip>
                    <DropdownMenuTrigger asChild>
                        <TooltipTrigger asChild>
                            <Button
                                className="border-stroke-neutral-secondary p-2 shadow-none hover:bg-surface-neutral-primary-hover data-[state=open]:border-stroke-brand-secondary data-[state=open]:bg-surface-brand-secondary data-[state=open]:text-content-brand-primary"
                                size="icon"
                                variant="outline"
                            >
                                <ListFilterIcon />
                            </Button>
                        </TooltipTrigger>
                    </DropdownMenuTrigger>

                    <TooltipContent>Sort workflows</TooltipContent>
                </Tooltip>

                <DropdownMenuContent align="end">
                    {sortOptions.map((option) => (
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
};

export default WorkflowsListFilter;
