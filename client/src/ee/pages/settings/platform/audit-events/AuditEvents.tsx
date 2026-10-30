import {Button} from '@/components/ui/button';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useAuditEventsQuery} from '@/shared/middleware/graphql';
import {useState} from 'react';

import AuditEventDetailSheet from './components/AuditEventDetailSheet';
import AuditEventsActiveFilterBadges from './components/AuditEventsActiveFilterBadges';
import AuditEventsFilters from './components/AuditEventsFilters';
import AuditEventsSearch from './components/AuditEventsSearch';
import AuditEventsTable from './components/AuditEventsTable';
import {useAuditEventFilters} from './hooks/useAuditEventFilters';
import {AuditEventItemType} from './types';

const PAGE_SIZE = 25;

const AuditEvents = () => {
    const [selected, setSelected] = useState<AuditEventItemType | undefined>();
    const [sheetOpen, setSheetOpen] = useState(false);

    const {
        filters,
        handleClearDateRange,
        handleClearEventType,
        handleClearFromDate,
        handleClearToDate,
        handleCustomFromDateChange,
        handleCustomToDateChange,
        handleDateRangePresetChange,
        handleEventTypeChange,
        handleResetFilters,
        hasActiveFilters,
        page,
        search,
        searchInput,
        setPage,
        setSearchInput,
    } = useAuditEventFilters();

    const {data, isError, isLoading, refetch} = useAuditEventsQuery({
        eventType: filters.eventType,
        fromDate: filters.fromDate,
        page,
        search,
        size: PAGE_SIZE,
        toDate: filters.toDate,
    });

    const content = data?.auditEvents.content || [];
    const totalPages = data?.auditEvents.totalPages || 0;

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle
                    position="main"
                    right={
                        <div className="flex items-center gap-2">
                            <AuditEventsSearch onSearchChange={setSearchInput} search={searchInput} />

                            <AuditEventsFilters
                                filters={filters}
                                hasActiveFilters={hasActiveFilters}
                                onClearDateRange={handleClearDateRange}
                                onCustomFromDateChange={handleCustomFromDateChange}
                                onCustomToDateChange={handleCustomToDateChange}
                                onDateRangePresetChange={handleDateRangePresetChange}
                                onEventTypeChange={handleEventTypeChange}
                                onResetFilters={handleResetFilters}
                            />
                        </div>
                    }
                    title="Audit Events"
                />
            }
            leftSidebarOpen={false}
        >
            <div className="flex w-full flex-1 flex-col gap-4 p-6">
                <AuditEventsActiveFilterBadges
                    filters={filters}
                    hasActiveFilters={hasActiveFilters}
                    onClearDateRange={handleClearDateRange}
                    onClearEventType={handleClearEventType}
                    onClearFromDate={handleClearFromDate}
                    onClearToDate={handleClearToDate}
                    onResetFilters={handleResetFilters}
                />

                {isLoading && <p className="p-8 text-center text-sm text-muted-foreground">Loading…</p>}

                {isError && (
                    <div className="flex flex-col items-center gap-2 p-8">
                        <p className="text-center text-sm text-destructive">Audit events could not be loaded.</p>

                        <Button onClick={() => refetch()} variant="outline">
                            Retry
                        </Button>
                    </div>
                )}

                {!isLoading && !isError && (
                    <AuditEventsTable
                        auditEvents={content}
                        onRowClick={(auditEvent) => {
                            setSelected(auditEvent);
                            setSheetOpen(true);
                        }}
                    />
                )}

                {!isError && totalPages > 1 && (
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
