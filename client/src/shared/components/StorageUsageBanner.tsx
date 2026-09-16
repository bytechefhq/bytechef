import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {TriangleAlertIcon} from 'lucide-react';

interface StorageUsageBannerProps {
    label: string;
    limitBytes: number;
    percentage: number;
    unlimited: boolean;
    usedBytes: number;
}

const WARNING_THRESHOLD = 80;

const formatBytes = (bytes: number) => {
    if (bytes < 1_024) {
        return `${bytes} B`;
    }

    if (bytes < 1_048_576) {
        return `${Math.round(bytes / 1_024)} KB`;
    }

    if (bytes < 1_073_741_824) {
        return `${Math.round(bytes / 1_048_576)} MB`;
    }

    return `${(bytes / 1_073_741_824).toFixed(1)} GB`;
};

const StorageUsageBanner = ({label, limitBytes, percentage, unlimited, usedBytes}: StorageUsageBannerProps) => {
    if (unlimited || percentage < WARNING_THRESHOLD) {
        return null;
    }

    return (
        <Alert className="m-4 mb-0 w-auto" variant="warning">
            <TriangleAlertIcon />

            <AlertTitle>{`${label} storage is at ${Math.round(percentage)}%`}</AlertTitle>

            <AlertDescription>
                <span>
                    {`Using ${formatBytes(usedBytes)} of ${formatBytes(limitBytes)}. New items are blocked once the limit is reached — delete items or increase the limit.`}
                </span>
            </AlertDescription>
        </Alert>
    );
};

export default StorageUsageBanner;
