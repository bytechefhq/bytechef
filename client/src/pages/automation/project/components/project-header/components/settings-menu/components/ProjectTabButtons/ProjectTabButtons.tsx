import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import {Separator} from '@/components/ui/separator';
import EEVersion from '@/shared/edition/EEVersion';
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
    UsersIcon,
} from 'lucide-react';
import {MouseEvent} from 'react';

const ProjectTabButtons = ({
    canViewMembers = true,
    onCloseDropdownMenuClick,
    onDeleteProjectClick,
    onDuplicateProjectClick,
    onMembersClick,
    onPullProjectFromGitClick,
    onShareProject,
    onShowEditProjectDialogClick,
    onShowProjectGitConfigurationDialog,
    onShowProjectVersionHistorySheet,
    projectGitConfigurationEnabled,
    projectId,
}: {
    canViewMembers?: boolean;
    onCloseDropdownMenuClick: () => void;
    onDeleteProjectClick: () => void;
    onDuplicateProjectClick: () => void;
    onMembersClick: () => void;
    onPullProjectFromGitClick: () => void;
    onShareProject: () => void;
    onShowEditProjectDialogClick: () => void;
    onShowProjectGitConfigurationDialog: () => void;
    onShowProjectVersionHistorySheet: () => void;
    projectGitConfigurationEnabled: boolean;
    projectId: number;
}) => {
    const templatesSubmissionForm = useApplicationInfoStore((state) => state.templatesSubmissionForm.projects);

    const gitIntegrationEnabled = useFeatureFlagsStore()('ff-1039');
    const shareProjectEnabled = useFeatureFlagsStore()('ff-1042');
    const projectExportEnabled = useFeatureFlagsStore()('ff-2482');
    const shareWithCommunityEnabled = useFeatureFlagsStore()('ff-2939');

    const handleButtonClick = (event: MouseEvent<HTMLDivElement>) => {
        if ((event.target as HTMLElement).tagName === 'BUTTON') {
            onCloseDropdownMenuClick();
        }
    };

    return (
        <div className="flex flex-col" onClick={handleButtonClick}>
            <Button
                aria-label="Edit Project Button"
                className="dropdown-menu-item"
                icon={<EditIcon />}
                label="Edit"
                onClick={() => onShowEditProjectDialogClick()}
                variant="ghost"
            />

            <Button
                aria-label="Duplicate Project Button"
                className="dropdown-menu-item"
                icon={<CopyIcon />}
                label="Duplicate"
                onClick={onDuplicateProjectClick}
                variant="ghost"
            />

            {shareProjectEnabled && (
                <Button
                    aria-label="Share ProjectButton"
                    className="dropdown-menu-item"
                    icon={<Share2Icon />}
                    label="Share"
                    onClick={onShareProject}
                    variant="ghost"
                />
            )}

            {shareWithCommunityEnabled && (
                <Button
                    aria-label="Share Project with Community Button"
                    className="dropdown-menu-item"
                    icon={<Share2Icon />}
                    label="Share with Community"
                    onClick={() => {
                        if (templatesSubmissionForm) {
                            window.open(templatesSubmissionForm, '_blank');
                        }
                    }}
                    variant="ghost"
                />
            )}

            {projectExportEnabled && (
                <Button
                    aria-label="Export Project"
                    className="dropdown-menu-item"
                    icon={<DownloadIcon />}
                    label="Export"
                    onClick={() => (window.location.href = `/api/automation/internal/projects/${projectId}/export`)}
                    variant="ghost"
                />
            )}

            <Separator />

            {gitIntegrationEnabled && (
                <EEVersion hidden={true}>
                    <Button
                        aria-label="Pull Project from Git"
                        className="dropdown-menu-item"
                        disabled={!projectGitConfigurationEnabled}
                        icon={<GitPullRequestArrowIcon />}
                        label="Pull Project from Git"
                        onClick={onPullProjectFromGitClick}
                        variant="ghost"
                    />

                    <Button
                        aria-label="Git Configuration"
                        className="dropdown-menu-item"
                        icon={<GitBranchIcon />}
                        label="Git Configuration"
                        onClick={onShowProjectGitConfigurationDialog}
                        variant="ghost"
                    />

                    <Separator />
                </EEVersion>
            )}

            {canViewMembers && (
                <Button
                    aria-label="Members"
                    className="dropdown-menu-item"
                    icon={<UsersIcon />}
                    label="Members"
                    onClick={onMembersClick}
                    variant="ghost"
                />
            )}

            <Button
                aria-label="Project History"
                className="dropdown-menu-item"
                icon={<HistoryIcon />}
                label="Project History"
                onClick={onShowProjectVersionHistorySheet}
                variant="ghost"
            />

            <Separator />

            <Button
                aria-label="Delete Project"
                className="dropdown-menu-item-destructive"
                icon={<Trash2Icon />}
                label="Delete"
                onClick={onDeleteProjectClick}
                variant="ghost"
            />
        </div>
    );
};

export default ProjectTabButtons;
