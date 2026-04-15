import DatePicker from '@/components/DatePicker/DatePicker';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {useAuditEventTypesQuery} from '@/shared/middleware/graphql';
import {XIcon} from 'lucide-react';
import {useEffect, useState} from 'react';

interface AuditEventsFilterBarPropsI {
    dataSearch?: string;
    fromDate?: number;
    onChange: (filters: {
        dataSearch?: string;
        eventType?: string;
        fromDate?: number;
        principal?: string;
        toDate?: number;
    }) => void;
    toDate?: number;
}

const AuditEventsFilterBar = ({fromDate, onChange, toDate}: AuditEventsFilterBarPropsI) => {
    const [dataSearch, setDataSearch] = useState('');
    const [eventType, setEventType] = useState<string | undefined>();
    const [principal, setPrincipal] = useState('');

    const {data: eventTypesData} = useAuditEventTypesQuery();

    useEffect(() => {
        const handle = window.setTimeout(() => {
            onChange({
                dataSearch: dataSearch.trim() || undefined,
                eventType,
                fromDate,
                principal: principal.trim() || undefined,
                toDate,
            });
        }, 300);

        return () => window.clearTimeout(handle);
    }, [dataSearch, eventType, fromDate, onChange, principal, toDate]);

    const hasFilters = !!(principal || dataSearch || eventType || fromDate || toDate);

    const handleFromDateChange = (date?: Date) => {
        const normalized = date
            ? new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate(), 0, 0, 0, 0)).getTime()
            : undefined;

        onChange({
            dataSearch: dataSearch.trim() || undefined,
            eventType,
            fromDate: normalized,
            principal: principal.trim() || undefined,
            toDate,
        });
    };

    const handleToDateChange = (date?: Date) => {
        const normalized = date
            ? new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59, 999)).getTime()
            : undefined;

        onChange({
            dataSearch: dataSearch.trim() || undefined,
            eventType,
            fromDate,
            principal: principal.trim() || undefined,
            toDate: normalized,
        });
    };

    return (
        <div className="space-y-4 px-4">
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
                    value={eventType ?? '__all__'}
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
