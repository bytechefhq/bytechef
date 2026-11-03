import Button from '@/components/Button/Button';
import {Checkbox} from '@/components/ui/checkbox';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import MoveConnectedUsersImpact from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersImpact';
import MoveConnectedUsersResult from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersResult';
import {formatSkippedLine} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/moveConnectedUsersReasons';
import {useMemo, useState} from 'react';

type ConnectedUserIdType = number | string;

interface MoveConnectedUsersDialogConnectedUserI {
    connectedUserId: string;
    email?: string | null;
    externalId: string;
    name?: string | null;
}

export interface MoveConnectedUsersSkippedI {
    connectedUserId: ConnectedUserIdType;
    reason: string;
    workflowLabels: string[];
}

export interface MoveConnectedUsersResultI {
    movedConnectedUserIds: ConnectedUserIdType[];
    needsAttentionConnectedUserIds: ConnectedUserIdType[];
    skipped: MoveConnectedUsersSkippedI[];
}

interface MoveConnectedUsersDialogProps {
    connectedUsers?: MoveConnectedUsersDialogConnectedUserI[];
    connectedUsersError?: boolean;
    impact?: Array<{
        affectedConnectedUserCount: number;
        label: string;
        newRequiredConnections: string[];
        newRequiredInputs: string[];
    }>;
    initialSelectedConnectedUserIds?: string[];
    mcpServerCount?: number;
    onClose: () => void;
    onMove: (targetId: string, connectedUserIds: string[]) => Promise<MoveConnectedUsersResultI>;
    onSelectionChange?: (targetId: string | undefined, connectedUserIds: string[]) => void;
    previewError?: boolean;
    previewLoading?: boolean;
    previewSkipped?: MoveConnectedUsersSkippedI[];
    sourceLabel: string;
    targets: Array<{id: string; label: string}>;
    title: string;
}

type MoveConnectedUsersDialogContentPropsType = Omit<
    MoveConnectedUsersDialogProps,
    'connectedUsers' | 'connectedUsersError'
> & {
    connectedUsers: MoveConnectedUsersDialogConnectedUserI[];
};

const getConnectedUserLabel = (connectedUser: MoveConnectedUsersDialogConnectedUserI): string =>
    connectedUser.name || connectedUser.email || connectedUser.externalId;

