import Badge from '@/components/Badge/Badge';
import {X} from 'lucide-react';

import {getPriorityLabel, getStatusLabel} from '../utils/task-utils';
import {useTaskActiveFilterBadges} from './hooks/useTaskActiveFilterBadges';

export default function TaskActiveFilterBadges() {
    const {
        filters,
        handleClearAssigneeFilter,
        handleClearPriorityFilter,
        handleClearStatusFilter,
        handleResetFilters,
        hasActiveFilters,
    } = useTaskActiveFilterBadges();

    if (!hasActiveFilters) {
        return null;
    }

    return (
        <div className="mb-3 flex flex-wrap items-center gap-2">
            {filters.status !== 'all' && (
                <Badge className="text-xs" styleType="primary-outline">
                    <span>Status: {getStatusLabel(filters.status)}</span>

                    <button
                        aria-label="Clear status filter"
                        className="ml-1 rounded-full p-0.5 hover:bg-muted"
                        onClick={handleClearStatusFilter}
                        type="button"
                    >
                        <X className="size-3" />
                    </button>
                </Badge>
            )}

            {filters.priority !== 'all' && (
                <Badge className="text-xs" styleType="primary-outline">
                    <span>Priority: {getPriorityLabel(filters.priority)}</span>

                    <button
                        aria-label="Clear priority filter"
                        className="ml-1 rounded-full p-0.5 hover:bg-muted"
                        onClick={handleClearPriorityFilter}
                        type="button"
                    >
                        <X className="size-3" />
                    </button>
                </Badge>
            )}

            {filters.assignee !== 'all' && (
                <Badge className="text-xs" styleType="primary-outline">
                    <span>Assignee: {filters.assignee}</span>

                    <button
                        aria-label="Clear assignee filter"
                        className="ml-1 rounded-full p-0.5 hover:bg-muted"
                        onClick={handleClearAssigneeFilter}
                        type="button"
                    >
                        <X className="size-3" />
                    </button>
                </Badge>
            )}

            <button className="text-xs text-primary hover:underline" onClick={handleResetFilters} type="button">
                Clear all
            </button>
        </div>
    );
}
