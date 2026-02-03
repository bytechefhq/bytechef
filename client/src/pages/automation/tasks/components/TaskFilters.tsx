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
} from '@/components/ui/dropdown-menu';
import {Check, CheckCircle2, Circle, Clock, SlidersHorizontal, User, X} from 'lucide-react';

import {useTaskFilters} from './hooks/useTaskFilters';

export default function TaskFilters() {
    const {
        assigneeCounts,
        assignees,
        filters,
        handleAssigneeChange,
        handlePriorityChange,
        handleResetFilters,
        handleStatusChange,
        hasActiveFilters,
        priorityCounts,
        taskCounts,
    } = useTaskFilters();

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    aria-label="Filter options"
                    className="size-6 shrink-0"
                    icon={<SlidersHorizontal className="size-4" />}
                    size="icon"
                    variant={hasActiveFilters ? 'default' : 'ghost'}
                />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuLabel>Filter Tasks</DropdownMenuLabel>

                <DropdownMenuSeparator />

                {/* Status Section */}

                <DropdownMenuGroup>
                    <DropdownMenuLabel className="text-xs font-normal text-muted-foreground">Status</DropdownMenuLabel>

                    <DropdownMenuItem
                        className="flex items-center justify-between"
                        onClick={() => handleStatusChange('all')}
                    >
                        <span>All</span>

                        {filters.status === 'all' && <Check className="size-4" />}

                        <span className="ml-auto text-xs text-muted-foreground">({taskCounts.all})</span>
                    </DropdownMenuItem>

                    <DropdownMenuItem
                        className="flex items-center justify-between"
                        onClick={() => handleStatusChange('open')}
                    >
                        <div className="flex items-center">
                            <Circle className="mr-2 size-3 text-gray-400" />

                            <span>Open</span>
                        </div>

                        {filters.status === 'open' && <Check className="size-4" />}

                        <span className="ml-auto text-xs text-muted-foreground">({taskCounts.open})</span>
                    </DropdownMenuItem>

                    <DropdownMenuItem
                        className="flex items-center justify-between"
                        onClick={() => handleStatusChange('in-progress')}
                    >
                        <div className="flex items-center">
                            <Clock className="mr-2 size-3 text-blue-600" />

                            <span>In Progress</span>
                        </div>

                        {filters.status === 'in-progress' && <Check className="size-4" />}

                        <span className="ml-auto text-xs text-muted-foreground">({taskCounts['in-progress']})</span>
                    </DropdownMenuItem>

                    <DropdownMenuItem
                        className="flex items-center justify-between"
                        onClick={() => handleStatusChange('completed')}
                    >
                        <div className="flex items-center">
                            <CheckCircle2 className="mr-2 size-3 text-green-600" />

                            <span>Completed</span>
                        </div>

                        {filters.status === 'completed' && <Check className="size-4" />}

                        <span className="ml-auto text-xs text-muted-foreground">({taskCounts.completed})</span>
                    </DropdownMenuItem>
                </DropdownMenuGroup>

                <DropdownMenuSeparator />

                {/* Priority Section */}

                <DropdownMenuGroup>
                    <DropdownMenuLabel className="text-xs font-normal text-muted-foreground">
                        Priority
                    </DropdownMenuLabel>

                    <DropdownMenuItem
                        className="flex items-center justify-between"
                        onClick={() => handlePriorityChange('all')}
                    >
                        <span>All</span>

                        {filters.priority === 'all' && <Check className="size-4" />}

                        <span className="ml-auto text-xs text-muted-foreground">({priorityCounts.all})</span>
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

                        {filters.priority === 'high' && <Check className="size-4" />}

                        <span className="ml-auto text-xs text-muted-foreground">({priorityCounts.high})</span>
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

                        {filters.priority === 'medium' && <Check className="size-4" />}

                        <span className="ml-auto text-xs text-muted-foreground">({priorityCounts.medium})</span>
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

                        {filters.priority === 'low' && <Check className="size-4" />}

                        <span className="ml-auto text-xs text-muted-foreground">({priorityCounts.low})</span>
                    </DropdownMenuItem>
                </DropdownMenuGroup>

                <DropdownMenuSeparator />

                {/* Assignee Section */}

                <DropdownMenuGroup>
                    <DropdownMenuLabel className="text-xs font-normal text-muted-foreground">
                        Assignee
                    </DropdownMenuLabel>

                    <DropdownMenuItem
                        className="flex items-center justify-between"
                        onClick={() => handleAssigneeChange('all')}
                    >
                        <span>All</span>

                        {filters.assignee === 'all' && <Check className="size-4" />}

                        <span className="ml-auto text-xs text-muted-foreground">({assigneeCounts.all})</span>
                    </DropdownMenuItem>

                    {assignees.map((assignee) => (
                        <DropdownMenuItem
                            className="flex items-center justify-between"
                            key={assignee}
                            onClick={() => handleAssigneeChange(assignee)}
                        >
                            <div className="flex items-center">
                                <User className="mr-2 size-3 text-muted-foreground" />

                                <span className="truncate">{assignee}</span>
                            </div>

                            {filters.assignee === assignee && <Check className="size-4" />}

                            <span className="ml-auto text-xs text-muted-foreground">({assigneeCounts[assignee]})</span>
                        </DropdownMenuItem>
                    ))}
                </DropdownMenuGroup>

                <DropdownMenuSeparator />

                {/* Clear All Filters */}

                <DropdownMenuItem
                    className="text-primary focus:text-primary"
                    disabled={!hasActiveFilters}
                    onClick={handleResetFilters}
                >
                    <X className="mr-2 size-4" />
                    Clear all filters
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
