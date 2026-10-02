import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import {Separator} from '@/components/ui/separator';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
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

            <Button
                aria-label="Share ProjectButton"
                className="dropdown-menu-item"
                icon={<Share2Icon />}
                label="Share"
                onClick={onShareProject}
                variant="ghost"
            />

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
