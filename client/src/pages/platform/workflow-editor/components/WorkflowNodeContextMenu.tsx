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
import {
    ArrowLeftRightIcon,
    ClipboardPlusIcon,
    CopyIcon,
    RefreshCcwIcon,
    TextCursorInputIcon,
    Trash2Icon,
} from 'lucide-react';
import {ReactNode, useCallback, useMemo, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

interface WorkflowNodeContextMenuProps {
    canPaste: boolean;
    children: ReactNode;
    data: NodeDataType;
    hasSavedPosition: boolean;
    onContextMenuOpenChange?: (open: boolean) => void;
    onCopy: () => void;
    onDelete: () => void;
    onPaste: () => void;
    onRename: () => void;
    onResetPosition: () => void;
    onSwitch: () => void;
}

const WorkflowNodeContextMenu = ({
    canPaste,
    children,
    data,
    hasSavedPosition,
    onContextMenuOpenChange,
    onCopy,
    onDelete,
    onPaste,
    onRename,
    onResetPosition,
    onSwitch,
}: WorkflowNodeContextMenuProps) => {
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

    const {copiedNode, setContextMenuOpen} = useWorkflowEditorStore(
        useShallow((state) => ({
            copiedNode: state.copiedNode,
            setContextMenuOpen: state.setContextMenuOpen,
        }))
    );

    const handleOpenChange = useCallback(
        (open: boolean) => {
            setContextMenuOpen(open);
            onContextMenuOpenChange?.(open);
        },
        [onContextMenuOpenChange, setContextMenuOpen]
    );

    const handleDeleteClick = useCallback(() => setDeleteDialogOpen(true), []);

    const handleDeleteCancel = useCallback(() => setDeleteDialogOpen(false), []);

    const pasteMenuItem = useMemo(() => {
        if (!canPaste || !copiedNode) {
            return null;
        }

        const copiedNodeLabel = copiedNode.label ?? copiedNode.componentName ?? 'Node';

        return (
            <ContextMenuItem className="flex w-full flex-col items-start gap-1" onClick={onPaste}>
                <div className="flex w-full items-center gap-2 self-stretch text-content-neutral-primary">
                    <ClipboardPlusIcon className="size-4 shrink-0" />

                    <span>Paste After</span>
                </div>

                <div className="flex w-full items-center gap-2 text-content-neutral-secondary">
                    <span className="flex size-4 shrink-0 items-center justify-center">{copiedNode.icon ?? null}</span>

                    <span
                        className="line-clamp-1 flex-1 text-xs font-normal"
                        title={`${copiedNodeLabel}${copiedNode.workflowNodeName ? ` (${copiedNode.workflowNodeName})` : ''}`}
                    >
                        {copiedNodeLabel}

                        {copiedNode.workflowNodeName ? ` (${copiedNode.workflowNodeName})` : null}
                    </span>
                </div>
            </ContextMenuItem>
        );
    }, [canPaste, copiedNode, onPaste]);

    return (
        <>
            <ContextMenu onOpenChange={handleOpenChange}>
                <ContextMenuTrigger asChild>{children}</ContextMenuTrigger>

                <ContextMenuContent className="w-[280px]">
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
                            <ContextMenuItem onClick={onCopy}>
                                <CopyIcon />
                                Copy
                            </ContextMenuItem>

                            {pasteMenuItem}

                            <ContextMenuSeparator />

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

                            <ContextMenuItem destructive onClick={handleDeleteClick}>
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
                    onCancel={handleDeleteCancel}
                    onDelete={onDelete}
                    open={deleteDialogOpen}
                />
            )}
        </>
    );
};

export default WorkflowNodeContextMenu;
