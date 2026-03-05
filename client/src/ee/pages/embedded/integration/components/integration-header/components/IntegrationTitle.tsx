import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Integration, IntegrationStatus} from '@/ee/shared/middleware/embedded/configuration';

const IntegrationTitle = ({integration}: {integration: Integration}) => {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <div className="flex max-w-96 items-center space-x-2">
                    <h1 className="truncate">{integration?.name}</h1>

                    {integration.name && integration.name.length > 43 && (
                        <TooltipContent>{integration.name}</TooltipContent>
                    )}

                    {integration && (
                        <Badge
                            className="flex space-x-1 bg-surface-neutral-primary"
                            styleType={
                                integration.lastStatus === IntegrationStatus.Published
                                    ? 'success-outline'
                                    : 'outline-outline'
                            }
                            weight="semibold"
                        >
                            <span>V{integration.lastIntegrationVersion}</span>

                            <span>{integration.lastStatus}</span>
                        </Badge>
                    )}
                </div>
            </TooltipTrigger>
        </Tooltip>
    );
};

export default IntegrationTitle;
