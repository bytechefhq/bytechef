import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';

interface ConnectedUserOriginBadgeProps {
    provisioned: boolean;
    resourceName: string;
}

const ConnectedUserOriginBadge = ({provisioned, resourceName}: ConnectedUserOriginBadgeProps) => (
    <Tooltip>
        <TooltipTrigger asChild>
            <Badge
                className="uppercase"
                label={provisioned ? 'Provisioned' : 'Self-created'}
                styleType="secondary-outline"
                weight="semibold"
            />
        </TooltipTrigger>

        <TooltipContent>
            {provisioned
                ? `An admin provisioned this ${resourceName} for the connected user.`
                : `The connected user added this ${resourceName}.`}
        </TooltipContent>
    </Tooltip>
);

export default ConnectedUserOriginBadge;
