import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {ArrowUpDown} from 'lucide-react';

import {useTaskSortMenu} from './hooks/useTaskSortMenu';

import type {SortDirectionType, SortOptionType} from '../types/types';

interface TaskSortMenuProps {
    sortBy: SortOptionType;
    sortDirection: SortDirectionType;
    onSortChange: (sortBy: SortOptionType, direction: SortDirectionType) => void;
}

const SORT_OPTIONS = [
    {label: 'Created Date', value: 'created'},
    {label: 'Title', value: 'title'},
    {label: 'Priority', value: 'priority'},
    {label: 'Status', value: 'status'},
    {label: 'Assignee', value: 'assignee'},
    {label: 'Due Date', value: 'dueDate'},
] as const;

export default function TaskSortMenu({onSortChange, sortBy, sortDirection}: TaskSortMenuProps) {
    const {handleSortOptionClick} = useTaskSortMenu({onSortChange, sortBy, sortDirection});

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    aria-label="Sort options"
                    className="size-6 shrink-0"
                    icon={<ArrowUpDown className="size-4" />}
                    size="icon"
                    variant="ghost"
                />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end" className="w-48">
                <DropdownMenuLabel>Sort by</DropdownMenuLabel>

                <DropdownMenuSeparator />

                {SORT_OPTIONS.map((option) => (
                    <DropdownMenuItem
                        className="flex items-center justify-between"
                        key={option.value}
                        onClick={() => handleSortOptionClick(option.value)}
                    >
                        <span>{option.label}</span>

                        {sortBy === option.value && (
                            <span className="text-xs">{sortDirection === 'asc' ? '↑' : '↓'}</span>
                        )}
                    </DropdownMenuItem>
                ))}
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
