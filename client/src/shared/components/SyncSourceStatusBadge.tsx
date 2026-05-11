import {twMerge} from 'tailwind-merge';

interface SyncSourceStatusBadgePropsI {
    status: string;
}

const STATUS_STYLES: Record<string, {className: string; label: string}> = {
    BUILDING_PREVIEW: {className: 'bg-yellow-100 text-yellow-800', label: 'Building Preview'},
    DISABLED: {className: 'bg-gray-100 text-gray-800', label: 'Disabled'},
    FAILED: {className: 'bg-red-100 text-red-800', label: 'Failed'},
    PREVIEW: {className: 'bg-blue-100 text-blue-800', label: 'Preview'},
    READY: {className: 'bg-green-100 text-green-800', label: 'Ready'},
};

const SyncSourceStatusBadge = ({status}: SyncSourceStatusBadgePropsI) => {
    const style = STATUS_STYLES[status] || {className: 'bg-gray-100 text-gray-800', label: status};

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
