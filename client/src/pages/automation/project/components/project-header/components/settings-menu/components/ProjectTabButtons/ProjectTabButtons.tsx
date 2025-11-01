import '@/shared/styles/dropdownMenu.css';
import {Button} from '@/components/ui/button';
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
            <Button className="dropdown-menu-item" onClick={() => onShowEditProjectDialogClick()} variant="ghost">
                <EditIcon /> Edit
            </Button>

            <Button className="dropdown-menu-item" onClick={onDuplicateProjectClick} variant="ghost">
                <CopyIcon /> Duplicate
            </Button>

            {ff_1042 && (
                <Button className="dropdown-menu-item" onClick={onShareProject} variant="ghost">
                    <Share2Icon /> Share
                </Button>
            )}

            {ff_2939 && (
                <Button
                    className="dropdown-menu-item"
                    onClick={() => {
                        if (templatesSubmissionForm) {
                            window.open(templatesSubmissionForm, '_blank');
                        }
                    }}
                    variant="ghost"
                >
                    <Share2Icon /> Share with Community
                </Button>
            )}

            {ff_2482 && (
                <Button
                    className="dropdown-menu-item"
                    onClick={() => (window.location.href = `/api/automation/internal/projects/${projectId}/export`)}
                    variant="ghost"
                >
                    <DownloadIcon /> Export
                </Button>
            )}

            <Separator />

            {ff_1039 && (
                <EEVersion hidden={true}>
                    <Button
                        className="dropdown-menu-item"
                        disabled={!projectGitConfigurationEnabled}
                        onClick={onPullProjectFromGitClick}
                        variant="ghost"
                    >
                        <GitPullRequestArrowIcon /> Pull Project from Git
                    </Button>

                    <Button
                        className="dropdown-menu-item"
                        onClick={onShowProjectGitConfigurationDialog}
                        variant="ghost"
                    >
                        <GitBranchIcon /> Git Configuration
                    </Button>

                    <Separator />
                </EEVersion>
            )}

            <Button className="dropdown-menu-item" onClick={onShowProjectVersionHistorySheet} variant="ghost">
                <HistoryIcon /> Project History
            </Button>

            <Separator />

            <Button className="dropdown-menu-item-destructive" onClick={onDeleteProjectClick} variant="ghost">
                <Trash2Icon /> Delete
            </Button>
        </div>
    );
};

export default ProjectTabButtons;
