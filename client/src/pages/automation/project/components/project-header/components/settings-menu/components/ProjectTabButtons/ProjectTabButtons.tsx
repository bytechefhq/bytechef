import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import {Separator} from '@/components/ui/separator';
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
    Share2Icon,
    Trash2Icon,
} from 'lucide-react';
import {MouseEvent} from 'react';

const ProjectTabButtons = ({
    onCloseDropdownMenuClick,
    onDeleteProjectClick,
    onDuplicateProjectClick,
    onPullProjectFromGitClick,
    onShareProject,
    onShowEditProjectDialogClick,
    onShowProjectGitConfigurationDialog,
    onShowProjectVersionHistorySheet,
    projectGitConfigurationEnabled,
    projectId,
}: {
    onCloseDropdownMenuClick: () => void;
    onDeleteProjectClick: () => void;
    onDuplicateProjectClick: () => void;
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
