import '@/shared/styles/dropdownMenu.css';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {getWorkflowNodeMenuItems} from '@/pages/platform/workflow-editor/utils/getWorkflowNodeMenuItems';
import {NodeDataType} from '@/shared/types';
import {ReactNode, useCallback, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

interface WorkflowNodeDropdownMenuProps {
    canPaste?: boolean;
    data: NodeDataType;
    hasSavedPosition: boolean;
    onCopy?: () => void;
    onCut?: () => void;
    onDelete: () => void;
    onInfo?: () => void;
    onOpenChange?: (open: boolean) => void;
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
    trigger: ReactNode;
}

const WorkflowNodeDropdownMenu = ({
    canPaste = false,
    data,
    hasSavedPosition,
    onCopy,
    onCut,
    onDelete,
    onInfo,
    onOpenChange,
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
    trigger,
}: WorkflowNodeDropdownMenuProps) => {
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

    const copiedNode = useWorkflowEditorStore(useShallow((state) => state.copiedNode));

    const handleDeleteClick = useCallback(() => setDeleteDialogOpen(true), []);

    const handleDeleteCancel = useCallback(() => setDeleteDialogOpen(false), []);

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
            <DropdownMenu modal={false} onOpenChange={onOpenChange}>
                <DropdownMenuTrigger asChild>{trigger}</DropdownMenuTrigger>

                <DropdownMenuContent
                    align="start"
                    className="w-workflow-node-context-menu-width p-0"
                    onCloseAutoFocus={(event) => event.preventDefault()}
                    side="right"
                >
                    {menuItems.map((menuItem) =>
                        menuItem.type === 'separator' ? (
                            <DropdownMenuSeparator className="m-0" key={menuItem.key} />
                        ) : (
                            <DropdownMenuItem
                                className={twMerge(
                                    menuItem.variant === 'destructive'
                                        ? 'dropdown-menu-item-destructive'
                                        : 'dropdown-menu-item',
                                    menuItem.key === 'paste' ? 'w-full' : 'gap-2'
                                )}
                                key={menuItem.key}
                                onSelect={menuItem.onSelect}
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
                            </DropdownMenuItem>
                        )
                    )}
                </DropdownMenuContent>
            </DropdownMenu>

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

export default WorkflowNodeDropdownMenu;
