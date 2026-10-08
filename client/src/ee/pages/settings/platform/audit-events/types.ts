import {AuditEventsQuery} from '@/shared/middleware/graphql';

export type AuditEventItemType = NonNullable<AuditEventsQuery['auditEvents']['content']>[number];

export interface AuditEventFiltersI {
    dataSearch?: string;
    eventType?: string;
    fromDate?: number;
    principal?: string;
    toDate?: number;
}
