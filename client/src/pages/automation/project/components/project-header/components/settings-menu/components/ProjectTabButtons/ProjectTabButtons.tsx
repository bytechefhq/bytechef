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
    onShareProject: () => void;
    onShowEditProjectDialogClick: () => void;
    onPullProjectFromGitClick: () => void;
    onShowProjectGitConfigurationDialog: () => void;
    onShowProjectVersionHistorySheet: () => void;
    projectGitConfigurationEnabled: boolean;
    projectId: number;
}) => {
    const templatesSubmissionForm = useApplicationInfoStore((state) => state.templatesSubmissionForm.projects);

    const ff_1039 = useFeatureFlagsStore()('ff-1039');
    const ff_1042 = useFeatureFlagsStore()('ff-1042');
    const ff_2482 = useFeatureFlagsStore()('ff-2482');
    const ff_2939 = useFeatureFlagsStore()('ff-2939');

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

            {ff_1042 && (
                <Button
                    aria-label="Share ProjectButton"
                    className="dropdown-menu-item"
                    icon={<Share2Icon />}
                    label="Share"
                    onClick={onShareProject}
                    variant="ghost"
                />
            )}

            {ff_2939 && (
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

            {ff_2482 && (
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

            {ff_1039 && (
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
