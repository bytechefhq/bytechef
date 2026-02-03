import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {XIcon} from 'lucide-react';

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

                    <Button
                        aria-label="Clear status filter"
                        className="ml-1 size-4 p-0"
                        icon={<XIcon className="size-3" />}
                        onClick={handleClearStatusFilter}
                        size="iconXxs"
                        variant="ghost"
                    />
                </Badge>
            )}

            {filters.priority !== 'all' && (
                <Badge className="text-xs" styleType="primary-outline">
                    <span>Priority: {getPriorityLabel(filters.priority)}</span>

                    <Button
                        aria-label="Clear priority filter"
                        className="ml-1 size-4 p-0"
                        icon={<XIcon className="size-3" />}
                        onClick={handleClearPriorityFilter}
                        size="iconXxs"
                        variant="ghost"
                    />
                </Badge>
            )}

            {filters.assignee !== 'all' && (
                <Badge className="text-xs" styleType="primary-outline">
                    <span>Assignee: {filters.assignee}</span>

                    <Button
                        aria-label="Clear assignee filter"
                        className="ml-1 size-4 p-0"
                        icon={<XIcon className="size-3" />}
                        onClick={handleClearAssigneeFilter}
                        size="iconXxs"
                        variant="ghost"
                    />
                </Badge>
            )}

            <Button label="Clear all" onClick={handleResetFilters} size="xs" variant="link" />
        </div>
    );
}
