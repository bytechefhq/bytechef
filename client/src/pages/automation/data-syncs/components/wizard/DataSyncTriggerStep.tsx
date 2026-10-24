import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Label} from '@/components/ui/label';
import {RadioGroup, RadioGroupItem} from '@/components/ui/radio-group';
import AgentScheduleFrequencyFields from '@/pages/automation/agents/components/detail/AgentScheduleFrequencyFields';
import {
    AgentScheduleCadenceI,
    SCHEDULE_FREQUENCY_KINDS,
    SCHEDULE_FREQUENCY_LABELS,
    ScheduleFrequencyKindType,
    fromCadenceParameters,
    toCadenceParameters,
    toCronExpression,
    validateAgentScheduleCadence,
} from '@/pages/automation/agents/utils/agentScheduleCron';
import useDebouncedSave from '@/pages/automation/data-syncs/hooks/useDebouncedSave';
import invalidateDataSyncQueries from '@/pages/automation/data-syncs/utils/invalidateDataSyncQueries';
import {DataSync, DataSyncTriggerType, useUpdateDataSyncTriggerMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo, useRef, useState} from 'react';

interface DataSyncTriggerStepProps {
    dataSync: Pick<DataSync, 'id' | 'triggerParameters' | 'triggerType'>;
}

const DEFAULT_CADENCE: AgentScheduleCadenceI = {frequencyKind: 'DAILY', timeOfDay: '09:00'};

export default function DataSyncTriggerStep({dataSync}: DataSyncTriggerStepProps) {
    const storedParameters = (dataSync.triggerParameters ?? {}) as Record<string, unknown>;

    const [cadence, setCadence] = useState<AgentScheduleCadenceI>(
        dataSync.triggerType === DataSyncTriggerType.Schedule
            ? fromCadenceParameters(storedParameters)
            : DEFAULT_CADENCE
    );
    const [timezone, setTimezone] = useState<string>(String(storedParameters.timezone ?? 'UTC'));
    const [triggerType, setTriggerType] = useState<DataSyncTriggerType>(dataSync.triggerType);

    const lastImmediateWriteRef = useRef<string>('');

    const queryClient = useQueryClient();

    const updateTriggerMutation = useUpdateDataSyncTriggerMutation({
        onSuccess: () => invalidateDataSyncQueries(queryClient),
    });

    const timezones = useMemo(() => Intl.supportedValuesOf('timeZone'), []);

    const cadenceErrors = useMemo(() => validateAgentScheduleCadence(cadence), [cadence]);

    const candidateScheduleToSave = useMemo(() => {
        if (triggerType !== DataSyncTriggerType.Schedule || Object.keys(cadenceErrors).length > 0) {
            return null;
        }

        return {...toCadenceParameters(cadence), expression: toCronExpression(cadence), timezone};
    }, [cadence, cadenceErrors, timezone, triggerType]);

    const scheduleToSave =
        candidateScheduleToSave && JSON.stringify(candidateScheduleToSave) === lastImmediateWriteRef.current
            ? null
            : candidateScheduleToSave;

    useDebouncedSave(scheduleToSave, (triggerParameters) => {
        if (triggerParameters) {
            updateTriggerMutation.mutate({
                input: {id: dataSync.id, triggerParameters, triggerType: DataSyncTriggerType.Schedule},
            });
        }
    });

    const handleTriggerTypeChange = (value: string) => {
        const nextTriggerType = value as DataSyncTriggerType;

        setTriggerType(nextTriggerType);

        if (nextTriggerType === DataSyncTriggerType.Manual) {
            updateTriggerMutation.mutate({
                input: {id: dataSync.id, triggerParameters: null, triggerType: DataSyncTriggerType.Manual},
            });

            return;
        }

        if (Object.keys(cadenceErrors).length > 0) {
            return;
        }

        const scheduleParameters = {...toCadenceParameters(cadence), expression: toCronExpression(cadence), timezone};

        lastImmediateWriteRef.current = JSON.stringify(scheduleParameters);

        updateTriggerMutation.mutate({
            input: {id: dataSync.id, triggerParameters: scheduleParameters, triggerType: DataSyncTriggerType.Schedule},
        });
    };

    return (
        <div className="space-y-6 py-4">
            <div>
                <h2 className="text-lg font-semibold">Trigger</h2>

                <p className="mt-1 text-sm text-muted-foreground">
                    Run the sync on demand from its deployment, or on a schedule.
                </p>
            </div>

            <RadioGroup onValueChange={handleTriggerTypeChange} value={triggerType}>
                <div className="flex items-center gap-2">
                    <RadioGroupItem id="data-sync-trigger-manual" value={DataSyncTriggerType.Manual} />

                    <Label htmlFor="data-sync-trigger-manual">Manual</Label>
                </div>

                <div className="flex items-center gap-2">
                    <RadioGroupItem id="data-sync-trigger-schedule" value={DataSyncTriggerType.Schedule} />

                    <Label htmlFor="data-sync-trigger-schedule">Scheduled</Label>
                </div>
            </RadioGroup>

            {triggerType === DataSyncTriggerType.Schedule && (
                <fieldset className="space-y-4 border-0 p-0">
                    <div className="flex flex-col gap-2">
                        <Label htmlFor="data-sync-frequency">Frequency</Label>

                        <Select
                            onValueChange={(value) =>
                                setCadence({frequencyKind: value as ScheduleFrequencyKindType, timeOfDay: '09:00'})
                            }
                            value={cadence.frequencyKind}
                        >
                            <SelectTrigger id="data-sync-frequency">
                                <SelectValue />
                            </SelectTrigger>

                            <SelectContent>
                                {SCHEDULE_FREQUENCY_KINDS.map((kind) => (
                                    <SelectItem key={kind} value={kind}>
                                        {SCHEDULE_FREQUENCY_LABELS[kind]}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    <AgentScheduleFrequencyFields cadence={cadence} errors={cadenceErrors} onChange={setCadence} />

                    <div className="flex flex-col gap-2">
                        <Label htmlFor="data-sync-timezone">Timezone</Label>

                        <Select onValueChange={setTimezone} value={timezone}>
                            <SelectTrigger id="data-sync-timezone">
                                <SelectValue placeholder="Choose a timezone…" />
                            </SelectTrigger>

                            <SelectContent>
                                {timezones.map((zone) => (
                                    <SelectItem key={zone} value={zone}>
                                        {zone}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                </fieldset>
            )}
        </div>
    );
}
