import Button from '@/components/Button/Button';
import {Alert, AlertDescription} from '@/components/ui/alert';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import MoveConnectedUsersImpact from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersImpact';
import MoveConnectedUsersResult from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersResult';
import {formatSkippedReason} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/moveConnectedUsersReasons';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';
import {AlertTriangleIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

import type {
    MoveConnectedUsersResultI,
    MoveConnectedUsersSkippedI,
} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersDialog';
import type {SkippedUserDisplayI} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersSkippedList';

const UNMOVABLE_SKIP_REASONS = ['ALREADY_ON_TARGET', 'NOT_ON_SOURCE'];

export interface UpgradeOptionI {
    badge?: string;
    id: string;
    label: string;
}

export interface UpgradeImpactI {
    mcpServerCount?: number;
    skipped: MoveConnectedUsersSkippedI[];
    workflows: Array<{
        affectedConnectedUserCount: number;
        label: string;
        newRequiredConnections: string[];
        newRequiredInputs: string[];
    }>;
}

interface UpgradeConnectedUsersDialogViewProps {
    connectedUsers: ConnectedUser[];
    emptyParentMessage: string;
    impact?: UpgradeImpactI;
    notOnSourceReason: string;
    onClose: () => void;
    onParentChange: (parentId: string) => void;
    onTargetChange: (targetId: string) => void;
    onUpgrade: () => Promise<MoveConnectedUsersResultI>;
    onUpgraded: () => void;
    parentId?: string;
    parentLabel: string;
    parentOptions: UpgradeOptionI[];
    previewError?: boolean;
    previewLoading?: boolean;
    targetId?: string;
    targetOptions: UpgradeOptionI[];
    title: string;
}

const UpgradeConnectedUsersDialogView = ({
    connectedUsers,
    emptyParentMessage,
    impact,
    notOnSourceReason,
    onClose,
    onParentChange,
    onTargetChange,
    onUpgrade,
    onUpgraded,
    parentId,
    parentLabel,
    parentOptions,
    previewError,
    previewLoading,
    targetId,
    targetOptions,
    title,
}: UpgradeConnectedUsersDialogViewProps) => {
    const [result, setResult] = useState<MoveConnectedUsersResultI>();
    const [upgrading, setUpgrading] = useState(false);

    const labelByConnectedUserId = useMemo(
        () =>
            new Map(
                connectedUsers.map((connectedUser) => [
                    String(connectedUser.id),
                    connectedUser.name || connectedUser.email || connectedUser.externalId,
                ])
            ),
        [connectedUsers]
    );

    const noConnectedUserWouldMove = useMemo(() => {
        if (!impact) {
            return false;
        }

        const unmovableConnectedUserIds = new Set(
            impact.skipped
                .filter((skippedUser) => UNMOVABLE_SKIP_REASONS.includes(skippedUser.reason))
                .map((skippedUser) => String(skippedUser.connectedUserId))
        );

        return connectedUsers.every((connectedUser) => unmovableConnectedUserIds.has(String(connectedUser.id)));
    }, [connectedUsers, impact]);

    const canUpgrade =
        !!targetId && !!impact && !noConnectedUserWouldMove && !previewLoading && !previewError && !upgrading;

    const toSkippedUser = (skippedUser: MoveConnectedUsersSkippedI): SkippedUserDisplayI => {
        const connectedUserId = String(skippedUser.connectedUserId);

        return {
            label: labelByConnectedUserId.get(connectedUserId) || connectedUserId,
            reason: formatSkippedReason(skippedUser, notOnSourceReason),
            workflowLabels: skippedUser.workflowLabels,
        };
    };

    const handleClose = () => {
        if (upgrading) {
            return;
        }

        if (result) {
            onUpgraded();
        }

        onClose();
    };

    const handleUpgrade = async () => {
        setUpgrading(true);

        try {
            setResult(await onUpgrade());
        } catch {
            setResult(undefined);
        } finally {
            setUpgrading(false);
        }
    };

    return (
        <Dialog onOpenChange={(open) => !open && handleClose()} open>
            <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-xl" showCloseButton>
                <DialogHeader>
                    <DialogTitle>{title}</DialogTitle>

                    <DialogDescription>
                        {`Upgrading ${connectedUsers.length} ${connectedUsers.length === 1 ? 'user' : 'users'}`}
                    </DialogDescription>
                </DialogHeader>

                {result ? (
                    <MoveConnectedUsersResult result={result} toSkippedUser={toSkippedUser} />
                ) : (
                    <div className="flex flex-col gap-4">
                        <fieldset className="space-y-2 border-0 p-0">
                            <Label htmlFor="upgrade-connected-users-parent">{parentLabel}</Label>

                            <Select onValueChange={onParentChange} value={parentId || ''}>
                                <SelectTrigger aria-label={parentLabel} id="upgrade-connected-users-parent">
                                    <SelectValue placeholder="Select..." />
                                </SelectTrigger>

                                <SelectContent>
                                    {parentOptions.map((option) => (
                                        <SelectItem key={option.id} value={option.id}>
                                            {option.label}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>

                            {parentOptions.length === 0 && (
                                <p className="text-sm text-content-neutral-secondary">{emptyParentMessage}</p>
                            )}
                        </fieldset>

                        <fieldset className="space-y-2 border-0 p-0">
                            <Label htmlFor="upgrade-connected-users-target">Configuration</Label>

                            <Select disabled={!parentId} onValueChange={onTargetChange} value={targetId || ''}>
                                <SelectTrigger aria-label="Configuration" id="upgrade-connected-users-target">
                                    <SelectValue placeholder="Select..." />
                                </SelectTrigger>

                                <SelectContent>
                                    {targetOptions.map((option) => (
                                        <SelectItem key={option.id} value={option.id}>
                                            <span>{option.label}</span>

                                            {option.badge && (
                                                <span className="ml-2 text-xs text-content-neutral-secondary">
                                                    {option.badge}
                                                </span>
                                            )}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </fieldset>

                        {noConnectedUserWouldMove && (
                            <Alert variant="warning">
                                <AlertTriangleIcon />

                                <AlertDescription>
                                    None of the selected users would move to this configuration.
                                </AlertDescription>
                            </Alert>
                        )}

                        <MoveConnectedUsersImpact
                            impact={impact?.workflows}
                            mcpServerCount={impact?.mcpServerCount}
                            previewError={previewError}
                            previewSkipped={impact?.skipped}
                            toSkippedUser={toSkippedUser}
                        />
                    </div>
                )}

                <DialogFooter>
                    {result ? (
                        <Button label="Close" onClick={handleClose} />
                    ) : (
                        <>
                            <Button disabled={upgrading} label="Cancel" onClick={handleClose} variant="outline" />

                            <Button
                                disabled={!canUpgrade}
                                label={upgrading ? 'Upgrading...' : 'Upgrade'}
                                onClick={() => void handleUpgrade()}
                            />
                        </>
                    )}
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default UpgradeConnectedUsersDialogView;
