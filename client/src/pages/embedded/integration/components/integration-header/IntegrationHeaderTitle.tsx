import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useIntegrationsLeftSidebarStore from '@/pages/embedded/integration/stores/useIntegrationsLeftSidebarStore';
import {Integration} from '@/shared/middleware/embedded/configuration';
import {PanelLeftIcon} from 'lucide-react';
import * as React from 'react';

const IntegrationHeaderTitle = ({integration}: {integration: Integration}) => {
    const {leftSidebarOpen, setLeftSidebarOpen} = useIntegrationsLeftSidebarStore();

    return (
        <div className="flex items-center space-x-2">
            {!leftSidebarOpen && (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            className="hover:bg-background/70"
                            onClick={() => setLeftSidebarOpen(!leftSidebarOpen)}
                            size="icon"
                            variant="ghost"
                        >
                            <PanelLeftIcon className="size-5" />
                        </Button>
                    </TooltipTrigger>

                    <TooltipContent>See integrations</TooltipContent>
                </Tooltip>
            )}

            <h1>{integration?.componentName}</h1>

            {integration && (
                <Badge className="flex space-x-1" variant="secondary">
                    <span>V{integration.lastIntegrationVersion}</span>

                    <span>{integration.lastStatus}</span>
                </Badge>
            )}
        </div>
    );
};

export default IntegrationHeaderTitle;
