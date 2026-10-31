import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {XIcon} from 'lucide-react';

import {AUDIT_EVENT_DATE_RANGE_PRESETS, AUDIT_EVENT_PLATFORMS} from '../constants';
import {AuditEventFiltersI} from '../types';

interface AuditEventsActiveFilterBadgesProps {
    filters: AuditEventFiltersI;
    hasActiveFilters: boolean;
    onClearDateRange: () => void;
    onClearEventType: () => void;
    onClearFromDate: () => void;
    onClearPlatform: () => void;
    onClearToDate: () => void;
    onResetFilters: () => void;
}

interface ActiveFilterBadgeProps {
    clearLabel: string;
    label: string;
    onClear: () => void;
}

const formatDate = (value: number) => new Date(value).toLocaleDateString();

const ActiveFilterBadge = ({clearLabel, label, onClear}: ActiveFilterBadgeProps) => (
    <Badge className="text-xs" styleType="primary-outline">
        <span>{label}</span>

        <Button
            aria-label={clearLabel}
            className="ml-1 size-4 p-0"
            icon={<XIcon className="size-3" />}
            onClick={onClear}
            size="iconXxs"
            variant="ghost"
        />
    </Badge>
);

const AuditEventsActiveFilterBadges = ({
    filters,
    hasActiveFilters,
    onClearDateRange,
    onClearEventType,
    onClearFromDate,
    onClearPlatform,
    onClearToDate,
    onResetFilters,
}: AuditEventsActiveFilterBadgesProps) => {
    if (!hasActiveFilters) {
        return null;
    }

    const {dateRange, eventType, fromDate, platform, toDate} = filters;

    const dateRangePreset = AUDIT_EVENT_DATE_RANGE_PRESETS.find(
        (dateRangePresetItem) => dateRangePresetItem.value === dateRange
    );

    const selectedPlatform = AUDIT_EVENT_PLATFORMS.find((platformItem) => platformItem.value === platform);

    return (
        <div className="flex flex-wrap items-center gap-2">
            {eventType && (
                <ActiveFilterBadge
                    clearLabel="Clear event type filter"
                    label={`Event type: ${eventType}`}
                    onClear={onClearEventType}
                />
            )}

            {selectedPlatform && (
                <ActiveFilterBadge
                    clearLabel="Clear platform filter"
                    label={`Platform: ${selectedPlatform.label}`}
                    onClear={onClearPlatform}
                />
            )}

            {dateRangePreset && (
                <ActiveFilterBadge
                    clearLabel="Clear date filter"
                    label={dateRangePreset.label}
                    onClear={onClearDateRange}
                />
            )}

            {dateRange === 'custom' && fromDate && (
                <ActiveFilterBadge
                    clearLabel="Clear from date filter"
                    label={`From: ${formatDate(fromDate)}`}
                    onClear={onClearFromDate}
                />
            )}

            {dateRange === 'custom' && toDate && (
                <ActiveFilterBadge
                    clearLabel="Clear to date filter"
                    label={`To: ${formatDate(toDate)}`}
                    onClear={onClearToDate}
                />
            )}

            <Button label="Clear all" onClick={onResetFilters} size="xs" variant="link" />
        </div>
    );
};

export default AuditEventsActiveFilterBadges;
