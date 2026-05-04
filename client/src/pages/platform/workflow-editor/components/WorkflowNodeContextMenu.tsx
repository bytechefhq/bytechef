import '@/shared/styles/dropdownMenu.css';
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
    onCopy,
    onDelete,
    onPaste,
    onRename,
    onResetPosition,
    onSwitch,
}: WorkflowNodeContextMenuProps) => {
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

    const copiedNode = useWorkflowEditorStore(useShallow((state) => state.copiedNode));

    const handleDeleteClick = useCallback(() => setDeleteDialogOpen(true), []);

    const handleDeleteCancel = useCallback(() => setDeleteDialogOpen(false), []);

    const pasteMenuItem = useMemo(() => {
        if (!canPaste || !copiedNode) {
            return null;
        }

        const copiedNodeLabel = copiedNode.label ?? copiedNode.componentName ?? 'Node';

        return (
            <ContextMenuItem className="dropdown-menu-item flex w-full flex-col items-start gap-1" onClick={onPaste}>
                <div className="flex w-full items-center gap-2 self-stretch text-content-neutral-primary">
                    <ClipboardPlusIcon className="size-4 shrink-0" />

                    <span>Paste After</span>
                </div>

                <div className="flex w-full items-center gap-2 text-content-neutral-secondary">
                    <span className="flex size-4 shrink-0 items-center justify-center overflow-hidden [&>svg]:size-4">
                        {copiedNode.icon ?? null}
                    </span>

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
            <ContextMenu>
                <ContextMenuTrigger asChild>{children}</ContextMenuTrigger>

                <ContextMenuContent className="w-[280px] [padding:0px]">
                    {data.trigger ? (
                        <>
                            <ContextMenuItem className="dropdown-menu-item gap-2" onClick={onSwitch}>
                                <ArrowLeftRightIcon className="size-4 shrink-0" />
                                Replace
                            </ContextMenuItem>

                            <ContextMenuItem className="dropdown-menu-item gap-2" onClick={onRename}>
                                <TextCursorInputIcon className="size-4 shrink-0" />
                                Rename
                            </ContextMenuItem>
                        </>
                    ) : (
                        <>
                            <ContextMenuItem className="dropdown-menu-item gap-2" onClick={onCopy}>
                                <CopyIcon className="size-4 shrink-0" />
                                Copy
                            </ContextMenuItem>

                            {pasteMenuItem}

                            <ContextMenuSeparator className="m-0" />

                            <ContextMenuItem className="dropdown-menu-item gap-2" onClick={onRename}>
                                <TextCursorInputIcon className="size-4 shrink-0" />
                                Rename
                            </ContextMenuItem>

                            {hasSavedPosition && (
                                <ContextMenuItem className="dropdown-menu-item gap-2" onClick={onResetPosition}>
                                    <RefreshCcwIcon className="size-4 shrink-0" />
                                    Reset position
                                </ContextMenuItem>
                            )}

                            <ContextMenuSeparator className="m-0" />

                            <ContextMenuItem
                                className="dropdown-menu-item-destructive gap-2"
                                onClick={handleDeleteClick}
                            >
                                <Trash2Icon className="size-4 shrink-0" />
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
