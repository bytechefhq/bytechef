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
import {getWorkflowNodeMenuItems} from '@/pages/platform/workflow-editor/utils/getWorkflowNodeMenuItems';
import {NodeDataType} from '@/shared/types';
import {ReactNode, useCallback, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

interface WorkflowNodeContextMenuProps {
    canPaste?: boolean;
    children: ReactNode;
    data: NodeDataType;
    hasSavedPosition: boolean;
    onCopy?: () => void;
    onCut?: () => void;
    onDelete: () => void;
    onInfo?: () => void;
    onPaste?: () => void;
    onRename: () => void;
    onResetPosition: () => void;
    onSwitch: () => void;
    showCopyAction?: boolean;
    showCutAction?: boolean;
    showDeleteAction?: boolean;
    showInfoAction?: boolean;
    showRenameAction?: boolean;
    showReplaceAction?: boolean;
}

const WorkflowNodeContextMenu = ({
    canPaste = false,
    children,
    data,
    hasSavedPosition,
    onCopy,
    onCut,
    onDelete,
    onInfo,
    onPaste,
    onRename,
    onResetPosition,
    onSwitch,
    showCopyAction = false,
    showCutAction = false,
    showDeleteAction = false,
    showInfoAction = false,
    showRenameAction = false,
    showReplaceAction = false,
}: WorkflowNodeContextMenuProps) => {
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [menuReady, setMenuReady] = useState(false);

    const copiedNode = useWorkflowEditorStore(useShallow((state) => state.copiedNode));

    const handleDeleteClick = useCallback(() => setDeleteDialogOpen(true), []);

    const handleDeleteCancel = useCallback(() => setDeleteDialogOpen(false), []);

    const handleOpenChange = (open: boolean) => {
        if (open) {
            setMenuReady(false);
            setTimeout(() => setMenuReady(true), 200);
        } else {
            setMenuReady(false);
        }
    };

    const menuItems = useMemo(
        () =>
            getWorkflowNodeMenuItems({
                canPaste,
                copiedNode,
                data,
                hasSavedPosition,
                onCopy,
                onCut,
                onDelete: handleDeleteClick,
                onInfo,
                onPaste,
                onRename,
                onResetPosition,
                onSwitch,
                showCopyAction,
                showCutAction,
                showDeleteAction,
                showInfoAction,
                showRenameAction,
                showReplaceAction,
            }),
        [
            canPaste,
            copiedNode,
            data,
            handleDeleteClick,
            hasSavedPosition,
            onCopy,
            onCut,
            onInfo,
            onPaste,
            onRename,
            onResetPosition,
            onSwitch,
            showCopyAction,
            showCutAction,
            showDeleteAction,
            showInfoAction,
            showRenameAction,
            showReplaceAction,
        ]
    );

    return (
        <>
            <ContextMenu onOpenChange={handleOpenChange}>
                <ContextMenuTrigger asChild>{children}</ContextMenuTrigger>

                <ContextMenuContent
                    className={twMerge('w-workflow-node-context-menu-width p-0', !menuReady && 'pointer-events-none')}
                    onCloseAutoFocus={(event) => event.preventDefault()}
                >
                    {menuItems.map((menuItem) =>
                        menuItem.type === 'separator' ? (
                            <ContextMenuSeparator className="m-0" key={menuItem.key} />
                        ) : (
                            <ContextMenuItem
                                className={twMerge(
                                    menuItem.variant === 'destructive'
                                        ? 'dropdown-menu-item-destructive'
                                        : 'dropdown-menu-item',
                                    menuItem.key === 'paste' ? 'w-full' : 'gap-2'
                                )}
                                key={menuItem.key}
                                onClick={menuItem.onSelect}
                                variant={menuItem.variant}
                            >
                                {menuItem.key === 'paste' ? (
                                    menuItem.label
                                ) : (
                                    <>
                                        {menuItem.icon}
                                        {menuItem.label}
                                    </>
                                )}
                            </ContextMenuItem>
                        )
                    )}
                </ContextMenuContent>
            </ContextMenu>

            {!data.trigger && showDeleteAction && (
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
