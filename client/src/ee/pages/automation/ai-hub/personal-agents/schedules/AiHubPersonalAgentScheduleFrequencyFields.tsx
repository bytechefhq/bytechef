import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {ScheduleFrequencyKind} from '@/shared/middleware/graphql';

export interface ScheduleFrequencyFieldsValueI {
    cronExpression?: string | null;
    dayOfMonth?: number | null;
    dayOfWeek?: number | null;
    intervalMinutes?: number | null;
    minuteOfHour?: number | null;
    timeOfDay?: string | null;
}

interface AiHubPersonalAgentScheduleFrequencyFieldsProps {
    frequencyKind: ScheduleFrequencyKind;
    onChange: (next: ScheduleFrequencyFieldsValueI) => void;
    value: ScheduleFrequencyFieldsValueI;
}

const DAYS_OF_WEEK = [
    {label: 'Monday', value: 1},
    {label: 'Tuesday', value: 2},
    {label: 'Wednesday', value: 3},
    {label: 'Thursday', value: 4},
    {label: 'Friday', value: 5},
    {label: 'Saturday', value: 6},
    {label: 'Sunday', value: 7},
];

const AiHubPersonalAgentScheduleFrequencyFields = ({
    frequencyKind,
    onChange,
    value,
}: AiHubPersonalAgentScheduleFrequencyFieldsProps) => {
    const update = (patch: Partial<ScheduleFrequencyFieldsValueI>) => onChange({...value, ...patch});

    if (frequencyKind === ScheduleFrequencyKind.EveryXMinutes) {
        return (
            <fieldset className="border-0">
                <Label htmlFor="intervalMinutes">Every</Label>

                <Input
                    id="intervalMinutes"
                    max={59}
                    min={1}
                    onChange={(event) => update({intervalMinutes: parseInt(event.target.value, 10) || null})}
                    placeholder="minutes (1-59)"
                    type="number"
                    value={value.intervalMinutes ?? ''}
                />
            </fieldset>
        );
    }

    if (frequencyKind === ScheduleFrequencyKind.Hourly) {
        return (
            <fieldset className="border-0">
                <Label htmlFor="minuteOfHour">Minute of hour</Label>

                <Input
                    id="minuteOfHour"
                    max={59}
                    min={0}
                    onChange={(event) => update({minuteOfHour: parseInt(event.target.value, 10) || 0})}
                    type="number"
                    value={value.minuteOfHour ?? ''}
                />
            </fieldset>
        );
    }

    if (frequencyKind === ScheduleFrequencyKind.Daily) {
        return (
            <fieldset className="border-0">
                <Label htmlFor="timeOfDay">Time</Label>

                <Input
                    id="timeOfDay"
                    onChange={(event) => update({timeOfDay: event.target.value})}
                    type="time"
                    value={value.timeOfDay ?? ''}
                />
            </fieldset>
        );
    }

    if (frequencyKind === ScheduleFrequencyKind.Weekly) {
        return (
            <>
                <fieldset className="border-0">
                    <Label htmlFor="dayOfWeek">Day of week</Label>

                    <Select
                        onValueChange={(selected) => update({dayOfWeek: parseInt(selected, 10)})}
                        value={value.dayOfWeek?.toString() ?? ''}
                    >
                        <SelectTrigger id="dayOfWeek">
                            <SelectValue placeholder="Select..." />
                        </SelectTrigger>

                        <SelectContent>
                            {DAYS_OF_WEEK.map((day) => (
                                <SelectItem key={day.value} value={day.value.toString()}>
                                    {day.label}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </fieldset>

                <fieldset className="border-0">
                    <Label htmlFor="timeOfDay">Time</Label>

                    <Input
                        id="timeOfDay"
                        onChange={(event) => update({timeOfDay: event.target.value})}
                        type="time"
                        value={value.timeOfDay ?? ''}
                    />
                </fieldset>
            </>
        );
    }

    if (frequencyKind === ScheduleFrequencyKind.Monthly) {
        return (
            <>
                <fieldset className="border-0">
                    <Label htmlFor="dayOfMonth">Day of month</Label>

                    <Input
                        id="dayOfMonth"
                        max={31}
                        min={1}
                        onChange={(event) => update({dayOfMonth: parseInt(event.target.value, 10) || null})}
                        type="number"
                        value={value.dayOfMonth ?? ''}
                    />
                </fieldset>

                <fieldset className="border-0">
                    <Label htmlFor="timeOfDay">Time</Label>

                    <Input
                        id="timeOfDay"
                        onChange={(event) => update({timeOfDay: event.target.value})}
                        type="time"
                        value={value.timeOfDay ?? ''}
                    />
                </fieldset>
            </>
        );
    }

    if (frequencyKind === ScheduleFrequencyKind.CustomCron) {
        return (
            <fieldset className="border-0">
                <Label htmlFor="cronExpression">Cron expression</Label>

                <Input
                    id="cronExpression"
                    onChange={(event) => update({cronExpression: event.target.value})}
                    placeholder="0 9 * * *"
                    type="text"
                    value={value.cronExpression ?? ''}
                />
            </fieldset>
        );
    }

    return null;
};

export default AiHubPersonalAgentScheduleFrequencyFields;
