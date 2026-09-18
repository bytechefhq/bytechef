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

const formatMegabytes = (bytes: number) => `${Math.round(bytes / 1_048_576)} MB`;

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
                    {`Using ${formatMegabytes(usedBytes)} of ${formatMegabytes(limitBytes)}. New items are blocked once the limit is reached — delete items or increase the limit.`}
                </span>
            </AlertDescription>
        </Alert>
    );
};

export default StorageUsageBanner;
