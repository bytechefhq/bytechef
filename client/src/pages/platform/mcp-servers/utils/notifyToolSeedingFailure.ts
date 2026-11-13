import {toast} from 'sonner';

interface NotifyToolSeedingFailureParamsI {
    failedCount: number;
    failedNames?: string[];
    toolLabel: string;
}

export default function notifyToolSeedingFailure({
    failedCount,
    failedNames,
    toolLabel,
}: NotifyToolSeedingFailureParamsI): void {
    if (failedCount === 0) {
        return;
    }

    const subject = failedNames?.length
        ? failedNames.join(', ')
        : `${failedCount} ${toolLabel}${failedCount === 1 ? '' : 's'}`;

    toast.warning(`Couldn't load the settings of ${subject}`, {
        description: `${failedCount === 1 ? 'It is' : 'They are'} added without AI values. Open the ${toolLabel} settings to set them.`,
    });
}
