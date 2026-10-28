import Button from '@/components/Button/Button';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectDeploymentsPanel from '@/pages/automation/project/components/ProjectDeploymentsPanel';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ENVIRONMENT_CONFIGS} from '@/shared/constants/environmentConfigs';
import {Project} from '@/shared/middleware/automation/configuration';
import {useGetWorkspaceProjectDeploymentsQuery} from '@/shared/queries/automation/projectDeployments.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ExternalLinkIcon, RefreshCwIcon, RocketIcon} from 'lucide-react';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

export interface ProjectDeploymentsSheetStateI {
    expandedProjectDeploymentId?: number;
}

interface ProjectDeploymentsSheetProps {
    expandedProjectDeploymentId?: number;
    onSheetOpenChange: (open: boolean) => void;
    project: Project;
    sheetOpen: boolean;
}

const ProjectDeploymentsSheet = ({
    expandedProjectDeploymentId: initialExpandedProjectDeploymentId,
    onSheetOpenChange,
    project,
    sheetOpen,
}: ProjectDeploymentsSheetProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const [expandedProjectDeploymentId, setExpandedProjectDeploymentId] = useState<number | undefined>(
        initialExpandedProjectDeploymentId
    );

    const navigate = useNavigate();

    const {isFetching: projectDeploymentsIsFetching, refetch: refetchProjectDeployments} =
        useGetWorkspaceProjectDeploymentsQuery(
            {
                id: currentWorkspaceId ?? 0,
                projectId: project.id ?? 0,
            },
            sheetOpen && !!currentWorkspaceId && !!project.id
        );

    const handleOpenInProjectsClick = () => {
        onSheetOpenChange(false);

        void navigate(`/automation/projects?projectId=${project.id}&tab=deployments`);
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
                        <RocketIcon className="size-5" />

                        <SheetTitle className="text-lg font-semibold">Project Deployments</SheetTitle>
                    </div>

                    <div className="flex items-center gap-x-1">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    aria-label="Refresh project deployments"
                                    disabled={projectDeploymentsIsFetching}
                                    icon={
                                        <RefreshCwIcon
                                            className={twMerge(projectDeploymentsIsFetching && 'animate-spin')}
                                        />
                                    }
                                    onClick={() => void refetchProjectDeployments()}
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
                    <ProjectDeploymentsPanel
                        enabled={sheetOpen}
                        expandedProjectDeploymentId={expandedProjectDeploymentId}
                        onProjectDeploymentSaved={setExpandedProjectDeploymentId}
                        project={project}
                        showEnvironment
                    />
                </div>

                <footer className="flex shrink-0 justify-end gap-x-2 rounded-b-md border-t border-t-border/50 bg-surface-neutral-primary p-3">
                    <Button label="Close" onClick={() => onSheetOpenChange(false)} variant="outline" />

                    <Button icon={<ExternalLinkIcon />} label="Open in Projects" onClick={handleOpenInProjectsClick} />
                </footer>
            </SheetContent>
        </Sheet>
    );
};

export default ProjectDeploymentsSheet;
