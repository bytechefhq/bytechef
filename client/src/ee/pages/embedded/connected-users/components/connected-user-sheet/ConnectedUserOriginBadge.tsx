import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';

interface ConnectedUserOriginBadgeProps {
    provisioned: boolean;
    provisionedBy?: string;
    resourceName: string;
}

const getTooltip = ({provisioned, provisionedBy, resourceName}: ConnectedUserOriginBadgeProps) => {
    if (!provisioned) {
        return `The connected user added this ${resourceName}.`;
    }

    if (provisionedBy) {
        return `${provisionedBy} provisioned this ${resourceName} for the connected user.`;
    }

    return `An admin provisioned this ${resourceName} for the connected user.`;
};

const ConnectedUserOriginBadge = ({provisioned, provisionedBy, resourceName}: ConnectedUserOriginBadgeProps) => (
    <Tooltip>
        <TooltipTrigger asChild>
            <Badge
                className="shrink-0 uppercase"
                label={provisioned ? 'Provisioned' : 'Self-created'}
                styleType="secondary-outline"
                weight="semibold"
            />
        </TooltipTrigger>

        <TooltipContent>{getTooltip({provisioned, provisionedBy, resourceName})}</TooltipContent>
    </Tooltip>
);

export default ConnectedUserOriginBadge;
