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
import MoveConnectedUsersSkippedList from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersSkippedList';
import {formatSkippedReason} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/moveConnectedUsersReasons';
import useMoveWorkflowInputs, {
    MoveImpactWorkflowI,
    MoveWorkflowInputsI,
} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/useMoveWorkflowInputs';
import {InfoIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

import type {
    MoveConnectedUsersResultI,
    MoveConnectedUsersSkippedI,
} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersDialog';
import type {SkippedUserDisplayI} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersSkippedList';

export const NO_OTHER_VERSION_REASON = 'No other published version';

export const VERSIONS_LOAD_FAILED_REASON = "Couldn't load the versions. Try again later.";

export interface ConnectedUserChangeVersionConfigurationI {
    enabled: boolean;
    id: string;
    name: string;
    version: number;
}

interface ConnectedUserChangeVersionDialogProps {
    configurations: ConnectedUserChangeVersionConfigurationI[];
    connectedUserLabel: string;
    currentConfigurationName: string;
    currentVersion: number;
    editableInputs?: boolean;
    impact?: MoveImpactWorkflowI[];
    mcpServerCount?: number;
    onChange: (
        version: number,
        targetId: string | undefined,
        workflowInputs: MoveWorkflowInputsI[]
    ) => Promise<MoveConnectedUsersResultI>;
    onClose: () => void;
    onTargetChange?: (targetId: string | undefined) => void;
    pending?: boolean;
    previewError?: boolean;
    previewLoading?: boolean;
    previewSkipped?: MoveConnectedUsersSkippedI[];
    title?: string;
    versions: number[];
}

export const getOtherVersions = (versions: number[], currentVersion: number): number[] =>
    [...new Set(versions)].filter((version) => version !== currentVersion).sort((first, second) => second - first);

const formatVersionLabel = (version: number, latestVersion: number, currentVersion: number): string => {
    let label = `V${version}`;

    if (version === latestVersion) {
        label += ' (latest)';
    }

    if (version < currentVersion) {
        label += ' (downgrade)';
    }

    return label;
};

const ConnectedUserChangeVersionDialog = ({
    configurations,
    connectedUserLabel,
    currentConfigurationName,
    currentVersion,
    editableInputs = false,
    impact,
    mcpServerCount,
    onChange,
    onClose,
    onTargetChange,
    pending,
    previewError,
    previewLoading,
    previewSkipped,
    title = 'Change version',
    versions,
}: ConnectedUserChangeVersionDialogProps) => {
    const [changing, setChanging] = useState(false);
    const [movedResult, setMovedResult] = useState<MoveConnectedUsersResultI>();
    const [notMovedResult, setNotMovedResult] = useState<MoveConnectedUsersResultI>();
    const [selectedTargetId, setSelectedTargetId] = useState<string>();
    const [selectedVersion, setSelectedVersion] = useState<number>();

    const {inputValues, missingRequiredInput, setInputValue, workflowInputs} = useMoveWorkflowInputs(impact);

    const otherVersions = useMemo(() => getOtherVersions(versions, currentVersion), [currentVersion, versions]);

    const latestVersion = useMemo(() => Math.max(...versions), [versions]);

    const matchingConfigurations = useMemo(
        () =>
            selectedVersion === undefined
                ? []
                : configurations.filter(
                      (configuration) => configuration.enabled && configuration.version === selectedVersion
                  ),
        [configurations, selectedVersion]
    );

    const selectedTarget = matchingConfigurations.find((configuration) => configuration.id === selectedTargetId);

    const toSkippedUser = (skippedUser: MoveConnectedUsersSkippedI): SkippedUserDisplayI => ({
        label: connectedUserLabel,
        reason: formatSkippedReason(skippedUser),
        workflowLabels: skippedUser.workflowLabels,
    });

    const handleTargetChange = (targetId: string | undefined) => {
        setNotMovedResult(undefined);
        setSelectedTargetId(targetId);

        onTargetChange?.(targetId);
    };

    const handleVersionChange = (value: string) => {
        const version = Number(value);

        const configurationsOnVersion = configurations.filter(
            (configuration) => configuration.enabled && configuration.version === version
        );

        const preselectedConfiguration =
            configurationsOnVersion.find((configuration) => configuration.name === currentConfigurationName) ??
            configurationsOnVersion[0];

        setSelectedVersion(version);

        handleTargetChange(preselectedConfiguration?.id);
    };

    const handleChange = async () => {
        if (selectedVersion === undefined) {
            return;
        }

        setChanging(true);

        let result: MoveConnectedUsersResultI;

        try {
            result = await onChange(
                selectedVersion,
                selectedTarget?.id,
                editableInputs && selectedTarget ? workflowInputs : []
            );
        } catch {
            setChanging(false);

            return;
        }

        setChanging(false);

        if (result.movedConnectedUserIds.length === 0) {
            setNotMovedResult(result);
        } else if (result.needsAttentionConnectedUserIds.length > 0) {
            setMovedResult(result);
        } else {
            onClose();
        }
    };

    const canChange =
        selectedVersion !== undefined &&
        (matchingConfigurations.length === 0 || !!selectedTarget) &&
        !changing &&
        !pending &&
        !(selectedTarget && previewLoading) &&
        !(editableInputs && selectedTarget && missingRequiredInput);

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open>
            <DialogContent className="sm:max-w-lg" showCloseButton>
                <DialogHeader>
                    <DialogTitle>{title}</DialogTitle>

                    <DialogDescription>{`Currently on ${currentConfigurationName} · V${currentVersion}`}</DialogDescription>
                </DialogHeader>

                {movedResult ? (
                    <div className="flex flex-col gap-2">
                        <p className="text-sm text-content-neutral-secondary">
                            {`${connectedUserLabel} moved to V${selectedVersion}, but some turned-on workflows need attention.`}
                        </p>

                        <MoveConnectedUsersResult result={movedResult} toSkippedUser={toSkippedUser} />
                    </div>
                ) : otherVersions.length === 0 ? (
                    <p className="text-sm text-content-neutral-secondary">{NO_OTHER_VERSION_REASON}</p>
                ) : (
                    <div className="flex flex-col gap-4">
                        <fieldset className="space-y-2 border-0 p-0">
                            <Label htmlFor="connected-user-change-version-version">Version</Label>

                            <Select
                                onValueChange={handleVersionChange}
                                value={selectedVersion === undefined ? undefined : String(selectedVersion)}
                            >
                                <SelectTrigger aria-label="Version" id="connected-user-change-version-version">
                                    <SelectValue placeholder="Select..." />
                                </SelectTrigger>

                                <SelectContent>
                                    {otherVersions.map((version) => (
                                        <SelectItem key={version} value={String(version)}>
                                            {formatVersionLabel(version, latestVersion, currentVersion)}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </fieldset>

                        {selectedVersion !== undefined && matchingConfigurations.length > 0 && (
                            <fieldset className="space-y-2 border-0 p-0">
                                <Label htmlFor="connected-user-change-version-configuration">Configuration</Label>

                                <Select onValueChange={handleTargetChange} value={selectedTargetId}>
                                    <SelectTrigger
                                        aria-label="Configuration"
                                        id="connected-user-change-version-configuration"
                                    >
                                        <SelectValue placeholder="Select..." />
                                    </SelectTrigger>

                                    <SelectContent>
                                        {matchingConfigurations.map((configuration) => (
                                            <SelectItem key={configuration.id} value={configuration.id}>
                                                {`${configuration.name} · V${configuration.version}`}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>

                                {selectedTarget && (
                                    <p className="text-sm text-content-neutral-secondary">
                                        {`Moves this connected user to ${selectedTarget.name} · V${selectedVersion}.`}
                                    </p>
                                )}
                            </fieldset>
                        )}

                        {selectedTarget && (
                            <MoveConnectedUsersImpact
                                impact={impact}
                                inputValues={inputValues}
                                mcpServerCount={mcpServerCount}
                                onInputChange={editableInputs ? setInputValue : undefined}
                                previewError={previewError}
                                previewSkipped={previewSkipped}
                                toSkippedUser={toSkippedUser}
                            />
                        )}

                        {selectedVersion !== undefined && matchingConfigurations.length === 0 && (
                            <Alert variant="default">
                                <InfoIcon />

                                <AlertDescription>
                                    {`Creates ${currentConfigurationName} · V${selectedVersion} from ${currentConfigurationName}, then moves this connected user to it. New connected users keep landing on ${currentConfigurationName}.`}
                                </AlertDescription>
                            </Alert>
                        )}

                        {notMovedResult && (
                            <div className="space-y-1 text-sm" role="alert">
                                <p className="font-medium">{`${connectedUserLabel} was not moved`}</p>

                                <MoveConnectedUsersSkippedList
                                    skippedUsers={notMovedResult.skipped.map(toSkippedUser)}
                                />
                            </div>
                        )}
                    </div>
                )}

                <DialogFooter>
                    {movedResult ? (
                        <Button label="Close" onClick={onClose} />
                    ) : (
                        <>
                            <Button label="Cancel" onClick={onClose} variant="outline" />

                            <Button
                                disabled={!canChange}
                                label={changing || pending ? 'Changing...' : 'Change'}
                                onClick={() => void handleChange()}
                            />
                        </>
                    )}
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default ConnectedUserChangeVersionDialog;
