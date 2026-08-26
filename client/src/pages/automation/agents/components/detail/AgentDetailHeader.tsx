import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {ButtonGroup} from '@/components/ui/button-group';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import AgentDialog from '@/pages/automation/agents/components/AgentDialog';
import AgentVisibilityDialog from '@/pages/automation/agents/components/detail/AgentVisibilityDialog';
import exportAgent from '@/pages/automation/agents/utils/agentImportExport';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import ProjectVersionHistorySheet from '@/pages/automation/project/components/ProjectVersionHistorySheet';
import PublishPopover from '@/pages/automation/project/components/project-header/components/PublishPopover';
import {ResourceVisibilityValueType} from '@/shared/components/visibility/ResourceVisibilityPicker';
import {useIsVisibilityEditionEnabled} from '@/shared/hooks/useVisibilityFeatureEnabled';
import Header from '@/shared/layout/Header';
import {ProjectDeployment, ProjectStatus} from '@/shared/middleware/automation/configuration';
import {
    useAiAgentVersionsQuery,
    useDeleteAiAgentMutation,
    usePublishAiAgentMutation,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {
    DownloadIcon,
    EllipsisVerticalIcon,
    EyeIcon,
    HistoryIcon,
    PencilIcon,
    PlayIcon,
    RocketIcon,
    SparklesIcon,
    Trash2Icon,
} from 'lucide-react';
import {useMemo, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';

interface AgentDetailHeaderProps {
    description?: string | null;
    id: string;
    lastPublishedVersion: number;
    onAskCopilot?: () => void;
    onToggleTestPanel: () => void;
    projectId: string;
    testPanelOpen: boolean;
    title: string;
    visibility?: ResourceVisibilityValueType;
}

const AgentDetailHeader = ({
    description,
    id,
    lastPublishedVersion,
    onAskCopilot,
    onToggleTestPanel,
    projectId,
    testPanelOpen,
    title,
    visibility,
}: AgentDetailHeaderProps) => {
    const [showDeployDialog, setShowDeployDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showVersionHistorySheet, setShowVersionHistorySheet] = useState(false);
    const [showVisibilityDialog, setShowVisibilityDialog] = useState(false);

    // The edition primitive rather than useVisibilityFeatureEnabled: the menu item only needs to know the
    // feature exists, and the dialog behind it does its own workspace-context check.
    const visibilityEnabled = useIsVisibilityEditionEnabled();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    // Only fetched once the sheet is opened: the history is a rarely-used view, and every publish invalidates it
    // through invalidateAgentQueries anyway.
    const {data: agentVersionsData} = useAiAgentVersionsQuery({id}, {enabled: showVersionHistorySheet});

    const publishAgentMutation = usePublishAiAgentMutation({
        onError: (error) => {
            toast.error(error instanceof Error ? error.message : 'Failed to publish the agent.');
        },
        onSuccess: () => {
            invalidateAgentQueries(queryClient);

            toast.success('Agent published.');
        },
    });

    const deleteAgentMutation = useDeleteAiAgentMutation({
        onError: (error) => {
            toast.error(error instanceof Error ? error.message : 'Failed to delete the agent.');
        },
        onSuccess: () => {
            invalidateAgentQueries(queryClient);

            // The page being viewed is gone, so returning to the list is the only sensible destination.
            navigate('/automation/agents');
        },
    });

    // Mirrors the project header's pill, which names the version the editor WRITES into and is therefore
    // always the draft: Project.publish() stamps the current version PUBLISHED and appends a fresh draft in
    // the same call, and getLastStatus() reads that newest version — so ProjectTitle renders DRAFT for the
    // whole life of a project, publishes included. An agent's backing project does exactly the same thing
    // (see AiAgentFacadeImpl.publishProjectVersion), and every agent mutation regenerates that draft's
    // workflow, so the draft here is always the version above the last published one — which makes a
    // never-published agent V1, not V0. The published version is what Deploy and the agent LIST pill report;
    // this one deliberately does not.

    // Only a published version has a workflow a ProjectDeployment can reference.
    const deployable = lastPublishedVersion > 0;

    const handlePublishSubmit = ({description, onSuccess}: {description?: string; onSuccess: () => void}) => {
        publishAgentMutation.mutate({description, id}, {onSuccess});
    };

    const handleExportClick = async () => {
        try {
            await exportAgent(id, title);
        } catch (error) {
            toast.error(error instanceof Error ? error.message : 'Failed to export the agent.');
        }
    };

    // The sheet is the project one: an agent version IS a ProjectVersion of the hidden backing project, so the
    // GraphQL rows are mapped into that shape rather than the sheet being duplicated for a second date format.
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
                        {/* The test panel starts closed and shares the right rail with the copilot, so the
                            two never compete for it — and opening the copilot no longer has to tear a
                            mounted panel out from under the layout.

                            The icon avoids two collisions. Not a flask — the simple editor's header uses
                            FlaskConicalIcon for the evals panel, which is a different thing. And not a bot —
                            the sidebar and every agent row already use one to mean "an agent", so a bot here
                            read as another agent rather than as an action. A play icon matches the panel's own
                            "Test Agent" button. */}

                        {/* A labelled Test button, the same shape the project header's own run control has —
                            testing an agent is its primary action, not an icon-sized afterthought. */}

                        <Button
                            aria-label={testPanelOpen ? 'Hide Test Agent panel' : 'Test Agent'}
                            icon={<PlayIcon />}
                            label="Test"
                            onClick={onToggleTestPanel}
                            variant={testPanelOpen ? 'secondary' : 'default'}
                        />

                        {/* Publish and Deploy as one segmented group of outline buttons beside the primary Test
                            button, the same arrangement and hierarchy the project header uses. Publish takes a
                            description through the project's own popover, which is what fills the version history
                            the button beside it opens. */}

                        <ButtonGroup>
                            <PublishPopover
                                isPending={publishAgentMutation.isPending}
                                onPublishProjectSubmit={handlePublishSubmit}
                                title="Publish Agent"
                                tooltip="Publish the agent"
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

                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button
                                    aria-label="Agent menu"
                                    icon={<EllipsisVerticalIcon />}
                                    size="icon"
                                    variant="ghost"
                                />
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end">
                                {/* In the menu rather than as a header button of its own, the way the project
                                    header keeps Project History in its settings menu. */}

                                <DropdownMenuItem onClick={() => setShowVersionHistorySheet(true)}>
                                    <HistoryIcon /> Agent History
                                </DropdownMenuItem>

                                <DropdownMenuItem onClick={() => setShowEditDialog(true)}>
                                    <PencilIcon /> Edit
                                </DropdownMenuItem>

                                {/* "Who can see this" rather than "Visibility": on an agent the short word reads
                                    as "who may talk to it", which this control does not change. */}

                                {visibilityEnabled && (
                                    <DropdownMenuItem onClick={() => setShowVisibilityDialog(true)}>
                                        <EyeIcon /> Who Can See This
                                    </DropdownMenuItem>
                                )}

                                <DropdownMenuItem onClick={handleExportClick}>
                                    <DownloadIcon /> Export
                                </DropdownMenuItem>

                                <DropdownMenuSeparator />

                                <DropdownMenuItem
                                    disabled={deleteAgentMutation.isPending}
                                    onClick={() => deleteAgentMutation.mutate({id})}
                                    variant="destructive"
                                >
                                    <Trash2Icon /> Delete
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                }
                title={
                    <div className="flex items-center space-x-2">
                        <span>{title}</span>

                        <Badge
                            className="flex space-x-1 bg-surface-neutral-primary"
                            styleType="outline-outline"
                            weight="semibold"
                        >
                            <span>V{lastPublishedVersion + 1}</span>

                            <span>DRAFT</span>
                        </Badge>
                    </div>
                }
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

            {/* Controlled: the menu item that opens it unmounts on select, so the dialog cannot hang off a
                trigger inside the menu. */}

            <AgentDialog agent={{description, id, title}} onOpenChange={setShowEditDialog} open={showEditDialog} />

            {showVisibilityDialog && (
                <AgentVisibilityDialog
                    agentId={id}
                    onClose={() => setShowVisibilityDialog(false)}
                    visibility={visibility}
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
