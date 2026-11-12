import {DropdownMenuItem, DropdownMenuSeparator} from '@/components/DropdownMenu/DropdownMenu';
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

const ProjectTabButtons = ({
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
    projectGitConfigurationEnabled,
    projectId,
}: {
    importN8nWorkflowDisabled: boolean;
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

    return (
        <>
            <DropdownMenuItem icon={<EditIcon />} label="Edit" onSelect={onShowEditProjectDialogClick} />

            <DropdownMenuItem icon={<CopyIcon />} label="Duplicate" onSelect={onDuplicateProjectClick} />

            <DropdownMenuItem icon={<Share2Icon />} label="Share" onSelect={onShareProject} />

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

            {ff_1039 && (
                <EEVersion hidden={true}>
                    <DropdownMenuItem
                        disabled={!projectGitConfigurationEnabled}
                        icon={<GitPullRequestArrowIcon />}
                        label="Pull Project from Git"
                        onSelect={onPullProjectFromGitClick}
                    />

                    <DropdownMenuItem
                        icon={<GitBranchIcon />}
                        label="Git Configuration"
                        onSelect={onShowProjectGitConfigurationDialog}
                    />

                    <DropdownMenuSeparator />
                </EEVersion>
            )}

            <DropdownMenuItem
                icon={<HistoryIcon />}
                label="Project History"
                onSelect={onShowProjectVersionHistorySheet}
            />

            <DropdownMenuSeparator />

            <DropdownMenuItem
                icon={<Trash2Icon />}
                label="Delete"
                onSelect={onDeleteProjectClick}
                variant="destructive"
            />
        </>
    );
};

export default ProjectTabButtons;
