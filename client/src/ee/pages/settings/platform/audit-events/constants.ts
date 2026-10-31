import {PlatformType} from '@/shared/middleware/graphql';

import {AuditEventDateRangePresetI, AuditEventPlatformI} from './types';

const ONE_DAY_MILLISECONDS = 24 * 60 * 60 * 1000;

export const AUDIT_EVENT_DATE_RANGE_PRESETS: AuditEventDateRangePresetI[] = [
    {durationMilliseconds: ONE_DAY_MILLISECONDS, label: 'Last 24 hours', value: 'last24Hours'},
    {durationMilliseconds: 7 * ONE_DAY_MILLISECONDS, label: 'Last 7 days', value: 'last7Days'},
    {durationMilliseconds: 30 * ONE_DAY_MILLISECONDS, label: 'Last 30 days', value: 'last30Days'},
];

export const AUDIT_EVENT_PLATFORMS: AuditEventPlatformI[] = [
    {label: 'Automation', value: PlatformType.Automation},
    {label: 'Embedded', value: PlatformType.Embedded},
];
