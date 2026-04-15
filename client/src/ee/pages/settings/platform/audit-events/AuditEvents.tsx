import {Button} from '@/components/ui/button';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {AuditEventsQuery, useAuditEventsQuery} from '@/shared/middleware/graphql';
import {useCallback, useState} from 'react';

import AuditEventDetailSheet from './components/AuditEventDetailSheet';
import AuditEventsFilterBar from './components/AuditEventsFilterBar';
import AuditEventsFilterTitle from './components/AuditEventsFilterTitle';
import AuditEventsTable from './components/AuditEventsTable';

type AuditEventItemType = NonNullable<AuditEventsQuery['auditEvents']['content']>[number];

interface FiltersI {
    dataSearch?: string;
    eventType?: string;
    fromDate?: number;
    principal?: string;
    toDate?: number;
}

const PAGE_SIZE = 25;

const AuditEvents = () => {
    const [filters, setFilters] = useState<FiltersI>({});
    const [page, setPage] = useState(0);
    const [selected, setSelected] = useState<AuditEventItemType | undefined>();
    const [sheetOpen, setSheetOpen] = useState(false);

    const {data, isLoading} = useAuditEventsQuery({
        dataSearch: filters.dataSearch,
        eventType: filters.eventType,
        fromDate: filters.fromDate,
        page,
        principal: filters.principal,
        size: PAGE_SIZE,
        toDate: filters.toDate,
    });

    const handleFilterChange = useCallback((next: FiltersI) => {
        setFilters(next);
        setPage(0);
    }, []);

    const content = data?.auditEvents.content || [];
    const totalPages = data?.auditEvents.totalPages || 0;

    return (
        <LayoutContainer
            header={<Header centerTitle position="main" title={<AuditEventsFilterTitle filterData={filters} />} />}
            leftSidebarBody={
                <AuditEventsFilterBar
                    dataSearch={filters.dataSearch}
                    fromDate={filters.fromDate}
                    onChange={handleFilterChange}
                    toDate={filters.toDate}
                />
            }
            leftSidebarHeader={<Header position="sidebar" title="Audit Events" titleClassName="font-normal" />}
            leftSidebarWidth="64"
        >
            <div className="flex w-full flex-1 flex-col gap-4 p-6">
                {isLoading ? (
                    <p className="p-8 text-center text-sm text-muted-foreground">Loading…</p>
                ) : (
                    <AuditEventsTable
                        auditEvents={content}
                        onRowClick={(auditEvent) => {
                            setSelected(auditEvent);
                            setSheetOpen(true);
                        }}
                    />
                )}

                {totalPages > 1 && (
                    <div className="flex items-center justify-end gap-2">
                        <Button
                            disabled={page === 0}
                            onClick={() => setPage((current) => current - 1)}
                            variant="outline"
                        >
                            Previous
                        </Button>

                        <span className="text-sm text-muted-foreground">
                            Page {page + 1} of {totalPages}
                        </span>

                        <Button
                            disabled={page + 1 >= totalPages}
                            onClick={() => setPage((current) => current + 1)}
                            variant="outline"
                        >
                            Next
                        </Button>
                    </div>
                )}

                <AuditEventDetailSheet auditEvent={selected} onOpenChange={setSheetOpen} open={sheetOpen} />
            </div>
        </LayoutContainer>
    );
};

export default AuditEvents;
