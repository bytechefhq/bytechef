import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import WorkflowNodeDeleteAlertDialog from '@/pages/platform/workflow-editor/components/WorkflowNodeDeleteAlertDialog';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {getWorkflowNodeMenuItems} from '@/pages/platform/workflow-editor/utils/getWorkflowNodeMenuItems';
import {NodeDataType} from '@/shared/types';
import {ReactNode, useCallback, useMemo, useState} from 'react';
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
    onToggleDisabled?: () => void;
    showCopyAction?: boolean;
    showCutAction?: boolean;
    showDeleteAction?: boolean;
    showDisableAction?: boolean;
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
    onToggleDisabled,
    showCopyAction = false,
    showCutAction = false,
    showDeleteAction = false,
    showDisableAction = false,
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
                onToggleDisabled,
                showCopyAction,
                showCutAction,
                showDeleteAction,
                showDisableAction,
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
            onToggleDisabled,
            showCopyAction,
            showCutAction,
            showDeleteAction,
            showDisableAction,
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
                    className="w-workflow-node-context-menu-width"
                    onCloseAutoFocus={(event) => event.preventDefault()}
                    side="right"
                >
                    {menuItems.map((menuItem) =>
                        menuItem.type === 'separator' ? (
                            <DropdownMenuSeparator key={menuItem.key} />
                        ) : (
                            <DropdownMenuItem
                                className={menuItem.key === 'paste' ? 'w-full' : undefined}
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

            {showDeleteAction && (
                <WorkflowNodeDeleteAlertDialog
                    nodeLabel={data.label}
                    onCancel={handleDeleteCancel}
                    onConfirm={onDelete}
                    open={deleteDialogOpen}
                />
            )}
        </>
    );
};

export default WorkflowNodeDropdownMenu;
