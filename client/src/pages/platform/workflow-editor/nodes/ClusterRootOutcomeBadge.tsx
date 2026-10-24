import {ShieldAlertIcon, ShieldBanIcon} from 'lucide-react';

import {type ClusterRootOutcomeType} from '../utils/getClusterRootOutcome';

interface ClusterRootOutcomeBadgeProps {
    outcome: ClusterRootOutcomeType;
}

/**
 * Names the guardrail outcome on a cluster root card, in the box's top-left corner. Positioned absolutely, so the box
 * it sits in must be `relative`.
 */
const ClusterRootOutcomeBadge = ({outcome}: ClusterRootOutcomeBadgeProps) => (
    <span
        className="absolute -top-3 left-3 z-10 flex items-center gap-1 rounded-full border border-amber-500 bg-surface-neutral-primary px-2 text-xs leading-5 text-amber-500"
        data-testid="cluster-root-outcome-badge"
    >
        {outcome === 'BLOCKED' ? <ShieldBanIcon className="size-3" /> : <ShieldAlertIcon className="size-3" />}

        {outcome === 'BLOCKED' ? 'Blocked' : 'Withheld'}
    </span>
);

export default ClusterRootOutcomeBadge;
