import '@/shared/styles/dropdownMenu.css';
import {Button} from '@/components/ui/button';
import {Separator} from '@/components/ui/separator';
import EEVersion from '@/shared/edition/EEVersion';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {
    CopyIcon,
    DownloadIcon,
    EditIcon,
    GitBranchIcon,
    GitPullRequestArrowIcon,
    HistoryIcon,
    Trash2Icon,
} from 'lucide-react';
import {RefObject} from 'react';

const ProjectTabButtons = ({
    hiddenFileInputRef,
    onCloseDropdownMenuClick,
    onDeleteProjectClick,
    onDuplicateProjectClick,
    onPullProjectFromGitClick,
    onShowEditProjectDialogClick,
    onShowProjectGitConfigurationDialog,
    onShowProjectVersionHistorySheet,
    projectGitConfigurationEnabled,
}: {
    hiddenFileInputRef: RefObject<HTMLInputElement>;
    onCloseDropdownMenuClick: () => void;
    onDeleteProjectClick: () => void;
    onDuplicateProjectClick: () => void;
    onShowEditProjectDialogClick: () => void;
    onPullProjectFromGitClick: () => void;
    onShowProjectGitConfigurationDialog: () => void;
    onShowProjectVersionHistorySheet: () => void;
    projectGitConfigurationEnabled: boolean;
}) => {
    const ff_1039 = useFeatureFlagsStore()('ff-1039');

    const handleButtonClick = (event: React.MouseEvent<HTMLDivElement>) => {
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

            <Button
                className="dropdown-menu-item"
                onClick={() => {
                    if (hiddenFileInputRef.current) {
                        hiddenFileInputRef.current.click();
                    }
                }}
                variant="ghost"
            >
                <DownloadIcon /> Import Workflow
            </Button>

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
