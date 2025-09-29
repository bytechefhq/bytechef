import '@/shared/styles/dropdownMenu.css';
import {Button} from '@/components/ui/button';
import {Separator} from '@/components/ui/separator';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {CopyIcon, DownloadIcon, EditIcon, Share2Icon, Trash2Icon} from 'lucide-react';
import {MouseEvent} from 'react';

const WorkflowTabButtons = ({
    onCloseDropdownMenu,
    onDuplicateWorkflow,
    onShareWorkflow,
    onShowDeleteWorkflowAlertDialog,
    onShowEditWorkflowDialog,
    workflowId,
}: {
    onCloseDropdownMenu: () => void;
    onDuplicateWorkflow: () => void;
    onShareWorkflow: () => void;
    onShowEditWorkflowDialog: () => void;
    onShowDeleteWorkflowAlertDialog: () => void;
    workflowId: string;
}) => {
    const templatesSubmissionForm = useApplicationInfoStore((state) => state.templatesSubmissionForm.workflows);

    const ff_1042 = useFeatureFlagsStore()('ff-1042');
    const ff_2939 = useFeatureFlagsStore()('ff-2939');

    const handleButtonClick = (event: MouseEvent<HTMLDivElement>) => {
        if ((event.target as HTMLElement).tagName === 'BUTTON') {
            onCloseDropdownMenu();
        }
    };

    return (
        <div className="flex flex-col" onClick={handleButtonClick}>
            <Button className="dropdown-menu-item" onClick={onShowEditWorkflowDialog} variant="ghost">
                <EditIcon /> Edit
            </Button>

            <Button className="dropdown-menu-item" onClick={onDuplicateWorkflow} variant="ghost">
                <CopyIcon /> Duplicate
            </Button>

            {ff_1042 && (
                <Button className="dropdown-menu-item" onClick={onShareWorkflow} variant="ghost">
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

            <Button
                className="dropdown-menu-item"
                onClick={() => (window.location.href = `/api/automation/internal/workflows/${workflowId}/export`)}
                variant="ghost"
            >
                <DownloadIcon /> Export
            </Button>

            <Separator />

            <Button
                className="dropdown-menu-item-destructive"
                onClick={() => onShowDeleteWorkflowAlertDialog()}
                variant="ghost"
            >
                <Trash2Icon /> Delete
            </Button>
        </div>
    );
};

export default WorkflowTabButtons;
