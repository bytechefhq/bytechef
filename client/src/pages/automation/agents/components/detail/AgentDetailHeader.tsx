import Button from '@/components/Button/Button';
import {ButtonGroup} from '@/components/ui/button-group';
import AgentDialog from '@/pages/automation/agents/components/AgentDialog';
import DeleteAgentAlertDialog from '@/pages/automation/agents/components/DeleteAgentAlertDialog';
import AgentTabButtons from '@/pages/automation/agents/components/detail/AgentTabButtons';
import useAgentActions from '@/pages/automation/agents/hooks/useAgentActions';
import exportAgent from '@/pages/automation/agents/utils/agentImportExport';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import ProjectVersionHistorySheet from '@/pages/automation/project/components/ProjectVersionHistorySheet';
import PublishPopover from '@/pages/automation/project/components/project-header/components/PublishPopover';
import SettingsMenu from '@/pages/automation/project/components/project-header/components/settings-menu/SettingsMenu';
import LoadingIndicator from '@/shared/components/LoadingIndicator';
import Header from '@/shared/layout/Header';
import {Project, ProjectDeployment, ProjectStatus} from '@/shared/middleware/automation/configuration';
import {useAiAgentVersionsQuery} from '@/shared/middleware/graphql';
import {usePublishProjectMutation} from '@/shared/mutations/automation/projects.mutations';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {onlineManager, useIsFetching, useQueryClient} from '@tanstack/react-query';
import {PlayIcon, RocketIcon, SparklesIcon} from 'lucide-react';
import {ReactNode, useMemo, useState} from 'react';
import {toast} from 'sonner';

interface AgentDetailHeaderProps {
    description?: string | null;
    id: string;
    lastPublishedVersion: number;
    leading?: ReactNode;
    onAskCopilot?: () => void;
    onToggleTestPanel: () => void;
    project?: Project;
    projectId: string;
    testPanelOpen: boolean;
    title: string;
}

const AgentDetailHeader = ({
    description,
    id,
    lastPublishedVersion,
    leading,
    onAskCopilot,
    onToggleTestPanel,
    project,
    projectId,
    testPanelOpen,
    title,
}: AgentDetailHeaderProps) => {
    const [showDeployDialog, setShowDeployDialog] = useState(false);
    const [showVersionHistorySheet, setShowVersionHistorySheet] = useState(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const isFetching = useIsFetching();
    const isOnline = onlineManager.isOnline();

    const queryClient = useQueryClient();

    const {
        deleteAgentMutation,
        handleConfirmDelete,
        handleDeleteClick,
        setShowDeleteConfirmDialog,
        setShowEditDialog,
        showDeleteConfirmDialog,
        showEditDialog,
    } = useAgentActions({
        agentId: id,
        navigateOnDelete: true,
    });

    const {data: agentVersionsData} = useAiAgentVersionsQuery({id}, {enabled: showVersionHistorySheet});

    const publishProjectMutation = usePublishProjectMutation({
        onSuccess: () => {
            invalidateAgentQueries(queryClient);

            queryClient.invalidateQueries({queryKey: ProjectKeys.project(+projectId)});

            toast.success('The project has been published.');
        },
    });

    const deployable = lastPublishedVersion > 0;

    const handlePublishSubmit = ({description, onSuccess}: {description?: string; onSuccess: () => void}) => {
        publishProjectMutation.mutate({id: +projectId, publishProjectRequest: {description}}, {onSuccess});
    };

    const handleExportClick = async () => {
        try {
            await exportAgent(id, title);
        } catch (error) {
            toast.error(error instanceof Error ? error.message : 'Failed to export the agent.');
        }
    };

    const agentVersions = useMemo(
        () =>
            (agentVersionsData?.aiAgentVersions ?? []).map((agentVersion) => ({
                description: agentVersion.description ?? undefined,
                publishedDate: agentVersion.publishedDate ? new Date(agentVersion.publishedDate) : undefined,
                status: agentVersion.status === 'PUBLISHED' ? ProjectStatus.Published : ProjectStatus.Draft,
                version: agentVersion.version,
            })),
        [agentVersionsData?.aiAgentVersions]
    );

    return (
        <>
            <Header
                centerTitle
                description={description || undefined}
                position="main"
                right={
                    <div className="flex items-center gap-1">
                        <LoadingIndicator isFetching={isFetching} isOnline={isOnline} />

                        {project && (
                            <SettingsMenu
                                firstTab={{
                                    ariaLabel: 'Agent tab',
                                    content: (onCloseDropdownMenu) => (
                                        <AgentTabButtons
                                            isDeletePending={deleteAgentMutation.isPending}
                                            onCloseDropdownMenu={onCloseDropdownMenu}
                                            onDeleteClick={handleDeleteClick}
                                            onExportClick={handleExportClick}
                                            onShowEditDialog={() => setShowEditDialog(true)}
                                            onShowVersionHistorySheet={() => setShowVersionHistorySheet(true)}
                                        />
                                    ),
                                    label: 'Agent',
                                    value: 'agent',
                                }}
                                project={project}
                            />
                        )}

                        <Button
                            aria-label={testPanelOpen ? 'Hide Test Agent panel' : 'Test Agent'}
                            icon={<PlayIcon />}
                            label="Test"
                            onClick={onToggleTestPanel}
                            variant={testPanelOpen ? 'secondary' : 'default'}
                        />

                        <ButtonGroup>
                            <PublishPopover
                                isPending={publishProjectMutation.isPending}
                                onPublishProjectSubmit={handlePublishSubmit}
                                title="Publish Project"
                                tooltip="Publishes every workflow and agent in this project."
                            />

                            <Button
                                disabled={!deployable}
                                icon={<RocketIcon />}
                                label="Deploy"
                                onClick={() => setShowDeployDialog(true)}
                                variant="outline"
                            />
                        </ButtonGroup>

                        {onAskCopilot && (
                            <Button
                                aria-label="Ask Copilot"
                                icon={<SparklesIcon className="size-4" />}
                                onClick={onAskCopilot}
                                size="icon"
                                variant="ghost"
                            />
                        )}
                    </div>
                }
                title={leading}
            />

            {showDeployDialog && (
                <ProjectDeploymentDialog
                    onClose={() => setShowDeployDialog(false)}
                    projectDeployment={
                        {
                            environmentId: currentEnvironmentId,
                            projectId: +projectId,
                            projectVersion: lastPublishedVersion,
                        } as ProjectDeployment
                    }
                    redirectOnSubmit={false}
                />
            )}

            <AgentDialog agent={{description, id, title}} onOpenChange={setShowEditDialog} open={showEditDialog} />

            {showDeleteConfirmDialog && (
                <DeleteAgentAlertDialog
                    agentTitle={title}
                    onClose={() => setShowDeleteConfirmDialog(false)}
                    onDelete={handleConfirmDelete}
                />
            )}

            {showVersionHistorySheet && (
                <ProjectVersionHistorySheet
                    onSheetOpenChange={setShowVersionHistorySheet}
                    projectVersions={agentVersions}
                    sheetOpen={showVersionHistorySheet}
                    title="Agent History"
                />
            )}
        </>
    );
};

export default AgentDetailHeader;
