import Button from '@/components/Button/Button';
import DatePicker from '@/components/DatePicker/DatePicker';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuGroup,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {Popover, PopoverAnchor, PopoverContent} from '@/components/ui/popover';
import {PlatformType, useAuditEventTypesQuery} from '@/shared/middleware/graphql';
import {CalendarRangeIcon, CheckIcon, SlidersHorizontalIcon, XIcon} from 'lucide-react';
import {useRef, useState} from 'react';

import {AUDIT_EVENT_DATE_RANGE_PRESETS, AUDIT_EVENT_PLATFORMS} from '../constants';
import {AuditEventDateRangePresetType, AuditEventFiltersI} from '../types';

interface AuditEventsFiltersProps {
    filters: AuditEventFiltersI;
    hasActiveFilters: boolean;
    onClearDateRange: () => void;
    onCustomFromDateChange: (date?: Date) => void;
    onCustomToDateChange: (date?: Date) => void;
    onDateRangePresetChange: (dateRangePreset: AuditEventDateRangePresetType) => void;
    onEventTypeChange: (eventType?: string) => void;
    onPlatformChange: (platform?: PlatformType) => void;
    onResetFilters: () => void;
}

const AuditEventsFilters = ({
    filters,
    hasActiveFilters,
    onClearDateRange,
    onCustomFromDateChange,
    onCustomToDateChange,
    onDateRangePresetChange,
    onEventTypeChange,
    onPlatformChange,
    onResetFilters,
}: AuditEventsFiltersProps) => {
    const [customRangeOpen, setCustomRangeOpen] = useState(false);

    const customRangeRequestedRef = useRef(false);

    const {data: eventTypesData, isError: isEventTypesError} = useAuditEventTypesQuery();

    const customFromDate = filters.dateRange === 'custom' && filters.fromDate ? new Date(filters.fromDate) : undefined;
    const customToDate = filters.dateRange === 'custom' && filters.toDate ? new Date(filters.toDate) : undefined;
    const eventTypes = eventTypesData?.auditEventTypes || [];

    return (
        <Popover onOpenChange={setCustomRangeOpen} open={customRangeOpen}>
            <PopoverAnchor asChild>
                <div className="flex">
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

                        <DropdownMenuContent
                            align="end"
                            className="w-64"
                            onCloseAutoFocus={(event) => {
                                if (customRangeRequestedRef.current) {
                                    customRangeRequestedRef.current = false;

                                    event.preventDefault();
                                }
                            }}
                        >
                            <DropdownMenuLabel>Filter Audit Events</DropdownMenuLabel>

                            <DropdownMenuSeparator />

                            <DropdownMenuGroup>
                                <DropdownMenuLabel>Event type</DropdownMenuLabel>

                                <DropdownMenuGroup aria-label="Event types" className="max-h-64 overflow-y-auto">
                                    <DropdownMenuItem
                                        className="flex items-center justify-between"
                                        onClick={() => onEventTypeChange(undefined)}
                                    >
                                        <span>All</span>

                                        {!filters.eventType && <CheckIcon className="size-4" />}
                                    </DropdownMenuItem>

                                    {eventTypes.map((eventType) => (
                                        <DropdownMenuItem
                                            className="flex items-center justify-between"
                                            key={eventType}
                                            onClick={() => onEventTypeChange(eventType)}
                                        >
                                            <span className="truncate" title={eventType}>
                                                {eventType}
                                            </span>

                                            {filters.eventType === eventType && <CheckIcon className="size-4" />}
                                        </DropdownMenuItem>
                                    ))}

                                    {isEventTypesError && (
                                        <p className="px-2 py-1.5 text-xs text-destructive">
                                            Event types could not be loaded.
                                        </p>
                                    )}
                                </DropdownMenuGroup>
                            </DropdownMenuGroup>

                            <DropdownMenuSeparator />

                            <DropdownMenuGroup aria-label="Platforms">
                                <DropdownMenuLabel>Platform</DropdownMenuLabel>

                                <DropdownMenuItem
                                    className="flex items-center justify-between"
                                    onClick={() => onPlatformChange(undefined)}
                                >
                                    <span>All</span>

                                    {!filters.platform && <CheckIcon className="size-4" />}
                                </DropdownMenuItem>

                                {AUDIT_EVENT_PLATFORMS.map((platform) => (
                                    <DropdownMenuItem
                                        className="flex items-center justify-between"
                                        key={platform.value}
                                        onClick={() => onPlatformChange(platform.value)}
                                    >
                                        <span>{platform.label}</span>

                                        {filters.platform === platform.value && <CheckIcon className="size-4" />}
                                    </DropdownMenuItem>
                                ))}
                            </DropdownMenuGroup>

                            <DropdownMenuSeparator />

                            <DropdownMenuGroup>
                                <DropdownMenuLabel>Date</DropdownMenuLabel>

                                <DropdownMenuItem
                                    className="flex items-center justify-between"
                                    onClick={onClearDateRange}
                                >
                                    <span>All time</span>

                                    {filters.dateRange === 'all' && <CheckIcon className="size-4" />}
                                </DropdownMenuItem>

                                {AUDIT_EVENT_DATE_RANGE_PRESETS.map((dateRangePreset) => (
                                    <DropdownMenuItem
                                        className="flex items-center justify-between"
                                        key={dateRangePreset.value}
                                        onClick={() => onDateRangePresetChange(dateRangePreset.value)}
                                    >
                                        <span>{dateRangePreset.label}</span>

                                        {filters.dateRange === dateRangePreset.value && (
                                            <CheckIcon className="size-4" />
                                        )}
                                    </DropdownMenuItem>
                                ))}

                                <DropdownMenuItem
                                    className="flex items-center justify-between"
                                    onClick={() => {
                                        customRangeRequestedRef.current = true;

                                        setCustomRangeOpen(true);
                                    }}
                                >
                                    <div className="flex items-center">
                                        <CalendarRangeIcon className="mr-2 size-3 text-content-neutral-secondary" />

                                        <span>Custom range</span>
                                    </div>

                                    {filters.dateRange === 'custom' && <CheckIcon className="size-4" />}
                                </DropdownMenuItem>
                            </DropdownMenuGroup>

                            <DropdownMenuSeparator />

                            <DropdownMenuItem
                                disabled={!hasActiveFilters}
                                icon={<XIcon />}
                                label="Clear all filters"
                                onClick={onResetFilters}
                            />
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            </PopoverAnchor>

            <PopoverContent align="end" className="w-72">
                <fieldset className="flex flex-col gap-3 border-0">
                    <legend className="mb-3 text-sm font-semibold">Custom range</legend>

                    <div className="flex flex-col gap-1">
                        <span className="text-sm text-muted-foreground">From</span>

                        <DatePicker onChange={onCustomFromDateChange} value={customFromDate} />
                    </div>

                    <div className="flex flex-col gap-1">
                        <span className="text-sm text-muted-foreground">To</span>

                        <DatePicker onChange={onCustomToDateChange} value={customToDate} />
                    </div>
                </fieldset>
            </PopoverContent>
        </Popover>
    );
};

export default AuditEventsFilters;
