import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useIntegrationsLeftSidebarStore from '@/ee/pages/embedded/integration/stores/useIntegrationsLeftSidebarStore';
import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {PanelLeftIcon} from 'lucide-react';
import * as React from 'react';
import {useShallow} from 'zustand/react/shallow';

const IntegrationHeaderTitle = ({integration}: {integration: Integration}) => {
    const {leftSidebarOpen, setLeftSidebarOpen} = useIntegrationsLeftSidebarStore(
        useShallow((state) => ({
            leftSidebarOpen: state.leftSidebarOpen,
            setLeftSidebarOpen: state.setLeftSidebarOpen,
        }))
    );

    return (
        <div className="flex items-center space-x-2">
            {!leftSidebarOpen && (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            className="hover:bg-background/70 [&_svg]:size-5"
                            icon={<PanelLeftIcon />}
                            onClick={() => setLeftSidebarOpen(!leftSidebarOpen)}
                            size="icon"
                            variant="ghost"
                        />
                    </TooltipTrigger>

                    <TooltipContent>See integrations</TooltipContent>
                </Tooltip>
            )}

            <h1>{integration?.name}</h1>

            {integration && (
                <Badge className="flex space-x-1" styleType="secondary-filled" weight="semibold">
                    <span>V{integration.lastIntegrationVersion}</span>

                    <span>{integration.lastStatus}</span>
                </Badge>
            )}
        </div>
    );
};

export default IntegrationHeaderTitle;
