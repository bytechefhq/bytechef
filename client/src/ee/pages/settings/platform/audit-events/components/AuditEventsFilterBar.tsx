import DatePicker from '@/components/DatePicker/DatePicker';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {useAuditEventTypesQuery} from '@/shared/middleware/graphql';
import {XIcon} from 'lucide-react';
import {useEffect, useRef, useState} from 'react';

import {AuditEventFiltersI} from '../types';

interface AuditEventsFilterBarPropsI {
    fromDate?: number;
    onChange: (filters: AuditEventFiltersI) => void;
    toDate?: number;
}

const AuditEventsFilterBar = ({fromDate, onChange, toDate}: AuditEventsFilterBarPropsI) => {
    const [dataSearch, setDataSearch] = useState('');
    const [eventType, setEventType] = useState<string | undefined>();
    const [principal, setPrincipal] = useState('');

    const lastTypedFiltersKeyRef = useRef(JSON.stringify({}));

    const {data: eventTypesData, isError: isEventTypesError} = useAuditEventTypesQuery();

    const hasFilters = !!(principal || dataSearch || eventType || fromDate || toDate);

    const buildFilters = (overrides: Partial<AuditEventFiltersI> = {}): AuditEventFiltersI => ({
        dataSearch: dataSearch.trim() || undefined,
        eventType,
        fromDate,
        principal: principal.trim() || undefined,
        toDate,
        ...overrides,
    });

    // Day boundaries are taken in the viewer's local time zone: that is the day the date picker shows, and epoch
    // millis carry the instant to the server unambiguously.
    const handleFromDateChange = (date?: Date) => {
        const normalized = date
            ? new Date(date.getFullYear(), date.getMonth(), date.getDate(), 0, 0, 0, 0).getTime()
            : undefined;

        onChange(buildFilters({fromDate: normalized}));
    };

    const handleToDateChange = (date?: Date) => {
        const normalized = date
            ? new Date(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59, 999).getTime()
            : undefined;

        onChange(buildFilters({toDate: normalized}));
    };

    useEffect(() => {
        const typedFilters = {
            dataSearch: dataSearch.trim() || undefined,
            eventType,
            principal: principal.trim() || undefined,
        };
        const typedFiltersKey = JSON.stringify(typedFilters);

        if (typedFiltersKey === lastTypedFiltersKeyRef.current) {
            return;
        }

        const timeoutId = window.setTimeout(() => {
            lastTypedFiltersKeyRef.current = typedFiltersKey;

            onChange({...typedFilters, fromDate, toDate});
        }, 300);

        return () => window.clearTimeout(timeoutId);
    }, [dataSearch, eventType, fromDate, onChange, principal, toDate]);

    return (
        <div className="grid grid-cols-1 items-end gap-4 md:grid-cols-3 xl:grid-cols-6">
            <div className="flex flex-col space-y-2">
                <Label>Principal</Label>

                <Input
                    onChange={(event) => setPrincipal(event.target.value)}
                    placeholder="e.g. admin@localhost.com"
                    value={principal}
                />
            </div>

            <div className="flex flex-col space-y-2">
                <Label>Search data</Label>

                <Input
                    onChange={(event) => setDataSearch(event.target.value)}
                    placeholder="match any data value"
                    value={dataSearch}
                />
            </div>

            <div className="flex flex-col space-y-2">
                <Label>Event type</Label>

                <Select
                    onValueChange={(value) => setEventType(value === '__all__' ? undefined : value)}
                    value={eventType || '__all__'}
                >
                    <SelectTrigger>
                        <SelectValue />
                    </SelectTrigger>

                    <SelectContent>
                        <SelectItem value="__all__">All</SelectItem>

                        {(eventTypesData?.auditEventTypes || []).map((type) => (
                            <SelectItem key={type} value={type}>
                                {type}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>

                {isEventTypesError && <p className="text-xs text-destructive">Event types could not be loaded.</p>}
            </div>

            <div className="flex flex-col space-y-2">
                <Label>From date</Label>

                <DatePicker onChange={handleFromDateChange} value={fromDate ? new Date(fromDate) : undefined} />
            </div>

            <div className="flex flex-col space-y-2">
                <Label>To date</Label>

                <DatePicker onChange={handleToDateChange} value={toDate ? new Date(toDate) : undefined} />
            </div>

            {hasFilters && (
                <button
                    className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
                    onClick={() => {
                        setDataSearch('');
                        setEventType(undefined);
                        setPrincipal('');

                        lastTypedFiltersKeyRef.current = JSON.stringify({});

                        onChange({});
                    }}
                    type="button"
                >
                    <XIcon className="size-4" /> Clear filters
                </button>
            )}
        </div>
    );
};

export default AuditEventsFilterBar;
