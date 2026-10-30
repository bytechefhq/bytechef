import {AuditEventsQuery} from '@/shared/middleware/graphql';

export type AuditEventItemType = NonNullable<AuditEventsQuery['auditEvents']['content']>[number];

export type AuditEventDateRangePresetType = 'last24Hours' | 'last30Days' | 'last7Days';

export type AuditEventDateRangeType = 'all' | 'custom' | AuditEventDateRangePresetType;

export interface AuditEventDateRangePresetI {
    durationMilliseconds: number;
    label: string;
    value: AuditEventDateRangePresetType;
}

export interface AuditEventFiltersI {
    dateRange: AuditEventDateRangeType;
    eventType?: string;
    fromDate?: number;
    toDate?: number;
}
