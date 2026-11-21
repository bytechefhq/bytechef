import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {ClipboardIcon, DownloadIcon, EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';
import {ReactNode} from 'react';
import {toast} from 'sonner';

interface WorkflowListRowDropdownMenuProps {
    children?: ReactNode;
    exportUrl: string;
    onDeleteClick?: () => void;
    onEditClick?: () => void;
    workflowLabel?: string;
    workflowUuid?: string;
}

const WorkflowListRowDropdownMenu = ({
    children,
    exportUrl,
    onDeleteClick,
    onEditClick,
    workflowLabel,
    workflowUuid,
}: WorkflowListRowDropdownMenuProps) => {
    const [, copyToClipboard] = useCopyToClipboard();

    const handleCopyWorkflowUuidClick = async () => {
        if (workflowUuid) {
            await copyToClipboard(workflowUuid);

            toast('Workflow UUID copied to clipboard');
        }
    };

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    aria-label={workflowLabel ? `Workflow actions for ${workflowLabel}` : 'Workflow actions'}
                    icon={<EllipsisVerticalIcon />}
                    size="icon"
                    variant="ghost"
                />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                {onEditClick && <DropdownMenuItem icon={<EditIcon />} label="Edit" onClick={onEditClick} />}

                {children}

                {workflowUuid && (
                    <DropdownMenuItem
                        icon={<ClipboardIcon />}
                        label="Copy Workflow UUID"
                        onClick={handleCopyWorkflowUuidClick}
                    />
                )}

                <DropdownMenuItem
                    icon={<DownloadIcon />}
                    label="Export"
                    onClick={() => (window.location.href = exportUrl)}
                />

                {onDeleteClick && (
                    <>
                        <DropdownMenuSeparator />

                        <DropdownMenuItem
                            icon={<Trash2Icon />}
                            label="Delete"
                            onClick={onDeleteClick}
                            variant="destructive"
                        />
                    </>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default WorkflowListRowDropdownMenu;
