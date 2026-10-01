export const SCHEDULE_FREQUENCY_KINDS = [
    'EVERY_X_MINUTES',
    'HOURLY',
    'DAILY',
    'WEEKLY',
    'MONTHLY',
    'CUSTOM_CRON',
] as const;

export type ScheduleFrequencyKindType = (typeof SCHEDULE_FREQUENCY_KINDS)[number];

export interface AgentScheduleCadenceI {
    cronExpression?: string | null;
    dayOfMonth?: number | null;
    dayOfWeek?: number | null;
    frequencyKind: ScheduleFrequencyKindType;
    intervalMinutes?: number | null;
    minuteOfHour?: number | null;
    timeOfDay?: string | null;
}

export interface AgentScheduleCadenceErrorsI {
    cronExpression?: string;
    dayOfMonth?: string;
    dayOfWeek?: string;
    intervalMinutes?: string;
    minuteOfHour?: string;
    timeOfDay?: string;
}

export const SCHEDULE_FREQUENCY_LABELS: Record<ScheduleFrequencyKindType, string> = {
    CUSTOM_CRON: 'Custom cron',
    DAILY: 'Daily',
    EVERY_X_MINUTES: 'Every X minutes',
    HOURLY: 'Hourly',
    MONTHLY: 'Monthly',
    WEEKLY: 'Weekly',
};

const parseTimeOfDay = (timeOfDay?: string | null) => {
    const [hour, minute] = (timeOfDay ?? '').split(':');

    return {hour: Number(hour), minute: Number(minute)};
};

export const toCronExpression = (cadence: AgentScheduleCadenceI): string => {
    if (cadence.frequencyKind === 'CUSTOM_CRON') {
        return (cadence.cronExpression ?? '').trim();
    }

    if (cadence.frequencyKind === 'EVERY_X_MINUTES') {
        return `0/${cadence.intervalMinutes} * * * ?`;
    }

    if (cadence.frequencyKind === 'HOURLY') {
        return `${cadence.minuteOfHour} * * * ?`;
    }

    const {hour, minute} = parseTimeOfDay(cadence.timeOfDay);

    if (cadence.frequencyKind === 'DAILY') {
        return `${minute} ${hour} * * ?`;
    }

    if (cadence.frequencyKind === 'WEEKLY') {
        const quartzDay = ((cadence.dayOfWeek ?? 1) % 7) + 1;

        return `${minute} ${hour} ? * ${quartzDay}`;
    }

    return `${minute} ${hour} ${cadence.dayOfMonth} * ?`;
};

export const validateAgentScheduleCadence = (cadence: AgentScheduleCadenceI): AgentScheduleCadenceErrorsI => {
    const errors: AgentScheduleCadenceErrorsI = {};

    if (cadence.frequencyKind === 'EVERY_X_MINUTES') {
        const interval = cadence.intervalMinutes;

        if (interval == null || interval < 1 || interval > 59) {
            errors.intervalMinutes = 'An interval of 1 to 59 minutes is required.';
        }
    }

    if (cadence.frequencyKind === 'HOURLY') {
        const minute = cadence.minuteOfHour;

        if (minute == null || minute < 0 || minute > 59) {
            errors.minuteOfHour = 'A minute of the hour (0 to 59) is required.';
        }
    }

    if (
        (cadence.frequencyKind === 'DAILY' ||
            cadence.frequencyKind === 'WEEKLY' ||
            cadence.frequencyKind === 'MONTHLY') &&
        !cadence.timeOfDay
    ) {
        errors.timeOfDay = 'A time of day is required.';
    }

    if (cadence.frequencyKind === 'WEEKLY' && cadence.dayOfWeek == null) {
        errors.dayOfWeek = 'A day of the week is required.';
    }

    if (cadence.frequencyKind === 'MONTHLY') {
        const dayOfMonth = cadence.dayOfMonth;

        if (dayOfMonth == null || dayOfMonth < 1 || dayOfMonth > 31) {
            errors.dayOfMonth = 'A day of the month (1 to 31) is required.';
        }
    }

    if (cadence.frequencyKind === 'CUSTOM_CRON' && !cadence.cronExpression?.trim()) {
        errors.cronExpression = 'A cron expression is required.';
    }

    return errors;
};

export const CADENCE_PARAMETER_KEYS = [
    'cronExpression',
    'dayOfMonth',
    'dayOfWeek',
    'frequencyKind',
    'intervalMinutes',
    'minuteOfHour',
    'timeOfDay',
] as const;

export const toCadenceParameters = (cadence: AgentScheduleCadenceI): Record<string, string> => {
    const parameters: Record<string, string> = {frequencyKind: cadence.frequencyKind};

    const put = (key: string, value: number | string | null | undefined) => {
        if (value != null && value !== '') {
            parameters[key] = String(value);
        }
    };

    put('cronExpression', cadence.cronExpression);
    put('dayOfMonth', cadence.dayOfMonth);
    put('dayOfWeek', cadence.dayOfWeek);
    put('intervalMinutes', cadence.intervalMinutes);
    put('minuteOfHour', cadence.minuteOfHour);
    put('timeOfDay', cadence.timeOfDay);

    return parameters;
};

