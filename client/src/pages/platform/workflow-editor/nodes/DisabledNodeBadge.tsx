import {BanIcon} from 'lucide-react';

/**
 * Shared "this node is disabled" indicator for `WorkflowNode` and `ReadOnlyNode`. Only ever
 * rendered when the node carries its OWN `disabled: true` flag -- never for a node that is
 * merely muted because it sits under a disabled ancestor.
 */
const DisabledNodeBadge = () => (
    <span title="Disabled — skipped during execution">
        <BanIcon aria-hidden className="size-3.5 shrink-0 text-content-neutral-tertiary" />
    </span>
);

export default DisabledNodeBadge;
