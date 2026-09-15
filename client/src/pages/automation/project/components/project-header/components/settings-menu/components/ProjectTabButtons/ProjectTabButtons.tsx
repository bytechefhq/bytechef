import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import {Separator} from '@/components/ui/separator';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import EEVersion from '@/shared/edition/EEVersion';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
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
    PlusIcon,
    Share2Icon,
    Trash2Icon,
    UploadIcon,
} from 'lucide-react';
import {MouseEvent} from 'react';

const ProjectTabButtons = ({
    importN8nWorkflowDisabled,
    onCloseDropdownMenuClick,
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
    projectGitConfigurationEnabled,
    projectId,
}: {
    importN8nWorkflowDisabled: boolean;
    onCloseDropdownMenuClick: () => void;
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

    const handleButtonClick = (event: MouseEvent<HTMLDivElement>) => {
        if ((event.target as HTMLElement).tagName === 'BUTTON') {
            onCloseDropdownMenuClick();
        }
    };

    return (
        <div className="flex flex-col" onClick={handleButtonClick}>
            {canEditProject && (
                <Button
                    aria-label="Edit Project Button"
                    className="dropdown-menu-item"
                    icon={<EditIcon />}
                    label="Edit"
                    onClick={() => onShowEditProjectDialogClick()}
                    variant="ghost"
                />
            )}

            {canCreateProject && (
                <Button
                    aria-label="Duplicate Project Button"
                    className="dropdown-menu-item"
                    icon={<CopyIcon />}
                    label="Duplicate"
                    onClick={onDuplicateProjectClick}
                    variant="ghost"
                />
            )}

            {canManageProjectSettings && (
                <Button
                    aria-label="Share ProjectButton"
                    className="dropdown-menu-item"
                    icon={<Share2Icon />}
                    label="Share"
                    onClick={onShareProject}
                    variant="ghost"
                />
            )}

            {templatesSubmissionForm && (
                <Button
                    aria-label="Share Project with Community Button"
                    className="dropdown-menu-item"
                    icon={<Share2Icon />}
                    label="Share with Community"
                    onClick={() => window.open(templatesSubmissionForm, '_blank')}
                    variant="ghost"
                />
            )}

            <Button
                aria-label="Export Project"
                className="dropdown-menu-item"
                icon={<DownloadIcon />}
                label="Export"
                onClick={() => (window.location.href = `/api/automation/internal/projects/${projectId}/export`)}
                variant="ghost"
            />

            <Separator />

            {canCreateWorkflow && (
                <>
                    <Button
                        aria-label="New Workflow"
                        className="dropdown-menu-item"
                        icon={<PlusIcon />}
                        label="New Workflow"
                        onClick={onNewWorkflowClick}
                        variant="ghost"
                    />

                    <Button
                        aria-label="New Workflow from Template"
                        className="dropdown-menu-item"
                        icon={<LayoutTemplateIcon />}
                        label="Workflow from Template"
                        onClick={onNewWorkflowFromTemplateClick}
                        variant="ghost"
                    />

                    <Button
                        aria-label="Import Workflow"
                        className="dropdown-menu-item"
                        icon={<UploadIcon />}
                        label="Import Workflow"
                        onClick={onImportWorkflowClick}
                        variant="ghost"
                    />

                    <Tooltip>
                        <TooltipTrigger asChild>
                            <span className="block">
                                <Button
                                    aria-label="Import n8n Workflow"
                                    className="dropdown-menu-item w-full"
                                    disabled={importN8nWorkflowDisabled}
                                    icon={<UploadIcon />}
                                    label="Import n8n Workflow"
                                    onClick={onImportN8nWorkflowClick}
                                    variant="ghost"
                                />
                            </span>
                        </TooltipTrigger>

                        {importN8nWorkflowDisabled && (
                            <TooltipContent>Enable an AI provider to import n8n workflows.</TooltipContent>
                        )}
                    </Tooltip>

                    <Separator />
                </>
            )}

            {gitIntegrationEnabled && (
                <EEVersion hidden={true}>
                    {canPullProjectFromGit && (
                        <Button
                            aria-label="Pull Project from Git"
                            className="dropdown-menu-item"
                            disabled={!projectGitConfigurationEnabled}
                            icon={<GitPullRequestArrowIcon />}
                            label="Pull Project from Git"
                            onClick={onPullProjectFromGitClick}
                            variant="ghost"
                        />
                    )}

                    {canConfigureProjectGit && (
                        <Button
                            aria-label="Git Configuration"
                            className="dropdown-menu-item"
                            icon={<GitBranchIcon />}
                            label="Git Configuration"
                            onClick={onShowProjectGitConfigurationDialog}
                            variant="ghost"
                        />
                    )}

                    <Separator />
                </EEVersion>
            )}

            <Button
                aria-label="Project History"
                className="dropdown-menu-item"
                icon={<HistoryIcon />}
                label="Project History"
                onClick={onShowProjectVersionHistorySheet}
                variant="ghost"
            />

            {canDeleteProject && (
                <>
                    <Separator />

                    <Button
                        aria-label="Delete Project"
                        className="dropdown-menu-item-destructive"
                        icon={<Trash2Icon />}
                        label="Delete"
                        onClick={onDeleteProjectClick}
                        variant="ghost"
                    />
                </>
            )}
        </div>
    );
};

export default ProjectTabButtons;
