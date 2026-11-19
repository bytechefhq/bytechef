import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';

const ConnectedUserVersionBadge = ({
    newerVersionAvailable = false,
    styleType = 'secondary-filled',
    tooltip,
    version,
}: {
    newerVersionAvailable?: boolean;
    styleType?: 'outline-outline' | 'secondary-filled';
    tooltip: string;
    version?: number | null;
}) => (
    <Tooltip>
        <TooltipTrigger asChild>
            <span className="relative inline-flex shrink-0">
                <Badge label={`V${version}`} styleType={styleType} weight="semibold" />

                {newerVersionAvailable && (
                    <span
                        aria-label="Newer version available"
                        className="absolute -top-0.5 -right-0.5 size-2 rounded-full bg-surface-warning-primary ring-2 ring-background"
                        role="img"
                    />
                )}
            </span>
        </TooltipTrigger>

        <TooltipContent>
            {newerVersionAvailable ? (
                <span className="flex flex-col">
                    <span>{tooltip}</span>

                    <span>Newer version available</span>
                </span>
            ) : (
                tooltip
            )}
        </TooltipContent>
    </Tooltip>
);

export default ConnectedUserVersionBadge;
