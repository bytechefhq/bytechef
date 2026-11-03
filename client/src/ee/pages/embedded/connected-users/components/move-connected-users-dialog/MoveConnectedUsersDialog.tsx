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
import {useMemo, useState} from 'react';

const TARGET_LACKS_REFERENCED_WORKFLOWS = 'TARGET_LACKS_REFERENCED_WORKFLOWS';

interface MoveConnectedUsersSkippedI {
    label: string;
    reason: string;
    workflowLabels: string[];
}

export interface MoveConnectedUsersResultI {
    movedCount: number;
    needsAttentionCount: number;
    skipped: MoveConnectedUsersSkippedI[];
}

interface MoveConnectedUsersDialogProps {
    connectedUsers: Array<{connectedUserId: string; email?: string | null; externalId: string; name?: string | null}>;
    impact?: Array<{
        affectedConnectedUserCount: number;
        label: string;
        newRequiredConnections: string[];
        newRequiredInputs: string[];
    }>;
    initialSelectedConnectedUserIds?: string[];
    onClose: () => void;
    onMove: (targetId: string, connectedUserIds: string[]) => Promise<MoveConnectedUsersResultI>;
    onSelectionChange?: (targetId: string | undefined, connectedUserIds: string[]) => void;
    previewSkipped?: MoveConnectedUsersSkippedI[];
    sourceLabel: string;
    targets: Array<{id: string; label: string}>;
    title: string;
}

const formatSkippedLine = ({label, reason, workflowLabels}: MoveConnectedUsersSkippedI): string =>
    reason === TARGET_LACKS_REFERENCED_WORKFLOWS
        ? `${label}: ${workflowLabels.join(', ')} missing in the target version`
        : `${label}: ${reason}`;

const MoveConnectedUsersDialog = ({
    connectedUsers,
    impact,
    initialSelectedConnectedUserIds,
    onClose,
    onMove,
    onSelectionChange,
    previewSkipped,
    sourceLabel,
    targets,
    title,
}: MoveConnectedUsersDialogProps) => {
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

    const canMove = !!targetId && selectedConnectedUserIds.length > 0 && !moving;

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
                    <div className="flex flex-col gap-2">
                        <p className="text-sm font-medium">
                            {`${result.movedCount} moved, ${result.needsAttentionCount} need attention`}
                        </p>

                        {result.skipped.map((skippedUser, index) => (
                            <p className="text-sm text-content-neutral-secondary" key={`${skippedUser.label}-${index}`}>
                                {formatSkippedLine(skippedUser)}
                            </p>
                        ))}
                    </div>
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

                        {impact?.map((impactItem) => (
                            <div
                                className="rounded border border-border/50 px-3 py-2 text-sm"
                                key={impactItem.label}
                                role="alert"
                            >
                                <p className="font-medium">
                                    {`${impactItem.label}: affects ${impactItem.affectedConnectedUserCount} ${impactItem.affectedConnectedUserCount === 1 ? 'user' : 'users'}`}
                                </p>

                                {impactItem.newRequiredConnections.length > 0 && (
                                    <p>{`New required connections: ${impactItem.newRequiredConnections.join(', ')}`}</p>
                                )}

                                {impactItem.newRequiredInputs.length > 0 && (
                                    <p>{`New required inputs: ${impactItem.newRequiredInputs.join(', ')}`}</p>
                                )}
                            </div>
                        ))}

                        {!!previewSkipped?.length && (
                            <div className="space-y-1 text-sm text-content-neutral-secondary">
                                <p className="font-medium">Users that would be skipped</p>

                                {previewSkipped.map((skippedUser, index) => (
                                    <p key={`${skippedUser.label}-${index}`}>{formatSkippedLine(skippedUser)}</p>
                                ))}
                            </div>
                        )}

                        <Input
                            aria-label="Search users"
                            onChange={(event) => setSearch(event.target.value)}
                            placeholder="Search users"
                            value={search}
                        />

                        <ul className="flex max-h-64 flex-col gap-2 overflow-y-auto">
                            {filteredConnectedUsers.map((connectedUser) => {
                                const label = connectedUser.name || connectedUser.email || connectedUser.externalId;

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

export default MoveConnectedUsersDialog;
