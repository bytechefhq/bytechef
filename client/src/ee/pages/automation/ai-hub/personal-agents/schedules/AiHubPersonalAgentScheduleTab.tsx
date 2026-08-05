import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import Switch from '@/components/Switch/Switch';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {
    AiHubPersonalAgentScheduleFieldsFragment,
    ScheduleFrequencyKind,
    ScheduleLifecycleKind,
} from '@/shared/middleware/graphql';
import {useState} from 'react';

import AiHubPersonalAgentScheduleFrequencyFields, {
    ScheduleFrequencyFieldsValueI,
} from './AiHubPersonalAgentScheduleFrequencyFields';

const COMMON_TIMEZONES = [
    'UTC',
    'America/New_York',
    'America/Chicago',
    'America/Denver',
    'America/Los_Angeles',
    'Europe/Zagreb',
    'Europe/London',
    'Europe/Berlin',
    'Asia/Tokyo',
    'Asia/Shanghai',
];

export interface AiHubPersonalAgentScheduleTabValueI {
    cronExpression?: string | null;
    dayOfMonth?: number | null;
    dayOfWeek?: number | null;
    enabled: boolean;
    frequencyKind: ScheduleFrequencyKind;
    intervalMinutes?: number | null;
    lifecycleKind: ScheduleLifecycleKind;
    maxRuns?: number | null;
    minuteOfHour?: number | null;
    nextRunAt?: string | null;
    prompt: string;
    startDate?: string | null;
    timeOfDay?: string | null;
    title: string;
    zoneId: string;
}

interface AiHubPersonalAgentScheduleTabProps {
    existingSchedule?: AiHubPersonalAgentScheduleFieldsFragment | null;
    onChange: (value: AiHubPersonalAgentScheduleTabValueI) => void;
    onRemove?: () => void;
    value: AiHubPersonalAgentScheduleTabValueI;
}

export const buildDefaultScheduleValue = (): AiHubPersonalAgentScheduleTabValueI => ({
    enabled: false,
    frequencyKind: ScheduleFrequencyKind.Daily,
    lifecycleKind: ScheduleLifecycleKind.Recurring,
    prompt: '',
    title: '',
    zoneId: Intl.DateTimeFormat().resolvedOptions().timeZone,
});

export const fromExistingSchedule = (
    schedule: AiHubPersonalAgentScheduleFieldsFragment
): AiHubPersonalAgentScheduleTabValueI => ({
    cronExpression: schedule.cronExpression,
    dayOfMonth: schedule.dayOfMonth,
    dayOfWeek: schedule.dayOfWeek,
    enabled: schedule.enabled,
    frequencyKind: schedule.frequencyKind,
    intervalMinutes: schedule.intervalMinutes,
    lifecycleKind: schedule.lifecycleKind,
    maxRuns: schedule.maxRuns,
    minuteOfHour: schedule.minuteOfHour,
    nextRunAt: schedule.nextRunAt,
    prompt: schedule.prompt,
    startDate: schedule.startDate,
    timeOfDay: schedule.timeOfDay,
    title: schedule.title,
    zoneId: schedule.zoneId,
});

