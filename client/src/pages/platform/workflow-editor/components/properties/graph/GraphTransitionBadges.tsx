import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipPortal, TooltipTrigger} from '@/components/ui/tooltip';
import {ArrowRightIcon, ShuffleIcon, TriangleAlertIcon} from 'lucide-react';

interface GraphTransitionBadgesPropsI {
    dangling: Array<string>;
    dynamic: boolean;
    targets: Array<string>;
}

/**
 * Pure display of a graph node's `next` expression, pre-classified by `extractNextTargets`:
 * one badge per statically-resolvable target, a "dynamic" marker when the expression is more
 * than a bare literal, and a warning badge per dangling literal (a target name that matches no
 * currently declared node — see the graph node rename decision: renaming a node does NOT
 * rewrite other nodes' `next` expressions, so a stale reference surfaces here instead).
 *
 * Renders nothing for a terminal node (no `next` expression at all).
 */
export default function GraphTransitionBadges({dangling, dynamic, targets}: GraphTransitionBadgesPropsI) {
    if (!dangling.length && !dynamic && !targets.length) {
        return null;
    }

    return (
        <div aria-label="Transition targets" className="flex flex-wrap items-center gap-1">
            {/* Reads as "this lane -> that lane" now that the badges sit inline after the lane's
                own name rather than on a row of their own. */}

            <ArrowRightIcon aria-hidden className="size-3 shrink-0 text-content-neutral-secondary" />

            {targets.map((target) => (
                <Badge key={`target_${target}`} label={target} styleType="secondary-outline" />
            ))}

            {dynamic && (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <span>
                            <Badge icon={<ShuffleIcon />} label="dynamic" styleType="outline-outline" />
                        </span>
                    </TooltipTrigger>

                    <TooltipPortal>
                        <TooltipContent>
                            The target of this transition is computed at runtime and cannot be resolved statically.
                        </TooltipContent>
                    </TooltipPortal>
                </Tooltip>
            )}

            {dangling.map((danglingTarget) => (
                <Tooltip key={`dangling_${danglingTarget}`}>
                    <TooltipTrigger asChild>
                        <span>
                            <Badge icon={<TriangleAlertIcon />} label={danglingTarget} styleType="warning-outline" />
                        </span>
                    </TooltipTrigger>

                    <TooltipPortal>
                        <TooltipContent>
                            {`No node named "${danglingTarget}" exists — this transition target is dangling.`}
                        </TooltipContent>
                    </TooltipPortal>
                </Tooltip>
            ))}
        </div>
    );
}
