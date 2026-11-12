import Button from '@/components/Button/Button';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import IntegrationInstanceConfigurationsPanel from '@/ee/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationsPanel';
import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {useGetIntegrationInstanceConfigurationsQuery} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {ENVIRONMENT_CONFIGS} from '@/shared/constants/environmentConfigs';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ExternalLinkIcon, RefreshCwIcon, Settings2Icon} from 'lucide-react';
import {useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

interface IntegrationConfigurationsSheetProps {
    integration: Integration;
    onSheetOpenChange: (open: boolean) => void;
    sheetOpen: boolean;
}

const IntegrationConfigurationsSheet = ({
    integration,
    onSheetOpenChange,
    sheetOpen,
}: IntegrationConfigurationsSheetProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const navigate = useNavigate();

    const {isFetching: configurationsIsFetching, refetch: refetchConfigurations} =
        useGetIntegrationInstanceConfigurationsQuery(
            {
                environmentId: DEVELOPMENT_ENVIRONMENT,
                integrationId: integration.id,
            },
            sheetOpen && !!integration.id
        );

    const handleOpenInConfigurationsClick = () => {
        onSheetOpenChange(false);

        void navigate(`/embedded/configurations?integrationId=${integration.id}`);
    };

    return (
        <Sheet onOpenChange={onSheetOpenChange} open={sheetOpen}>
            <SheetContent
                className="top-3 right-4 bottom-4 flex h-auto flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-workflow-sidebar-project-deployments-sheet-width"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <header
                    className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-b-sidebar-border bg-sidebar p-3"
                    data-environment={ENVIRONMENT_CONFIGS[currentEnvironmentId]?.sidebarTheme}
                >
                    <div className="flex items-center gap-x-2">
                        <Settings2Icon className="size-5" />

                        <SheetTitle className="text-lg font-semibold">Integration Configurations</SheetTitle>
                    </div>

                    <div className="flex items-center gap-x-1">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    aria-label="Refresh integration configurations"
                                    disabled={configurationsIsFetching}
                                    icon={
                                        <RefreshCwIcon
                                            className={twMerge(configurationsIsFetching && 'animate-spin')}
                                        />
                                    }
                                    onClick={() => void refetchConfigurations()}
                                    size="icon"
                                    variant="ghost"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Refresh</TooltipContent>
                        </Tooltip>

                        <SheetCloseButton />
                    </div>
                </header>

                <div className="flex min-h-0 flex-1 flex-col overflow-y-auto bg-surface-neutral-primary p-3">
                    <IntegrationInstanceConfigurationsPanel enabled={sheetOpen} integration={integration} />
                </div>

                <footer className="flex shrink-0 justify-end gap-x-2 rounded-b-md border-t border-t-border/50 bg-surface-neutral-primary p-3">
                    <Button label="Close" onClick={() => onSheetOpenChange(false)} variant="outline" />

                    <Button
                        icon={<ExternalLinkIcon />}
                        label="Open in Configurations"
                        onClick={handleOpenInConfigurationsClick}
                    />
                </footer>
            </SheetContent>
        </Sheet>
    );
};

export default IntegrationConfigurationsSheet;
