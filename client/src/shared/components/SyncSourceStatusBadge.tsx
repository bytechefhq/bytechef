import {twMerge} from 'tailwind-merge';

interface SyncSourceStatusBadgePropsI {
    status: string;
}

const STATUS_STYLES: Record<string, {className: string; label: string}> = {
    BUILDING_PREVIEW: {className: 'bg-yellow-100 text-yellow-800', label: 'Building Preview'},
    DISABLED: {className: 'bg-surface-neutral-secondary text-content-neutral-primary', label: 'Disabled'},
    FAILED: {className: 'bg-surface-destructive-secondary text-content-destructive', label: 'Failed'},
    PREVIEW: {className: 'bg-surface-brand-secondary text-content-brand-primary', label: 'Preview'},
    READY: {className: 'bg-surface-success-secondary text-content-success-primary', label: 'Ready'},
};

const SyncSourceStatusBadge = ({status}: SyncSourceStatusBadgePropsI) => {
    const style = STATUS_STYLES[status] || {
        className: 'bg-surface-neutral-secondary text-content-neutral-primary',
        label: status,
    };

    return (
        <span
            className={twMerge(
                'inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium',
                style.className
            )}
        >
            {style.label}
        </span>
    );
};

export default SyncSourceStatusBadge;