const MoveConnectedUsersDialogContent = ({
    connectedUsers,
    impact,
    initialSelectedConnectedUserIds,
    mcpServerCount,
    onClose,
    onMove,
    onSelectionChange,
    previewError,
    previewLoading,
    previewSkipped,
    sourceLabel,
    targets,
    title,
}: MoveConnectedUsersDialogContentPropsType) => {
    const [moving, setMoving] = useState(false);
    const [result, setResult] = useState<MoveConnectedUsersResultI>();
    const [search, setSearch] = useState('');
    const [selectedConnectedUserIds, setSelectedConnectedUserIds] = useState<string[]>(
        () => initialSelectedConnectedUserIds || connectedUsers.map((connectedUser) => connectedUser.connectedUserId)
    );
    const [targetId, setTargetId] = useState<string>();

    const filteredConnectedUsers = useMemo(() => {
        const term = search.trim().toLowerCase();

        if (!term) {
            return connectedUsers;
        }

        return connectedUsers.filter((connectedUser) =>
            [connectedUser.name, connectedUser.email, connectedUser.externalId].some((value) =>
                value?.toLowerCase().includes(term)
            )
        );
    }, [connectedUsers, search]);

    const labelByConnectedUserId = useMemo(
        () =>
            new Map(
                connectedUsers.map((connectedUser) => [
                    connectedUser.connectedUserId,
                    getConnectedUserLabel(connectedUser),
                ])
            ),
        [connectedUsers]
    );

    const toSkippedLine = (skippedUser: MoveConnectedUsersSkippedI): string => {
        const connectedUserId = String(skippedUser.connectedUserId);

        return formatSkippedLine(labelByConnectedUserId.get(connectedUserId) || connectedUserId, skippedUser);
    };

    const canMove = !!targetId && selectedConnectedUserIds.length > 0 && !moving && !previewLoading;

    const handleTargetChange = (value: string) => {
        setTargetId(value);

        onSelectionChange?.(value, selectedConnectedUserIds);
    };

    const handleToggle = (connectedUserId: string, checked: boolean) => {
        const nextSelectedConnectedUserIds = checked
            ? [...selectedConnectedUserIds, connectedUserId]
            : selectedConnectedUserIds.filter((selectedId) => selectedId !== connectedUserId);

        setSelectedConnectedUserIds(nextSelectedConnectedUserIds);

        onSelectionChange?.(targetId, nextSelectedConnectedUserIds);
    };

    const handleMove = async () => {
        if (!targetId) {
            return;
        }

        setMoving(true);

        try {
            setResult(await onMove(targetId, selectedConnectedUserIds));
        } catch {
            setResult(undefined);
        } finally {
            setMoving(false);
        }
    };

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open>
            <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-xl" showCloseButton>
                <DialogHeader>
                    <DialogTitle>{title}</DialogTitle>

                    <DialogDescription>{`Moving users from ${sourceLabel}`}</DialogDescription>
                </DialogHeader>

                {result ? (
                    <MoveConnectedUsersResult result={result} toSkippedLine={toSkippedLine} />
                ) : (
                    <div className="flex flex-col gap-4">
                        <fieldset className="space-y-2 border-0 p-0">
                            <Label htmlFor="move-connected-users-target">Target</Label>

                            <Select onValueChange={handleTargetChange} value={targetId}>
                                <SelectTrigger aria-label="Target" id="move-connected-users-target">
                                    <SelectValue placeholder="Select..." />
                                </SelectTrigger>

                                <SelectContent>
                                    {targets.map((target) => (
                                        <SelectItem key={target.id} value={target.id}>
                                            {target.label}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </fieldset>

                        <MoveConnectedUsersImpact
                            impact={impact}
                            mcpServerCount={mcpServerCount}
                            previewError={previewError}
                            previewSkipped={previewSkipped}
                            toSkippedLine={toSkippedLine}
                        />

                        <Input
                            aria-label="Search users"
                            onChange={(event) => setSearch(event.target.value)}
                            placeholder="Search users"
                            value={search}
                        />

                        <ul className="flex max-h-64 flex-col gap-2 overflow-y-auto">
                            {filteredConnectedUsers.map((connectedUser) => {
                                const label = getConnectedUserLabel(connectedUser);

                                return (
                                    <li className="flex items-center gap-2" key={connectedUser.connectedUserId}>
                                        <Checkbox
                                            aria-label={label}
                                            checked={selectedConnectedUserIds.includes(connectedUser.connectedUserId)}
                                            onCheckedChange={(checked) =>
                                                handleToggle(connectedUser.connectedUserId, checked === true)
                                            }
                                        />

                                        <span className="text-sm">{label}</span>

                                        {connectedUser.email && connectedUser.email !== label && (
                                            <span className="text-xs text-content-neutral-secondary">
                                                {connectedUser.email}
                                            </span>
                                        )}
                                    </li>
                                );
                            })}
                        </ul>
                    </div>
                )}

                <DialogFooter>
                    {result ? (
                        <Button label="Close" onClick={onClose} />
                    ) : (
                        <>
                            <Button label="Cancel" onClick={onClose} variant="outline" />

                            <Button
                                disabled={!canMove}
                                label={moving ? 'Moving...' : 'Move'}
                                onClick={() => void handleMove()}
                            />
                        </>
                    )}
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

const MoveConnectedUsersDialog = ({connectedUsers, connectedUsersError, ...props}: MoveConnectedUsersDialogProps) => {
    if (!connectedUsers) {
        return (
            <Dialog onOpenChange={(open) => !open && props.onClose()} open>
                <DialogContent showCloseButton>
                    <DialogHeader>
                        <DialogTitle>{props.title}</DialogTitle>

                        <DialogDescription>
                            {connectedUsersError ? 'Could not load the connected users.' : 'Loading users...'}
                        </DialogDescription>
                    </DialogHeader>
                </DialogContent>
            </Dialog>
        );
    }

    return <MoveConnectedUsersDialogContent connectedUsers={connectedUsers} {...props} />;
};

export default MoveConnectedUsersDialog;
