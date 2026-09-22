import Badge from '@/components/Badge/Badge';
import {AuditEventOutcome} from '@/shared/middleware/graphql';

interface AuditEventOutcomeBadgePropsI {
    outcome?: AuditEventOutcome | null;
}

const OUTCOME_STYLE_TYPES = {
    [AuditEventOutcome.Allowed]: 'success-outline',
    [AuditEventOutcome.Denied]: 'warning-outline',
    [AuditEventOutcome.Error]: 'destructive-outline',
    [AuditEventOutcome.RolledBack]: 'warning-outline',
    [AuditEventOutcome.Success]: 'success-outline',
} as const;

const AuditEventOutcomeBadge = ({outcome}: AuditEventOutcomeBadgePropsI) => {
    if (!outcome) {
        return <span className="text-muted-foreground">—</span>;
    }

    return <Badge label={outcome} styleType={OUTCOME_STYLE_TYPES[outcome]} weight="semibold" />;
};

export default AuditEventOutcomeBadge;
