import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import MoveConnectedUsersSkippedList, {
    type SkippedUserDisplayI,
} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersSkippedList';
import {
    MoveImpactWorkflowI,
    MoveWorkflowInputValuesType,
} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/useMoveWorkflowInputs';
import WorkflowConfigurationCard from '@/shared/components/workflow-configuration/WorkflowConfigurationCard';
import {InfoIcon} from 'lucide-react';

import type {MoveConnectedUsersSkippedI} from '@/ee/pages/embedded/connected-users/components/move-connected-users-dialog/MoveConnectedUsersDialog';

interface MoveConnectedUsersImpactProps {
    impact?: MoveImpactWorkflowI[];
    inputValues?: MoveWorkflowInputValuesType;
    mcpServerCount?: number;
    onInputChange?: (workflowUuid: string, inputName: string, value: string) => void;
    previewError?: boolean;
    previewSkipped?: MoveConnectedUsersSkippedI[];
    toSkippedUser: (skippedUser: MoveConnectedUsersSkippedI) => SkippedUserDisplayI;
}

const AWAITS_CONFIGURATION_LABEL = 'Stays disabled until the configuration sets its internal-only inputs';

const getAffectedLabel = (impactItem: MoveImpactWorkflowI) =>
    `affects ${impactItem.affectedConnectedUserCount} ${impactItem.affectedConnectedUserCount === 1 ? 'user' : 'users'}`;

const MoveConnectedUsersImpactCard = ({
    impactItem,
    inputValues,
    onInputChange,
    workflowUuid,
}: {
    impactItem: MoveImpactWorkflowI;
    inputValues?: MoveWorkflowInputValuesType;
    onInputChange: (workflowUuid: string, inputName: string, value: string) => void;
    workflowUuid: string;
}) => (
    <section
        aria-label={impactItem.label}
        className="flex flex-col gap-3 rounded-lg border border-stroke-neutral-secondary p-3"
    >
        <p className="text-sm">
            <span className="font-semibold">{impactItem.label}</span>

            <span className="text-content-neutral-secondary">{` · ${getAffectedLabel(impactItem)}`}</span>
        </p>

        {impactItem.awaitsConfiguration && (
            <p className="text-sm text-content-neutral-secondary">{AWAITS_CONFIGURATION_LABEL}</p>
        )}

        <WorkflowConfigurationCard
            connectionCount={impactItem.newRequiredConnections.length}
            connectionsContent={
                impactItem.newRequiredConnections.length === 0 ? (
                    <p className="text-sm">No new connections</p>
                ) : (
                    <div className="space-y-1 text-sm">
                        <p>{impactItem.newRequiredConnections.join(', ')}</p>

                        <p className="text-content-neutral-secondary">Each user connects their own.</p>
                    </div>
                )
            }
            defaultTab={impactItem.newRequiredInputs.length > 0 ? 'inputs' : 'connections'}
            enabled
            inputCount={impactItem.newRequiredInputs.length}
            inputsContent={
                impactItem.newRequiredInputs.length === 0 ? (
                    <p className="text-sm">No new inputs</p>
                ) : (
                    <fieldset className="space-y-3 border-0 p-0">
                        {impactItem.newRequiredInputs.map((inputName) => {
                            const inputId = `move-workflow-input-${workflowUuid}-${inputName}`;

                            return (
                                <div className="space-y-1" key={inputName}>
                                    <Label htmlFor={inputId}>
                                        {inputName}

                                        <span className="text-content-destructive-primary">*</span>
                                    </Label>

                                    <Input
                                        id={inputId}
                                        onChange={(event) => onInputChange(workflowUuid, inputName, event.target.value)}
                                        value={inputValues?.[workflowUuid]?.[inputName] || ''}
                                    />
                                </div>
                            );
                        })}
                    </fieldset>
                )
            }
            onEnabledChange={() => undefined}
            showWorkflowToggle={false}
            switchId={`move-workflow-${workflowUuid}`}
        />
    </section>
);

const MoveConnectedUsersImpact = ({
    impact,
    inputValues,
    mcpServerCount,
    onInputChange,
    previewError,
    previewSkipped,
    toSkippedUser,
}: MoveConnectedUsersImpactProps) => (
    <>
        {impact?.map((impactItem) =>
            onInputChange && impactItem.workflowUuid ? (
                <MoveConnectedUsersImpactCard
                    impactItem={impactItem}
                    inputValues={inputValues}
                    key={impactItem.workflowUuid}
                    onInputChange={onInputChange}
                    workflowUuid={impactItem.workflowUuid}
                />
            ) : (
                <Alert key={impactItem.label} variant="default">
                    <InfoIcon />

                    <AlertTitle>{`${impactItem.label}: ${getAffectedLabel(impactItem)}`}</AlertTitle>

                    {(impactItem.newRequiredConnections.length > 0 ||
                        impactItem.newRequiredInputs.length > 0 ||
                        impactItem.awaitsConfiguration) && (
                        <AlertDescription>
                            {impactItem.newRequiredConnections.length > 0 && (
                                <p>{`New required connections: ${impactItem.newRequiredConnections.join(', ')}`}</p>
                            )}

                            {impactItem.newRequiredInputs.length > 0 && (
                                <p>{`New required inputs: ${impactItem.newRequiredInputs.join(', ')}`}</p>
                            )}

                            {impactItem.awaitsConfiguration && <p>{AWAITS_CONFIGURATION_LABEL}</p>}
                        </AlertDescription>
                    )}
                </Alert>
            )
        )}

        {!!mcpServerCount && (
            <Alert variant="default">
                <InfoIcon />

                <AlertDescription>
                    {`${mcpServerCount} ${mcpServerCount === 1 ? 'MCP server uses' : 'MCP servers use'} one of these configurations. Their tools follow the configuration each user lands on.`}
                </AlertDescription>
            </Alert>
        )}

        {previewError && (
            <p className="text-sm text-content-destructive-primary" role="alert">
                Could not load the move preview.
            </p>
        )}

        {!!previewSkipped?.length && (
            <div className="space-y-1 text-sm">
                <p className="font-medium">Users that would be skipped</p>

                <MoveConnectedUsersSkippedList skippedUsers={previewSkipped.map(toSkippedUser)} />
            </div>
        )}
    </>
);

export default MoveConnectedUsersImpact;
