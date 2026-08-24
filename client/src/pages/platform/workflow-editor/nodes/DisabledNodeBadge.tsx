import {BanIcon} from 'lucide-react';

const DisabledNodeBadge = () => (
    <span title="Disabled — skipped during execution">
        <BanIcon aria-hidden className="size-3.5 shrink-0 text-content-neutral-tertiary" />
    </span>
);

export default DisabledNodeBadge;
