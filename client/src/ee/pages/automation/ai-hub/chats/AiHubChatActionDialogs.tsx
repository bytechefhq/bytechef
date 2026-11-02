import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Input} from '@/components/ui/input';
import {getChatDisplayTitle} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {type AiHubChatActionsI} from '@/ee/pages/automation/ai-hub/chats/hooks/useAiHubChatActions';
import {useEffect, useState} from 'react';

type AiHubChatActionDialogsPropsType = Pick<
    AiHubChatActionsI,
    'cancelDelete' | 'cancelRename' | 'confirmDelete' | 'deleteTarget' | 'renameTarget' | 'submitRename'
>;

/**
 * The confirm step for the two chat actions that need one. Rendered by whichever surface owns a useAiHubChatActions
 * instance (the chats sidebar and the chat header each render their own).
 */
const AiHubChatActionDialogs = ({
    cancelDelete,
    cancelRename,
    confirmDelete,
    deleteTarget,
    renameTarget,
    submitRename,
}: AiHubChatActionDialogsPropsType) => {
    const [titleValue, setTitleValue] = useState('');

    useEffect(() => {
        setTitleValue(renameTarget?.title ?? '');
    }, [renameTarget]);

    return (
        <>
            <Dialog onOpenChange={(nextOpen) => !nextOpen && cancelRename()} open={renameTarget !== null}>
                <DialogContent>
                    <DialogMain>
                        <DialogHeader title="Rename chat" />

                        <DialogBody>
                            <Input
                                autoFocus
                                onChange={(event) => setTitleValue(event.target.value)}
                                onKeyDown={(event) => {
                                    if (event.key === 'Enter') {
                                        submitRename(titleValue);
                                    }
                                }}
                                value={titleValue}
                            />
                        </DialogBody>

                        <DialogFooter>
                            <DialogCancelButton />

                            <Button label="Rename" onClick={() => submitRename(titleValue)} />
                        </DialogFooter>
                    </DialogMain>
                </DialogContent>
            </Dialog>

            <AlertDialog
                description={
                    deleteTarget &&
                    `"${getChatDisplayTitle(deleteTarget)}" will be permanently deleted. This cannot be undone.`
                }
                onCancel={cancelDelete}
                onConfirm={confirmDelete}
                open={deleteTarget !== null}
                title="Delete this chat?"
            />
        </>
    );
};

export default AiHubChatActionDialogs;
