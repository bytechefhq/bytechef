import {BanIcon} from 'lucide-react';

interface DisabledNodeBadgeProps {
    trigger?: boolean;
}

const DisabledNodeBadge = ({trigger = false}: DisabledNodeBadgeProps) => (
    <span title={trigger ? 'Disabled: does not start runs' : 'Disabled: skipped during execution'}>
        <BanIcon aria-hidden className="size-3.5 shrink-0 text-content-neutral-tertiary" />
    </span>
);

export default DisabledNodeBadge;
