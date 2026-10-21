import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuGroup,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {CheckCircle2Icon, CheckIcon, CircleIcon, ClockIcon, SlidersHorizontalIcon, UserIcon, XIcon} from 'lucide-react';

import {useApprovalTaskFilters} from './hooks/useApprovalTaskFilters';

export default function ApprovalTaskFilters() {
    const {
        approvalTaskCounts,
        assigneeCounts,
        assignees,
        filters,
        handleAssigneeChange,
        handlePriorityChange,
        handleResetFilters,
        handleStatusChange,
        hasActiveFilters,
        priorityCounts,
    } = useApprovalTaskFilters();

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    aria-label="Filter options"
                    className="size-6 shrink-0"
                    icon={<SlidersHorizontalIcon className="size-4" />}
                    size="icon"
                    variant={hasActiveFilters ? 'default' : 'ghost'}
                />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuLabel>Filter Approval Tasks</DropdownMenuLabel>

                <DropdownMenuSeparator />

                <DropdownMenuGroup>
                    <DropdownMenuLabel>Status</DropdownMenuLabel>

                    <DropdownMenuItem
                        className="flex items-center justify-between"
                        onClick={() => handleStatusChange('all')}
                    >
                        <span>All</span>

                        {filters.status === 'all' && <CheckIcon className="size-4" />}

                        <span className="ml-auto text-xs text-content-neutral-secondary">
                            ({approvalTaskCounts.all})
                        </span>
                    </DropdownMenuItem>

                    <DropdownMenuItem
                        className="flex items-center justify-between"
                        onClick={() => handleStatusChange('open')}
                    >
                        <div className="flex items-center">
                            <CircleIcon className="mr-2 size-3 text-gray-400" />

                            <span>Open</span>
                        </div>

                        {filters.status === 'open' && <CheckIcon className="size-4" />}

                        <span className="ml-auto text-xs text-content-neutral-secondary">
                            ({approvalTaskCounts.open})
                        </span>
                    </DropdownMenuItem>

                    <DropdownMenuItem
                        className="flex items-center justify-between"
                        onClick={() => handleStatusChange('in-progress')}
                    >
                        <div className="flex items-center">
                            <ClockIcon className="mr-2 size-3 text-blue-600" />

                            <span>In Progress</span>
                        </div>

                        {filters.status === 'in-progress' && <CheckIcon className="size-4" />}

                        <span className="ml-auto text-xs text-content-neutral-secondary">
                            ({approvalTaskCounts['in-progress']})
                        </span>
                    </DropdownMenuItem>

                    <DropdownMenuItem
                        className="flex items-center justify-between"
                        onClick={() => handleStatusChange('completed')}
                    >
                        <div className="flex items-center">
                            <CheckCircle2Icon className="mr-2 size-3 text-green-600" />

                            <span>Completed</span>
                        </div>

                        {filters.status === 'completed' && <CheckIcon className="size-4" />}

                        <span className="ml-auto text-xs text-content-neutral-secondary">
                            ({approvalTaskCounts.completed})
                        </span>
                    </DropdownMenuItem>
                </DropdownMenuGroup>

                <DropdownMenuSeparator />

                <DropdownMenuGroup>
                    <DropdownMenuLabel>Priority</DropdownMenuLabel>

                    <DropdownMenuItem
                        className="flex items-center justify-between"
                        onClick={() => handlePriorityChange('all')}
                    >
                        <span>All</span>

                        {filters.priority === 'all' && <CheckIcon className="size-4" />}

                        <span className="ml-auto text-xs text-content-neutral-secondary">({priorityCounts.all})</span>
                    </DropdownMenuItem>

                    <DropdownMenuItem
                        className="flex items-center justify-between"
                        onClick={() => handlePriorityChange('high')}
                    >
                        <div className="flex items-center">
                            <Badge
                                className="mr-2 h-3 border-red-200 bg-red-100 px-1 text-red-800"
                                styleType="outline-outline"
                            >
                                <span className="text-[10px]">High</span>
                            </Badge>

                            <span>High</span>
                        </div>

                        {filters.priority === 'high' && <CheckIcon className="size-4" />}

                        <span className="ml-auto text-xs text-content-neutral-secondary">({priorityCounts.high})</span>
                    </DropdownMenuItem>

                    <DropdownMenuItem
                        className="flex items-center justify-between"
                        onClick={() => handlePriorityChange('medium')}
                    >
                        <div className="flex items-center">
                            <Badge
                                className="mr-2 h-3 border-yellow-200 bg-yellow-100 px-1 text-yellow-800"
                                styleType="outline-outline"
                            >
                                <span className="text-[10px]">Med</span>
                            </Badge>

                            <span>Medium</span>
                        </div>

                        {filters.priority === 'medium' && <CheckIcon className="size-4" />}

                        <span className="ml-auto text-xs text-content-neutral-secondary">
                            ({priorityCounts.medium})
                        </span>
                    </DropdownMenuItem>

                    <DropdownMenuItem
                        className="flex items-center justify-between"
                        onClick={() => handlePriorityChange('low')}
                    >
                        <div className="flex items-center">
                            <Badge
                                className="mr-2 h-3 border-green-200 bg-green-100 px-1 text-green-800"
                                styleType="outline-outline"
                            >
                                <span className="text-[10px]">Low</span>
                            </Badge>

                            <span>Low</span>
                        </div>

                        {filters.priority === 'low' && <CheckIcon className="size-4" />}

                        <span className="ml-auto text-xs text-content-neutral-secondary">({priorityCounts.low})</span>
                    </DropdownMenuItem>
                </DropdownMenuGroup>

                <DropdownMenuSeparator />

                <DropdownMenuGroup>
                    <DropdownMenuLabel>Assignee</DropdownMenuLabel>

                    <DropdownMenuItem
                        className="flex items-center justify-between"
                        onClick={() => handleAssigneeChange('all')}
                    >
                        <span>All</span>

                        {filters.assignee === 'all' && <CheckIcon className="size-4" />}

                        <span className="ml-auto text-xs text-content-neutral-secondary">({assigneeCounts.all})</span>
                    </DropdownMenuItem>

                    {assignees.map((assignee) => (
                        <DropdownMenuItem
                            className="flex items-center justify-between"
                            key={assignee}
                            onClick={() => handleAssigneeChange(assignee)}
                        >
                            <div className="flex items-center">
                                <UserIcon className="mr-2 size-3 text-content-neutral-secondary" />

                                <span className="truncate">{assignee}</span>
                            </div>

                            {filters.assignee === assignee && <CheckIcon className="size-4" />}

                            <span className="ml-auto text-xs text-content-neutral-secondary">
                                ({assigneeCounts[assignee]})
                            </span>
                        </DropdownMenuItem>
                    ))}
                </DropdownMenuGroup>

                <DropdownMenuSeparator />

                <DropdownMenuItem
                    disabled={!hasActiveFilters}
                    icon={<XIcon />}
                    label="Clear all filters"
                    onClick={handleResetFilters}
                />
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
