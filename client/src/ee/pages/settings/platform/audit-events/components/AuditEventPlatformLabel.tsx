import {PlatformType} from '@/shared/middleware/graphql';

import {AUDIT_EVENT_PLATFORMS} from '../constants';

interface AuditEventPlatformLabelPropsI {
    platform?: PlatformType | null;
}

const AuditEventPlatformLabel = ({platform}: AuditEventPlatformLabelPropsI) => {
    const auditEventPlatform = AUDIT_EVENT_PLATFORMS.find((platformItem) => platformItem.value === platform);

    if (!auditEventPlatform) {
        return <span className="text-muted-foreground">—</span>;
    }

    return <span>{auditEventPlatform.label}</span>;
};

export default AuditEventPlatformLabel;
