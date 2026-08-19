import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Label} from '@/components/ui/label';
import {AgentScheduleCadenceErrorsI, AgentScheduleCadenceI} from '@/pages/automation/agents/utils/agentScheduleCron';

interface AgentScheduleFrequencyFieldsProps {
    cadence: AgentScheduleCadenceI;
    errors?: AgentScheduleCadenceErrorsI;
    onChange: (next: AgentScheduleCadenceI) => void;
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

/**
 * The cadence inputs for one frequency kind. This component decides which fields belong to which kind, and
 * validateAgentScheduleCadence keys its messages the same way — the two must stay in step, or a required
 * field can be unreachable while still blocking submit.
 */
const AgentScheduleFrequencyFields = ({cadence, errors, onChange}: AgentScheduleFrequencyFieldsProps) => {
    const update = (patch: Partial<AgentScheduleCadenceI>) => onChange({...cadence, ...patch});

    const fieldError = (message?: string) => message && <p className="text-xs text-destructive">{message}</p>;

    const timeField = (
        <div className="space-y-1">
            <Label htmlFor="agent-schedule-time-of-day">Time</Label>

            <Input
                id="agent-schedule-time-of-day"
                onChange={(event) => update({timeOfDay: event.target.value})}
                type="time"
                value={cadence.timeOfDay ?? ''}
            />

            {fieldError(errors?.timeOfDay)}
        </div>
    );

    if (cadence.frequencyKind === 'EVERY_X_MINUTES') {
        return (
            <div className="space-y-1">
                <Label htmlFor="agent-schedule-interval-minutes">Every</Label>

                <Input
                    id="agent-schedule-interval-minutes"
                    max={59}
                    min={1}
                    onChange={(event) => update({intervalMinutes: parseInt(event.target.value, 10) || null})}
                    placeholder="minutes (1-59)"
                    type="number"
                    value={cadence.intervalMinutes ?? ''}
                />

                {fieldError(errors?.intervalMinutes)}
            </div>
        );
    }

    if (cadence.frequencyKind === 'HOURLY') {
        return (
            <div className="space-y-1">
                <Label htmlFor="agent-schedule-minute-of-hour">Minute of hour</Label>

                <Input
                    id="agent-schedule-minute-of-hour"
                    max={59}
                    min={0}
                    onChange={(event) => update({minuteOfHour: parseInt(event.target.value, 10) || 0})}
                    type="number"
                    value={cadence.minuteOfHour ?? ''}
                />

                {fieldError(errors?.minuteOfHour)}
            </div>
        );
    }

    if (cadence.frequencyKind === 'DAILY') {
        return timeField;
    }

    if (cadence.frequencyKind === 'WEEKLY') {
        return (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="space-y-1">
                    <Label htmlFor="agent-schedule-day-of-week">Day of week</Label>

                    <Select
                        onValueChange={(selected) => update({dayOfWeek: parseInt(selected, 10)})}
                        value={cadence.dayOfWeek?.toString() ?? ''}
                    >
                        <SelectTrigger id="agent-schedule-day-of-week">
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

                    {fieldError(errors?.dayOfWeek)}
                </div>

                {timeField}
            </div>
        );
    }

    if (cadence.frequencyKind === 'MONTHLY') {
        return (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="space-y-1">
                    <Label htmlFor="agent-schedule-day-of-month">Day of month</Label>

                    <Input
                        id="agent-schedule-day-of-month"
                        max={31}
                        min={1}
                        onChange={(event) => update({dayOfMonth: parseInt(event.target.value, 10) || null})}
                        type="number"
                        value={cadence.dayOfMonth ?? ''}
                    />

                    {fieldError(errors?.dayOfMonth)}
                </div>

                {timeField}
            </div>
        );
    }

    return (
        <div className="space-y-1">
            <Label htmlFor="agent-schedule-cron-expression">Cron expression</Label>

            <Input
                id="agent-schedule-cron-expression"
                onChange={(event) => update({cronExpression: event.target.value})}
                placeholder="0 9 * * ?"
                type="text"
                value={cadence.cronExpression ?? ''}
            />

            {fieldError(errors?.cronExpression)}
        </div>
    );
};

export default AgentScheduleFrequencyFields;
