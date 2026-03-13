import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {
    ContextMenu,
    ContextMenuContent,
    ContextMenuItem,
    ContextMenuSeparator,
    ContextMenuTrigger,
} from '@/components/ui/context-menu';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {NodeDataType} from '@/shared/types';
import {ArrowLeftRightIcon, RefreshCcwIcon, TextCursorInputIcon, Trash2Icon} from 'lucide-react';
import {ReactNode, useState} from 'react';

interface WorkflowNodeContextMenuProps {
    children: ReactNode;
    data: NodeDataType;
    hasSavedPosition: boolean;
    onDelete: () => void;
    onRename: () => void;
    onResetPosition: () => void;
    onSwitch: () => void;
}

const WorkflowNodeContextMenu = ({
    children,
    data,
    hasSavedPosition,
    onDelete,
    onRename,
    onResetPosition,
    onSwitch,
}: WorkflowNodeContextMenuProps) => {
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

    const setContextMenuOpen = useWorkflowEditorStore((state) => state.setContextMenuOpen);

    return (
        <>
            <ContextMenu onOpenChange={setContextMenuOpen}>
                <ContextMenuTrigger asChild>{children}</ContextMenuTrigger>

                <ContextMenuContent>
                    {data.trigger ? (
                        <>
                            <ContextMenuItem onClick={onSwitch}>
                                <ArrowLeftRightIcon />
                                Replace
                            </ContextMenuItem>

                            <ContextMenuItem onClick={onRename}>
                                <TextCursorInputIcon />
                                Rename
                            </ContextMenuItem>
                        </>
                    ) : (
                        <>
                            <ContextMenuItem onClick={onRename}>
                                <TextCursorInputIcon />
                                Rename
                            </ContextMenuItem>

                            {hasSavedPosition && (
                                <ContextMenuItem onClick={onResetPosition}>
                                    <RefreshCcwIcon />
                                    Reset position
                                </ContextMenuItem>
                            )}

                            <ContextMenuSeparator />

                            <ContextMenuItem
                                className="text-destructive focus:text-destructive"
                                onClick={() => setDeleteDialogOpen(true)}
                            >
                                <Trash2Icon />
                                Delete
                            </ContextMenuItem>
                        </>
                    )}
                </ContextMenuContent>
            </ContextMenu>

            {!data.trigger && (
                <DeleteAlertDialog
                    nodeName={data.label}
                    onCancel={() => setDeleteDialogOpen(false)}
                    onDelete={onDelete}
                    open={deleteDialogOpen}
                />
            )}
        </>
    );
};

export default WorkflowNodeContextMenu;
