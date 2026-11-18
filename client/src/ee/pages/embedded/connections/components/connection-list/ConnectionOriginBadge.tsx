import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {CONNECTION_ORIGIN_BADGES} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connectionOriginBadges';
import {ConnectionOriginEnum} from '@/ee/shared/middleware/embedded/configuration';

const ConnectionOriginBadge = ({origin}: {origin?: ConnectionOriginEnum}) => {
    if (!origin) {
        return null;
    }

    const originBadge = CONNECTION_ORIGIN_BADGES[origin];

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Badge
                    className="uppercase"
                    label={originBadge.label}
                    styleType="secondary-outline"
                    weight="semibold"
                />
            </TooltipTrigger>

            <TooltipContent>{originBadge.description}</TooltipContent>
        </Tooltip>
    );
};

export default ConnectionOriginBadge;
