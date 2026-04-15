import Badge from '@/components/Badge/Badge';

interface AuditEventsFilterTitlePropsI {
    filterData: {
        dataSearch?: string;
        eventType?: string;
        fromDate?: number;
        principal?: string;
        toDate?: number;
    };
}

const formatDate = (value?: number) => (value ? new Date(value).toLocaleDateString() : undefined);

const AuditEventsFilterTitle = ({filterData}: AuditEventsFilterTitlePropsI) => {
    const {dataSearch, eventType, fromDate, principal, toDate} = filterData;

    const hasFilters = !!(dataSearch || eventType || fromDate || principal || toDate);

    return (
        <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
            <span className="text-sm font-semibold text-muted-foreground uppercase">Filter by</span>

            {!hasFilters && <span className="text-sm text-muted-foreground uppercase">none</span>}

            {principal && (
                <>
                    <span className="text-sm text-muted-foreground uppercase">principal:</span>

                    <Badge label={principal} styleType="secondary-filled" weight="semibold" />
                </>
            )}

            {eventType && (
                <>
                    <span className="text-sm text-muted-foreground uppercase">type:</span>

                    <Badge label={eventType} styleType="secondary-filled" weight="semibold" />
                </>
            )}

            {dataSearch && (
                <>
                    <span className="text-sm text-muted-foreground uppercase">data:</span>

                    <Badge label={dataSearch} styleType="secondary-filled" weight="semibold" />
                </>
            )}

            {(fromDate || toDate) && (
                <>
                    <span className="text-sm text-muted-foreground uppercase">date:</span>

                    <Badge
                        label={`${formatDate(fromDate) || '…'} → ${formatDate(toDate) || '…'}`}
                        styleType="secondary-filled"
                        weight="semibold"
                    />
                </>
            )}
        </div>
    );
};

export default AuditEventsFilterTitle;