const toNumber = (value: unknown): number | null => {
    if (value == null || value === '') {
        return null;
    }

    const parsed = Number(value);

    return Number.isNaN(parsed) ? null : parsed;
};

const toText = (value: unknown): string | null => (value == null || value === '' ? null : String(value));

export const fromCadenceParameters = (parameters: Record<string, unknown>): AgentScheduleCadenceI => {
    const storedKind = parameters.frequencyKind;

    const frequencyKind = SCHEDULE_FREQUENCY_KINDS.includes(storedKind as ScheduleFrequencyKindType)
        ? (storedKind as ScheduleFrequencyKindType)
        : 'CUSTOM_CRON';

    return {
        cronExpression:
            frequencyKind === 'CUSTOM_CRON'
                ? (toText(parameters.cronExpression) ?? toText(parameters.expression))
                : toText(parameters.cronExpression),
        dayOfMonth: toNumber(parameters.dayOfMonth),
        dayOfWeek: toNumber(parameters.dayOfWeek),
        frequencyKind,
        intervalMinutes: toNumber(parameters.intervalMinutes),
        minuteOfHour: toNumber(parameters.minuteOfHour),
        timeOfDay: toText(parameters.timeOfDay),
    };
};

const DAY_OF_WEEK_LABELS: Record<number, string> = {
    1: 'Monday',
    2: 'Tuesday',
    3: 'Wednesday',
    4: 'Thursday',
    5: 'Friday',
    6: 'Saturday',
    7: 'Sunday',
};

export const describeCadence = (cadence: AgentScheduleCadenceI): string => {
    const timeOfDay = cadence.timeOfDay;

    if (cadence.frequencyKind === 'EVERY_X_MINUTES' && cadence.intervalMinutes != null) {
        return cadence.intervalMinutes === 1 ? 'Every minute' : `Every ${cadence.intervalMinutes} minutes`;
    }

    if (cadence.frequencyKind === 'HOURLY' && cadence.minuteOfHour != null) {
        return `Hourly at :${String(cadence.minuteOfHour).padStart(2, '0')}`;
    }

    if (cadence.frequencyKind === 'DAILY' && timeOfDay) {
        return `Daily at ${timeOfDay}`;
    }

    if (cadence.frequencyKind === 'WEEKLY' && timeOfDay && cadence.dayOfWeek != null) {
        return `Weekly on ${DAY_OF_WEEK_LABELS[cadence.dayOfWeek] ?? `day ${cadence.dayOfWeek}`} at ${timeOfDay}`;
    }

    if (cadence.frequencyKind === 'MONTHLY' && timeOfDay && cadence.dayOfMonth != null) {
        return `Monthly on day ${cadence.dayOfMonth} at ${timeOfDay}`;
    }

    return cadence.cronExpression?.trim() ?? '';
};

export const fromCronExpression = (expression?: string | null): AgentScheduleCadenceI => {
    const trimmedExpression = (expression ?? '').trim();
    const custom: AgentScheduleCadenceI = {cronExpression: trimmedExpression, frequencyKind: 'CUSTOM_CRON'};

    const fields = trimmedExpression.split(/\s+/);

    if (fields.length !== 5) {
        return custom;
    }

    const [minuteField, hourField, dayOfMonthField, monthField, dayOfWeekField] = fields;

    if (monthField !== '*') {
        return custom;
    }

    const isNumeric = (field: string) => /^\d+$/.test(field);

    const timeOfDay = () =>
        `${String(Number(hourField)).padStart(2, '0')}:${String(Number(minuteField)).padStart(2, '0')}`;

    if (dayOfWeekField === '?' && dayOfMonthField === '*' && hourField === '*') {
        const everyXMinutesMatch = /^0\/(\d+)$/.exec(minuteField);

        if (everyXMinutesMatch) {
            return {frequencyKind: 'EVERY_X_MINUTES', intervalMinutes: Number(everyXMinutesMatch[1])};
        }

        if (isNumeric(minuteField)) {
            return {frequencyKind: 'HOURLY', minuteOfHour: Number(minuteField)};
        }

        return custom;
    }

    if (!isNumeric(minuteField) || !isNumeric(hourField)) {
        return custom;
    }

    if (dayOfWeekField === '?' && dayOfMonthField === '*') {
        return {frequencyKind: 'DAILY', timeOfDay: timeOfDay()};
    }

    if (dayOfMonthField === '?' && isNumeric(dayOfWeekField)) {
        const quartzDayOfWeek = Number(dayOfWeekField);

        if (quartzDayOfWeek < 1 || quartzDayOfWeek > 7) {
            return custom;
        }

        return {
            dayOfWeek: quartzDayOfWeek === 1 ? 7 : quartzDayOfWeek - 1,
            frequencyKind: 'WEEKLY',
            timeOfDay: timeOfDay(),
        };
    }

    if (dayOfWeekField === '?' && isNumeric(dayOfMonthField)) {
        return {dayOfMonth: Number(dayOfMonthField), frequencyKind: 'MONTHLY', timeOfDay: timeOfDay()};
    }

    return custom;
};

export const describeCronExpression = (expression?: string | null): string =>
    describeCadence(fromCronExpression(expression));