const AiHubPersonalAgentScheduleTab = ({
    existingSchedule,
    onChange,
    onRemove,
    value,
}: AiHubPersonalAgentScheduleTabProps) => {
    const [confirmingRemove, setConfirmingRemove] = useState(false);

    const update = (patch: Partial<AiHubPersonalAgentScheduleTabValueI>) => onChange({...value, ...patch});

    const frequencyFields: ScheduleFrequencyFieldsValueI = {
        cronExpression: value.cronExpression,
        dayOfMonth: value.dayOfMonth,
        dayOfWeek: value.dayOfWeek,
        intervalMinutes: value.intervalMinutes,
        minuteOfHour: value.minuteOfHour,
        timeOfDay: value.timeOfDay,
    };

    return (
        <fieldset className="mx-auto flex w-full max-w-2xl flex-col gap-5 border-0">
            <fieldset className="flex items-center gap-3 border-0">
                <Switch
                    checked={value.enabled}
                    id="schedule-enabled"
                    onCheckedChange={(enabled) => update({enabled})}
                />

                <Label htmlFor="schedule-enabled">Run this agent on a schedule</Label>
            </fieldset>

            <fieldset className="flex flex-col gap-1.5 border-0">
                <Label htmlFor="schedule-title">Title</Label>

                <Input
                    id="schedule-title"
                    onChange={(event) => update({title: event.target.value})}
                    placeholder="e.g., Daily report generation"
                    value={value.title}
                />
            </fieldset>

            <fieldset className="flex flex-col gap-1.5 border-0">
                <Label htmlFor="schedule-prompt">Task description</Label>

                <Textarea
                    id="schedule-prompt"
                    onChange={(event) => update({prompt: event.target.value})}
                    placeholder="Describe what this scheduled task should do..."
                    rows={3}
                    value={value.prompt}
                />
            </fieldset>

            <fieldset className="flex flex-col gap-1.5 border-0">
                <Label htmlFor="schedule-frequency">Run frequency</Label>

                <Select
                    onValueChange={(selected) => update({frequencyKind: selected as ScheduleFrequencyKind})}
                    value={value.frequencyKind}
                >
                    <SelectTrigger id="schedule-frequency">
                        <SelectValue />
                    </SelectTrigger>

                    <SelectContent>
                        <SelectItem value={ScheduleFrequencyKind.EveryXMinutes}>Every X Minutes</SelectItem>

                        <SelectItem value={ScheduleFrequencyKind.Hourly}>Hourly</SelectItem>

                        <SelectItem value={ScheduleFrequencyKind.Daily}>Daily</SelectItem>

                        <SelectItem value={ScheduleFrequencyKind.Weekly}>Weekly</SelectItem>

                        <SelectItem value={ScheduleFrequencyKind.Monthly}>Monthly</SelectItem>

                        <SelectItem value={ScheduleFrequencyKind.CustomCron}>Custom (Cron)</SelectItem>
                    </SelectContent>
                </Select>
            </fieldset>

            <AiHubPersonalAgentScheduleFrequencyFields
                frequencyKind={value.frequencyKind}
                onChange={(next) => update(next)}
                value={frequencyFields}
            />

            <fieldset className="flex flex-col gap-1.5 border-0">
                <Label htmlFor="schedule-timezone">Timezone</Label>

                <Select onValueChange={(zoneId) => update({zoneId})} value={value.zoneId}>
                    <SelectTrigger id="schedule-timezone">
                        <SelectValue placeholder="Select..." />
                    </SelectTrigger>

                    <SelectContent>
                        {COMMON_TIMEZONES.map((zone) => (
                            <SelectItem key={zone} value={zone}>
                                {zone}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            </fieldset>

            <fieldset className="flex flex-col gap-1.5 border-0">
                <Label htmlFor="schedule-start-date">Start date (optional)</Label>

                <Input
                    id="schedule-start-date"
                    onChange={(event) => update({startDate: event.target.value || null})}
                    placeholder="Starts immediately"
                    type="datetime-local"
                    value={value.startDate ?? ''}
                />
            </fieldset>

            <fieldset className="flex flex-col gap-1.5 border-0">
                <Label>Lifecycle</Label>

                <div className="flex gap-2">
                    <Button
                        label="Recurring"
                        onClick={() => update({lifecycleKind: ScheduleLifecycleKind.Recurring, maxRuns: null})}
                        variant={value.lifecycleKind === ScheduleLifecycleKind.Recurring ? 'default' : 'outline'}
                    />

                    <Button
                        label="Number of runs"
                        onClick={() => update({lifecycleKind: ScheduleLifecycleKind.NumberOfRuns})}
                        variant={value.lifecycleKind === ScheduleLifecycleKind.NumberOfRuns ? 'default' : 'outline'}
                    />
                </div>
            </fieldset>

            {value.lifecycleKind === ScheduleLifecycleKind.NumberOfRuns && (
                <fieldset className="flex flex-col gap-1.5 border-0">
                    <Label htmlFor="schedule-max-runs">Max runs (optional)</Label>

                    <Input
                        id="schedule-max-runs"
                        min={1}
                        onChange={(event) =>
                            update({maxRuns: event.target.value ? parseInt(event.target.value, 10) : null})
                        }
                        placeholder="No limit"
                        type="number"
                        value={value.maxRuns ?? ''}
                    />
                </fieldset>
            )}

            {value.nextRunAt && <p className="text-xs text-muted-foreground">Next run: {value.nextRunAt}</p>}

            {existingSchedule != null && onRemove != null && (
                <fieldset className="flex justify-start border-0 pt-4">
                    {confirmingRemove ? (
                        <div className="flex items-center gap-2">
                            <span className="text-sm text-muted-foreground">
                                Remove the schedule? Run history will be lost.
                            </span>

                            <Button
                                label="Confirm remove"
                                onClick={() => {
                                    setConfirmingRemove(false);
                                    onRemove();
                                }}
                                variant="destructive"
                            />

                            <Button label="Cancel" onClick={() => setConfirmingRemove(false)} variant="outline" />
                        </div>
                    ) : (
                        <Button label="Remove schedule" onClick={() => setConfirmingRemove(true)} variant="outline" />
                    )}
                </fieldset>
            )}
        </fieldset>
    );
};

export default AiHubPersonalAgentScheduleTab;
