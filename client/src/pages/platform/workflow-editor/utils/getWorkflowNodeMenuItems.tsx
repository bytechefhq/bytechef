import {NodeDataType} from '@/shared/types';
import {
    ArrowLeftRightIcon,
    ClipboardPlusIcon,
    CopyIcon,
    InfoIcon,
    RefreshCcwIcon,
    ScissorsIcon,
    TextCursorInputIcon,
    Trash2Icon,
} from 'lucide-react';
import {ReactNode} from 'react';

export type WorkflowNodeMenuItemType =
    | {type: 'separator'; key: string}
    | {
          icon: ReactNode;
          key: string;
          label: ReactNode;
          onSelect: () => void;
          type: 'item';
          variant?: 'default' | 'destructive';
      };

interface GetWorkflowNodeMenuItemsProps {
    canPaste: boolean;
    copiedNode: NodeDataType | undefined;
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
    showCopyAction: boolean;
    showCutAction: boolean;
    showDeleteAction: boolean;
    showInfoAction: boolean;
    showRenameAction: boolean;
    showReplaceAction: boolean;
}

function getPasteMenuLabel(copiedNode: NodeDataType): ReactNode {
    const copiedNodeLabel = copiedNode.label ?? copiedNode.componentName ?? 'Node';

    return (
        <div className="flex w-full flex-col items-start gap-1">
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
        </div>
    );
}

export function getWorkflowNodeMenuItems({
    canPaste,
    copiedNode,
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
    showCopyAction,
    showCutAction,
    showDeleteAction,
    showInfoAction,
    showRenameAction,
    showReplaceAction,
}: GetWorkflowNodeMenuItemsProps): WorkflowNodeMenuItemType[] {
    const menuItems: WorkflowNodeMenuItemType[] = [];

    if (data.trigger) {
        menuItems.push({
            icon: <ArrowLeftRightIcon className="size-4 shrink-0" />,
            key: 'replace',
            label: 'Replace',
            onSelect: onSwitch,
            type: 'item',
        });

        menuItems.push({
            icon: <TextCursorInputIcon className="size-4 shrink-0" />,
            key: 'rename',
            label: 'Rename',
            onSelect: onRename,
            type: 'item',
        });

        if (showInfoAction && onInfo) {
            menuItems.push({
                icon: <InfoIcon className="size-4 shrink-0" />,
                key: 'info',
                label: 'Info',
                onSelect: onInfo,
                type: 'item',
            });
        }

        return menuItems;
    }

    if (showCutAction && onCut) {
        menuItems.push({
            icon: <ScissorsIcon className="size-4 shrink-0" />,
            key: 'cut',
            label: 'Cut',
            onSelect: onCut,
            type: 'item',
        });
    }

    if (showReplaceAction) {
        menuItems.push({
            icon: <ArrowLeftRightIcon className="size-4 shrink-0" />,
            key: 'replace',
            label: 'Replace',
            onSelect: onSwitch,
            type: 'item',
        });
    }

    if (showCopyAction && onCopy) {
        menuItems.push({
            icon: <CopyIcon className="size-4 shrink-0" />,
            key: 'copy',
            label: 'Copy',
            onSelect: onCopy,
            type: 'item',
        });
    }

    if (showCopyAction && canPaste && copiedNode && onPaste) {
        menuItems.push({
            icon: null,
            key: 'paste',
            label: getPasteMenuLabel(copiedNode),
            onSelect: onPaste,
            type: 'item',
        });
    }

    const canRename = showRenameAction && !data.isNestedClusterRoot;

    const hasFirstGroup = showCutAction || showReplaceAction || showCopyAction || canPaste;
    const hasSecondGroup = canRename || hasSavedPosition || showInfoAction;

    if (hasFirstGroup && hasSecondGroup) {
        menuItems.push({key: 'separator-actions', type: 'separator'});
    }

    if (canRename) {
        menuItems.push({
            icon: <TextCursorInputIcon className="size-4 shrink-0" />,
            key: 'rename',
            label: 'Rename',
            onSelect: onRename,
            type: 'item',
        });
    }

    if (hasSavedPosition) {
        menuItems.push({
            icon: <RefreshCcwIcon className="size-4 shrink-0" />,
            key: 'reset-position',
            label: 'Reset position',
            onSelect: onResetPosition,
            type: 'item',
        });
    }

    if (showInfoAction && onInfo) {
        menuItems.push({
            icon: <InfoIcon className="size-4 shrink-0" />,
            key: 'info',
            label: 'Info',
            onSelect: onInfo,
            type: 'item',
        });
    }

    if (showDeleteAction) {
        menuItems.push({key: 'separator-delete', type: 'separator'});

        menuItems.push({
            icon: <Trash2Icon className="size-4 shrink-0" />,
            key: 'delete',
            label: 'Delete',
            onSelect: onDelete,
            type: 'item',
            variant: 'destructive',
        });
    }

    return menuItems;
}
