import {DropdownMenuItem, DropdownMenuSeparator} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import EEVersion from '@/shared/edition/EEVersion';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {useVisibilityFeatureEnabled} from '@/shared/hooks/useVisibilityFeatureEnabled';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {
    CopyIcon,
    DownloadIcon,
    EditIcon,
    GitBranchIcon,
    GitPullRequestArrowIcon,
    HistoryIcon,
    LayoutTemplateIcon,
    LockIcon,
    PlusIcon,
    Share2Icon,
    Trash2Icon,
    UploadIcon,
} from 'lucide-react';

const ProjectTabButtons = ({
    canManageVisibility,
    importN8nWorkflowDisabled,
    onDeleteProjectClick,
    onDuplicateProjectClick,
    onImportN8nWorkflowClick,
    onImportWorkflowClick,
    onNewWorkflowClick,
    onNewWorkflowFromTemplateClick,
    onPullProjectFromGitClick,
    onShareProject,
    onShowEditProjectDialogClick,
    onShowProjectGitConfigurationDialog,
    onShowProjectVersionHistorySheet,
    onShowVisibilityDialog,
    projectGitConfigurationEnabled,
    projectId,
}: {
    canManageVisibility: boolean;
    importN8nWorkflowDisabled: boolean;
    onDeleteProjectClick: () => void;
    onDuplicateProjectClick: () => void;
    onImportN8nWorkflowClick: () => void;
    onImportWorkflowClick: () => void;
    onNewWorkflowClick: () => void;
    onNewWorkflowFromTemplateClick: () => void;
    onPullProjectFromGitClick: () => void;
    onShareProject: () => void;
    onShowEditProjectDialogClick: () => void;
    onShowProjectGitConfigurationDialog: () => void;
    onShowProjectVersionHistorySheet: () => void;
    onShowVisibilityDialog: () => void;
    projectGitConfigurationEnabled: boolean;
    projectId: number;
}) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const templatesSubmissionForm = useApplicationInfoStore((state) => state.templatesSubmissionForm.projects);

    const gitIntegrationEnabled = useFeatureFlagsStore()('ff-1039');

    // Each scope below is the one the server actually refuses the corresponding call with, so a hidden item and a
    // rejected request always agree:
    //   Edit   -> ProjectFacadeImpl.updateProject        @PreAuthorize hasPermission(#projectDTO.id, 'Project', 'WORKFLOW_EDIT')
    //   Share  -> ProjectFacadeImpl.exportSharedProject  @PreAuthorize hasPermission(#id, 'Project', 'PROJECT_SETTINGS')
    //             ProjectFacadeImpl.deleteSharedProject  same scope, so one flag covers the whole dialog
    //   Delete -> ProjectFacadeImpl.deleteProject        @PreAuthorize hasPermission(#id, 'Project', 'PROJECT_DELETE')
    //   Dup.   -> ProjectFacadeImpl.duplicateProject    @PreAuthorize hasPermission(#id, 'Project', 'WORKFLOW_VIEW')
    //                                                     and hasPermission(#id, 'Project', 'PROJECT_CREATE')
    //             Only PROJECT_CREATE is checked here: WORKFLOW_VIEW is already a precondition of loading this project.
    //   New, from template and import workflow -> WORKFLOW_CREATE, the scope the project list's same actions check
    //   Pull   -> ProjectGitFacadeImpl.pullProjectFromGit
    //                                                   @PreAuthorize hasPermission(#projectId, 'Project',
    //                                                     'PROJECT_PULL')
    //   Git    -> ProjectGitFacadeImpl.getRemoteBranches  (the dialog's branch list)
    //   Config    ProjectGitConfigurationServiceImpl.save (its submit)
    //                                                   both @PreAuthorize hasPermission(..., 'Project',
    //                                                     'WORKSPACE_MANAGE')
    // Export and Project History are intentionally ungated: the server asks only for WORKFLOW_VIEW on those, which is
    // already a precondition of loading this project at all.
    const canCreateProject = useHasWorkspaceScope(currentWorkspaceId, 'PROJECT_CREATE');
    const canCreateWorkflow = useHasWorkspaceScope(currentWorkspaceId, 'WORKFLOW_CREATE');
    const canDeleteProject = useHasWorkspaceScope(currentWorkspaceId, 'PROJECT_DELETE');
    const canEditProject = useHasWorkspaceScope(currentWorkspaceId, 'WORKFLOW_EDIT');
    const canManageProjectSettings = useHasWorkspaceScope(currentWorkspaceId, 'PROJECT_SETTINGS');
    const canConfigureProjectGit = useHasWorkspaceScope(currentWorkspaceId, 'WORKSPACE_MANAGE');
    const canPullProjectFromGit = useHasWorkspaceScope(currentWorkspaceId, 'PROJECT_PULL');

    const {enabled: visibilityFeatureEnabled} = useVisibilityFeatureEnabled();

    return (
        <>
            {canEditProject && (
                <DropdownMenuItem icon={<EditIcon />} label="Edit" onSelect={onShowEditProjectDialogClick} />
            )}

            {canCreateProject && (
                <DropdownMenuItem icon={<CopyIcon />} label="Duplicate" onSelect={onDuplicateProjectClick} />
            )}

            {canManageProjectSettings && (
                <DropdownMenuItem icon={<Share2Icon />} label="Share" onSelect={onShareProject} />
            )}

            {visibilityFeatureEnabled && canManageVisibility && (
                <DropdownMenuItem icon={<LockIcon />} label="Visibility" onSelect={onShowVisibilityDialog} />
            )}

            {templatesSubmissionForm && (
                <DropdownMenuItem
                    icon={<Share2Icon />}
                    label="Share with Community"
                    onSelect={() => window.open(templatesSubmissionForm, '_blank')}
                />
            )}

            <DropdownMenuItem
                icon={<DownloadIcon />}
                label="Export"
                onSelect={() => (window.location.href = `/api/automation/internal/projects/${projectId}/export`)}
            />

            <DropdownMenuSeparator />

            {canCreateWorkflow && (
                <>
                    <DropdownMenuItem icon={<PlusIcon />} label="New Workflow" onSelect={onNewWorkflowClick} />

                    <DropdownMenuItem
                        icon={<LayoutTemplateIcon />}
                        label="Workflow from Template"
                        onSelect={onNewWorkflowFromTemplateClick}
                    />

                    <DropdownMenuItem icon={<UploadIcon />} label="Import Workflow" onSelect={onImportWorkflowClick} />

                    <Tooltip>
                        <TooltipTrigger asChild>
                            <span className="block">
                                <DropdownMenuItem
                                    disabled={importN8nWorkflowDisabled}
                                    icon={<UploadIcon />}
                                    label="Import n8n Workflow"
                                    onSelect={onImportN8nWorkflowClick}
                                />
                            </span>
                        </TooltipTrigger>

                        {importN8nWorkflowDisabled && (
                            <TooltipContent>Enable an AI provider to import n8n workflows.</TooltipContent>
                        )}
                    </Tooltip>

                    <DropdownMenuSeparator />
                </>
            )}

            {gitIntegrationEnabled && (
                <EEVersion hidden={true}>
                    {canPullProjectFromGit && (
                        <DropdownMenuItem
                            disabled={!projectGitConfigurationEnabled}
                            icon={<GitPullRequestArrowIcon />}
                            label="Pull Project from Git"
                            onSelect={onPullProjectFromGitClick}
                        />
                    )}

                    {canConfigureProjectGit && (
                        <DropdownMenuItem
                            icon={<GitBranchIcon />}
                            label="Git Configuration"
                            onSelect={onShowProjectGitConfigurationDialog}
                        />
                    )}

                    <DropdownMenuSeparator />
                </EEVersion>
            )}

            <DropdownMenuItem
                icon={<HistoryIcon />}
                label="Project History"
                onSelect={onShowProjectVersionHistorySheet}
            />

            {canDeleteProject && (
                <>
                    <DropdownMenuSeparator />

                    <DropdownMenuItem
                        icon={<Trash2Icon />}
                        label="Delete"
                        onSelect={onDeleteProjectClick}
                        variant="destructive"
                    />
                </>
            )}
        </>
    );
};

export default ProjectTabButtons;
