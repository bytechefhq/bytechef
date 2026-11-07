import {Badge} from '@/components/ui/badge';
import {type RiskLevel} from '@/shared/middleware/graphql';
import {twMerge} from 'tailwind-merge';

const RISK_LEVEL_BADGE_CLASS_NAMES: Record<RiskLevel, string> = {
    CRITICAL: 'border-transparent bg-red-100 font-semibold text-red-900 dark:bg-red-950 dark:text-red-200',
    HIGH: 'border-transparent bg-orange-100 font-semibold text-orange-900 dark:bg-orange-950 dark:text-orange-200',
    LOW: 'border-transparent bg-muted text-muted-foreground',
    MEDIUM: 'border-transparent bg-amber-100 text-amber-900 dark:bg-amber-950 dark:text-amber-200',
};

const RISK_LEVEL_LABELS: Record<RiskLevel, string> = {
    CRITICAL: 'Critical',
    HIGH: 'High',
    LOW: 'Low',
    MEDIUM: 'Medium',
};

interface RiskLevelBadgeProps {
    className?: string;
    riskLevel: RiskLevel;
}

const RiskLevelBadge = ({className, riskLevel}: RiskLevelBadgeProps) => (
    <Badge className={twMerge(RISK_LEVEL_BADGE_CLASS_NAMES[riskLevel], className)} variant="outline">
        {RISK_LEVEL_LABELS[riskLevel]}
    </Badge>
);

export default RiskLevelBadge;
