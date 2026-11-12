import Button from '@/components/Button/Button';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AutomationProjectConfigurationDialog from '@/ee/pages/embedded/automations/components/automation-project-configuration-dialog/AutomationProjectConfigurationDialog';
import AutomationProjectConfigurationList from '@/ee/pages/embedded/automations/components/automation-project-configuration-list/AutomationProjectConfigurationList';
import AutomationProjectPublishDialog from '@/ee/pages/embedded/automations/components/automation-project-publish-dialog/AutomationProjectPublishDialog';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {ENVIRONMENT_CONFIGS} from '@/shared/constants/environmentConfigs';
import {
    AutomationProjectConfigurationsQuery,
    AutomationProjectsQuery,
    useAutomationProjectConfigurationsQuery,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {RefreshCwIcon, Settings2Icon} from 'lucide-react';
import {useState} from 'react';
import {twMerge} from 'tailwind-merge';

type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];
type AutomationProjectConfigurationType =
    AutomationProjectConfigurationsQuery['automationProjectConfigurations'][number];

interface ConfigurationDialogStateI {
    configuration?: AutomationProjectConfigurationType;
    updateProjectVersion?: boolean;
}

interface AutomationConfigurationsSheetProps {
    onSheetOpenChange: (open: boolean) => void;
    project: AutomationProjectType;
    sheetOpen: boolean;
}

const AutomationConfigurationsSheet = ({onSheetOpenChange, project, sheetOpen}: AutomationConfigurationsSheetProps) => {
    const [configurationDialog, setConfigurationDialog] = useState<ConfigurationDialogStateI | undefined>();
    const [showPublishDialog, setShowPublishDialog] = useState(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {isFetching: configurationsIsFetching, refetch: refetchConfigurations} =
        useAutomationProjectConfigurationsQuery(
            {environmentId: String(DEVELOPMENT_ENVIRONMENT), projectId: project.id},
            {enabled: sheetOpen}
        );

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

                        <SheetTitle className="text-lg font-semibold">Automation Configurations</SheetTitle>
                    </div>

                    <div className="flex items-center gap-x-1">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    aria-label="Refresh automation configurations"
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
                    <AutomationProjectConfigurationList
                        environmentId={DEVELOPMENT_ENVIRONMENT}
                        onCreate={() => setConfigurationDialog({})}
                        onEdit={(configuration) => setConfigurationDialog({configuration})}
                        onPublishProject={() => setShowPublishDialog(true)}
                        onUpdateVersion={(configuration) =>
                            setConfigurationDialog({configuration, updateProjectVersion: true})
                        }
                        project={project}
                    />
                </div>

                <footer className="flex shrink-0 justify-end gap-x-2 rounded-b-md border-t border-t-border/50 bg-surface-neutral-primary p-3">
                    <Button label="Close" onClick={() => onSheetOpenChange(false)} variant="outline" />
                </footer>

                {configurationDialog && (
                    <AutomationProjectConfigurationDialog
                        configuration={configurationDialog.configuration}
                        onClose={() => setConfigurationDialog(undefined)}
                        project={project}
                        updateProjectVersion={configurationDialog.updateProjectVersion}
                    />
                )}

                {showPublishDialog && (
                    <AutomationProjectPublishDialog onClose={() => setShowPublishDialog(false)} project={project} />
                )}
            </SheetContent>
        </Sheet>
    );
};

export default AutomationConfigurationsSheet;
