# Delegate AI Hub "Scheduled" to the AI Agent — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Delete the AI Hub task subsystem entirely and move scheduled agent runs onto the AI Agent's existing `schedule` channel, adding a frequency picker, an optional schedule on agent creation, and a Scheduled filter with per-row markers on the agents list.

**Architecture:** Build first, delete second. Tasks 1–6 add everything to the Agents pages (a pure cron-generation module, a ported frequency picker, dialog wiring, list filter and marker) so the capability never disappears; Tasks 7–12 then remove the AI Hub tasks tree, the task tool callbacks and subagent, the `TASK` chat kind and its overlay, the task package and GraphQL, the Liquibase changelogs, and the Quartz hub scheduler. Task 13 updates the docs.

**Tech Stack:** React 19 + TypeScript 5.9 + Vitest 4 (client); Java 25 + Spring Boot 4 + Gradle (server); Liquibase; GraphQL Codegen.

**Spec:** `docs/superpowers/specs/2026-08-17-delegate-scheduled-to-ai-agent-design.md`

## Global Constraints

- **Commit with an explicit pathspec**: `git commit -m "..." -- <paths>`. The user commits in parallel on `0_732`; a bare `git commit` sweeps their work into your commit. Never `git commit --amend`.
- **Commit message convention**: client-only changes `732 client - <description>`; anything touching server `732 <description>`.
- **Client checks before every client commit**: `cd client && npm run check` (lint + typecheck + tests). ESLint `sort-keys` is **not** auto-fixable — write object literals in alphabetical key order by hand. Interface names must end in `I` or `Props`. Named imports inside `{}` must be alphabetically sorted. Icons import with the `Icon` suffix (`CalendarClockIcon`). Use `twMerge`, never `cn()`. `useRef` variables end in `Ref`.
- **Client hook order** in components: `useState` → `useRef` → store hooks → other custom hooks → derived/`useMemo`/`useCallback` → `useEffect` → `return`.
- **Server checks before every server commit**: `./gradlew spotlessApply` then `./gradlew compileJava compileTestJava --continue`. Never judge a Gradle run piped into `tail`/`grep` — redirect to a file, check `$?` on its own line, then grep the file for `^> Task .* FAILED`.
- **Cron format is 5-field**: `[Minute] [Hour] [Day of Month] [Month] [Day of Week]`. `ScheduleCronTrigger` prepends `"0 "` before handing it to Quartz, so exactly one of day-of-month / day-of-week must be `?`.
- **ISO→Quartz weekday**: `quartzDay = (isoDayOfWeek % 7) + 1` (ISO 1=Mon…7=Sun, Quartz 1=Sun…7=Sat).
- The AI Hub is unreleased (`git ls-tree -r --name-only v0.31.3 | grep ai/hub` returns nothing), which is what makes changelog deletion and the `AiHubChatKind` ordinal shift legal. Re-verify this command still returns nothing before Task 9 or Task 11.

---

### Task 1: Cron generation and validation module

A pure module with no React in it, so the cron rules are testable in isolation and shared by both dialogs.

**Files:**
- Create: `client/src/pages/automation/agents/utils/agentScheduleCron.ts`
- Test: `client/src/pages/automation/agents/utils/tests/agentScheduleCron.test.ts`

**Interfaces:**
- Consumes: nothing.
- Produces: `SCHEDULE_FREQUENCY_KINDS`, `ScheduleFrequencyKindType`, `AgentScheduleCadenceI`, `AgentScheduleCadenceErrorsI`, `toCronExpression(cadence: AgentScheduleCadenceI): string`, `validateAgentScheduleCadence(cadence: AgentScheduleCadenceI): AgentScheduleCadenceErrorsI`, `toCadenceParameters(cadence: AgentScheduleCadenceI): Record<string, string>`, `fromCadenceParameters(parameters: Record<string, unknown>): AgentScheduleCadenceI`.

- [ ] **Step 1: Write the failing test**

Create `client/src/pages/automation/agents/utils/tests/agentScheduleCron.test.ts`:

```ts
import {
    fromCadenceParameters,
    toCadenceParameters,
    toCronExpression,
    validateAgentScheduleCadence,
} from '@/pages/automation/agents/utils/agentScheduleCron';
import {describe, expect, it} from 'vitest';

describe('toCronExpression', () => {
    it('generates a five-field expression for EVERY_X_MINUTES', () => {
        expect(toCronExpression({frequencyKind: 'EVERY_X_MINUTES', intervalMinutes: 5})).toBe('0/5 * * * ?');
    });

    it('generates a five-field expression for HOURLY', () => {
        expect(toCronExpression({frequencyKind: 'HOURLY', minuteOfHour: 15})).toBe('15 * * * ?');
    });

    it('generates a five-field expression for DAILY', () => {
        expect(toCronExpression({frequencyKind: 'DAILY', timeOfDay: '09:30'})).toBe('30 9 * * ?');
    });

    it('converts an ISO weekday to a Quartz weekday for WEEKLY', () => {
        // ISO Monday (1) is Quartz 2; ISO Sunday (7) is Quartz 1.
        expect(toCronExpression({dayOfWeek: 1, frequencyKind: 'WEEKLY', timeOfDay: '08:00'})).toBe('0 8 ? * 2');
        expect(toCronExpression({dayOfWeek: 7, frequencyKind: 'WEEKLY', timeOfDay: '08:00'})).toBe('0 8 ? * 1');
    });

    it('generates a five-field expression for MONTHLY', () => {
        expect(toCronExpression({dayOfMonth: 3, frequencyKind: 'MONTHLY', timeOfDay: '23:05'})).toBe('5 23 3 * ?');
    });

    it('passes a custom cron expression through, trimmed', () => {
        expect(toCronExpression({cronExpression: '  0 9 * * 1  ', frequencyKind: 'CUSTOM_CRON'})).toBe('0 9 * * 1');
    });
});

describe('validateAgentScheduleCadence', () => {
    it('requires the cadence field each kind owns', () => {
        expect(validateAgentScheduleCadence({frequencyKind: 'EVERY_X_MINUTES'}).intervalMinutes).toBeDefined();
        expect(validateAgentScheduleCadence({frequencyKind: 'HOURLY'}).minuteOfHour).toBeDefined();
        expect(validateAgentScheduleCadence({frequencyKind: 'DAILY'}).timeOfDay).toBeDefined();
        expect(validateAgentScheduleCadence({frequencyKind: 'WEEKLY'}).dayOfWeek).toBeDefined();
        expect(validateAgentScheduleCadence({frequencyKind: 'MONTHLY'}).dayOfMonth).toBeDefined();
        expect(validateAgentScheduleCadence({frequencyKind: 'CUSTOM_CRON'}).cronExpression).toBeDefined();
    });

    it('rejects an out-of-range interval', () => {
        expect(validateAgentScheduleCadence({frequencyKind: 'EVERY_X_MINUTES', intervalMinutes: 60})
            .intervalMinutes).toBeDefined();
    });

    it('accepts a complete cadence', () => {
        expect(validateAgentScheduleCadence({dayOfWeek: 3, frequencyKind: 'WEEKLY', timeOfDay: '07:00'})).toEqual({});
    });
});

describe('cadence parameter round-trip', () => {
    it('restores the picker state it stored', () => {
        const cadence = {dayOfWeek: 3, frequencyKind: 'WEEKLY', timeOfDay: '07:00'} as const;

        expect(fromCadenceParameters(toCadenceParameters(cadence))).toEqual({
            cronExpression: null,
            dayOfMonth: null,
            dayOfWeek: 3,
            frequencyKind: 'WEEKLY',
            intervalMinutes: null,
            minuteOfHour: null,
            timeOfDay: '07:00',
        });
    });

    it('falls back to CUSTOM_CRON when no frequencyKind was stored', () => {
        expect(fromCadenceParameters({expression: '0 9 * * ?'})).toEqual({
            cronExpression: '0 9 * * ?',
            dayOfMonth: null,
            dayOfWeek: null,
            frequencyKind: 'CUSTOM_CRON',
            intervalMinutes: null,
            minuteOfHour: null,
            timeOfDay: null,
        });
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd client && npx vitest run src/pages/automation/agents/utils/tests/agentScheduleCron.test.ts`
Expected: FAIL — `Failed to resolve import ".../agentScheduleCron"`.

- [ ] **Step 3: Write the implementation**

Create `client/src/pages/automation/agents/utils/agentScheduleCron.ts`:

```ts
/**
 * Cron generation for an agent's `schedule` channel.
 *
 * The channel's `expression` parameter feeds `schedule/v1/cron`, whose property is documented as
 * `[Minute] [Hour] [Day of Month] [Month] [Day of Week]` — five fields. ScheduleCronTrigger prepends the
 * seconds field ("0 " + expression) before Quartz parses it, so exactly one of day-of-month / day-of-week
 * must be `?`, and generating a six-field Quartz expression here would shift every field by one.
 *
 * The cadence fields the picker collects are stored alongside the generated expression so reopening a
 * schedule shows "Daily at 09:00" instead of reverse-engineering a cron string. AiAgentWorkflowGenerator
 * emits only the trigger's declared properties from `parameters`, so these extra keys ride along unused.
 */
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
    /** ISO day of week: 1 = Monday … 7 = Sunday. */
    dayOfWeek?: number | null;
    frequencyKind: ScheduleFrequencyKindType;
    intervalMinutes?: number | null;
    minuteOfHour?: number | null;
    /** "HH:MM", as produced by <input type="time">. */
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
        // ISO 1=Mon…7=Sun; Quartz 1=Sun…7=Sat.
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

/** Channel parameters are a free-form map, so every cadence value is stored as a string and coerced on read. */
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

/**
 * Rebuilds picker state from a channel's parameters. A row written before the picker existed (or edited by
 * hand) carries no `frequencyKind`; it opens as CUSTOM_CRON showing its stored `expression`, which is exactly
 * what it is.
 */
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
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd client && npx vitest run src/pages/automation/agents/utils/tests/agentScheduleCron.test.ts`
Expected: PASS, 11 tests.

- [ ] **Step 5: Run the full client check**

Run: `cd client && npm run check`
Expected: no lint, typecheck, or test failures.

- [ ] **Step 6: Commit**

```bash
git commit -m "732 client - Add cron generation for agent schedules" -- \
  client/src/pages/automation/agents/utils/agentScheduleCron.ts \
  client/src/pages/automation/agents/utils/tests/agentScheduleCron.test.ts
```

---

### Task 2: Frequency fields component

The per-cadence inputs, ported from the AI Hub's `AiHubTaskScheduleFrequencyFields` but typed against Task 1's local union instead of the GraphQL `ScheduleFrequencyKind` enum (which Task 10 deletes).

**Files:**
- Create: `client/src/pages/automation/agents/components/detail/AgentScheduleFrequencyFields.tsx`
- Test: `client/src/pages/automation/agents/components/detail/AgentScheduleFrequencyFields.test.tsx`

**Interfaces:**
- Consumes: `AgentScheduleCadenceI`, `AgentScheduleCadenceErrorsI` from Task 1.
- Produces: default export `AgentScheduleFrequencyFields`, props `{cadence: AgentScheduleCadenceI; errors?: AgentScheduleCadenceErrorsI; onChange: (next: AgentScheduleCadenceI) => void}`.

- [ ] **Step 1: Write the failing test**

Create `client/src/pages/automation/agents/components/detail/AgentScheduleFrequencyFields.test.tsx`:

```tsx
import AgentScheduleFrequencyFields from '@/pages/automation/agents/components/detail/AgentScheduleFrequencyFields';
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

describe('AgentScheduleFrequencyFields', () => {
    it('renders the interval input for EVERY_X_MINUTES', () => {
        render(
            <AgentScheduleFrequencyFields cadence={{frequencyKind: 'EVERY_X_MINUTES'}} onChange={vi.fn()} />
        );

        expect(screen.getByLabelText('Every')).toBeInTheDocument();
        expect(screen.queryByLabelText('Time')).not.toBeInTheDocument();
    });

    it('renders both a day and a time for WEEKLY', () => {
        render(<AgentScheduleFrequencyFields cadence={{frequencyKind: 'WEEKLY'}} onChange={vi.fn()} />);

        expect(screen.getByLabelText('Day of week')).toBeInTheDocument();
        expect(screen.getByLabelText('Time')).toBeInTheDocument();
    });

    it('renders the cron input for CUSTOM_CRON', () => {
        render(<AgentScheduleFrequencyFields cadence={{frequencyKind: 'CUSTOM_CRON'}} onChange={vi.fn()} />);

        expect(screen.getByLabelText('Cron expression')).toBeInTheDocument();
    });

    it('shows the error message for the field it belongs to', () => {
        render(
            <AgentScheduleFrequencyFields
                cadence={{frequencyKind: 'DAILY'}}
                errors={{timeOfDay: 'A time of day is required.'}}
                onChange={vi.fn()}
            />
        );

        expect(screen.getByText('A time of day is required.')).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd client && npx vitest run src/pages/automation/agents/components/detail/AgentScheduleFrequencyFields.test.tsx`
Expected: FAIL — module not found.

- [ ] **Step 3: Write the implementation**

Create `client/src/pages/automation/agents/components/detail/AgentScheduleFrequencyFields.tsx`:

```tsx
import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Label} from '@/components/ui/label';
import {
    AgentScheduleCadenceErrorsI,
    AgentScheduleCadenceI,
} from '@/pages/automation/agents/utils/agentScheduleCron';

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
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd client && npx vitest run src/pages/automation/agents/components/detail/AgentScheduleFrequencyFields.test.tsx`
Expected: PASS, 4 tests.

- [ ] **Step 5: Run the full client check**

Run: `cd client && npm run check`

- [ ] **Step 6: Commit**

```bash
git commit -m "732 client - Add frequency fields for agent schedules" -- \
  client/src/pages/automation/agents/components/detail/AgentScheduleFrequencyFields.tsx \
  client/src/pages/automation/agents/components/detail/AgentScheduleFrequencyFields.test.tsx
```

---

### Task 3: Frequency picker on the agent schedule dialog

Replace the raw cron input in `AgentScheduleDialog` with the picker; store the generated expression plus the cadence fields.

**Files:**
- Modify: `client/src/pages/automation/agents/components/detail/AgentScheduleDialog.tsx`
- Modify: `client/src/pages/automation/agents/components/detail/AgentScheduleCard.tsx:80-92` (`toScheduleProperties`)
- Test: `client/src/pages/automation/agents/components/detail/AgentScheduleDialog.test.tsx`

**Interfaces:**
- Consumes: Task 1's `AgentScheduleCadenceI`, `fromCadenceParameters`, `toCadenceParameters`, `toCronExpression`, `validateAgentScheduleCadence`, `SCHEDULE_FREQUENCY_KINDS`, `SCHEDULE_FREQUENCY_LABELS`; Task 2's `AgentScheduleFrequencyFields`.
- Produces: `AgentSchedulePropertiesI` becomes a string-valued map with four required keys —
  `{[key: string]: string | undefined; expression: string; name: string; prompt: string; timezone: string}`.
  Every cadence value rides in that map as a string, because `AiAgentChannel.parameters` is a free-form map and
  a mixed number/string payload would need a cast at every boundary. `AgentScheduleCard.handleSubmit` passes it
  to `addAiAgentChannel`/`updateAiAgentChannel` unchanged.

- [ ] **Step 1: Write the failing test**

Create `client/src/pages/automation/agents/components/detail/AgentScheduleDialog.test.tsx`:

```tsx
import AgentScheduleDialog from '@/pages/automation/agents/components/detail/AgentScheduleDialog';
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

describe('AgentScheduleDialog', () => {
    it('submits a generated cron expression alongside the cadence fields', () => {
        const onSubmit = vi.fn();

        render(<AgentScheduleDialog onClose={vi.fn()} onSubmit={onSubmit} open={true} />);

        fireEvent.change(screen.getByLabelText('Name'), {target: {value: 'Daily digest'}});
        fireEvent.change(screen.getByLabelText('Prompt'), {target: {value: 'Summarise yesterday'}});
        fireEvent.change(screen.getByLabelText('Time'), {target: {value: '09:30'}});
        fireEvent.click(screen.getByRole('button', {name: 'Add'}));

        expect(onSubmit).toHaveBeenCalledWith(
            expect.objectContaining({expression: '30 9 * * ?', frequencyKind: 'DAILY', timeOfDay: '09:30'})
        );
    });

    it('blocks submit and shows the cadence error when a required field is missing', () => {
        const onSubmit = vi.fn();

        render(<AgentScheduleDialog onClose={vi.fn()} onSubmit={onSubmit} open={true} />);

        fireEvent.change(screen.getByLabelText('Prompt'), {target: {value: 'Summarise yesterday'}});
        fireEvent.click(screen.getByRole('button', {name: 'Add'}));

        expect(onSubmit).not.toHaveBeenCalled();
        expect(screen.getByText('A time of day is required.')).toBeInTheDocument();
    });

    it('opens a schedule with no stored frequencyKind as a custom cron', () => {
        render(
            <AgentScheduleDialog
                onClose={vi.fn()}
                onSubmit={vi.fn()}
                open={true}
                schedule={{expression: '0 9 * * ?', name: 'Legacy', prompt: 'Run', timezone: 'UTC'}}
            />
        );

        expect(screen.getByLabelText('Cron expression')).toHaveValue('0 9 * * ?');
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd client && npx vitest run src/pages/automation/agents/components/detail/AgentScheduleDialog.test.tsx`
Expected: FAIL — the dialog renders a "Cron expression" input in every mode and never produces `frequencyKind`.

- [ ] **Step 3: Rewrite the dialog**

Replace the whole of `client/src/pages/automation/agents/components/detail/AgentScheduleDialog.tsx` with:

```tsx
import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import AgentScheduleFrequencyFields from '@/pages/automation/agents/components/detail/AgentScheduleFrequencyFields';
import {
    AgentScheduleCadenceErrorsI,
    AgentScheduleCadenceI,
    SCHEDULE_FREQUENCY_KINDS,
    SCHEDULE_FREQUENCY_LABELS,
    ScheduleFrequencyKindType,
    fromCadenceParameters,
    toCadenceParameters,
    toCronExpression,
    validateAgentScheduleCadence,
} from '@/pages/automation/agents/utils/agentScheduleCron';
import {useMemo, useState} from 'react';

/**
 * What one schedule stores on its channel row. The four named keys are always present; the cadence keys
 * (frequencyKind, timeOfDay, dayOfWeek, …) ride in the same map as strings, written by toCadenceParameters
 * and read back by fromCadenceParameters.
 */
export interface AgentSchedulePropertiesI {
    [key: string]: string | undefined;
    expression: string;
    name: string;
    prompt: string;
    timezone: string;
}

interface AgentScheduleDialogPropsI {
    onClose: () => void;
    onSubmit: (values: AgentSchedulePropertiesI) => void;
    open: boolean;
    pending?: boolean;
    /** Present = edit that schedule; absent = add a new one. */
    schedule?: AgentSchedulePropertiesI | null;
}

/**
 * Add/edit form for one schedule.
 *
 * A dialog rather than inline fields: a schedule carries a name, a prompt, a timezone and a cadence, and
 * rendering all of them per row made a list of schedules unreadable — the row shows only what identifies it.
 *
 * The cadence is picked (daily, weekly, …) rather than typed as a cron string, and the generated expression
 * is what `schedule/v1/cron` actually reads. The picker's own fields are stored beside it so reopening the
 * dialog restores the choice instead of parsing a cron string back into one.
 */
const AgentScheduleDialog = ({onClose, onSubmit, open, pending, schedule}: AgentScheduleDialogPropsI) => {
    // A new schedule opens on DAILY rather than CUSTOM_CRON: the picker exists so the common cadences need
    // no cron knowledge, and fromCadenceParameters({}) would land on the one kind that does.
    const [cadence, setCadence] = useState<AgentScheduleCadenceI>(() =>
        schedule ? fromCadenceParameters(schedule) : {frequencyKind: 'DAILY', timeOfDay: null}
    );
    const [errors, setErrors] = useState<AgentScheduleCadenceErrorsI>({});
    const [name, setName] = useState(schedule?.name ?? '');
    const [prompt, setPrompt] = useState(schedule?.prompt ?? '');
    const [timezone, setTimezone] = useState(schedule?.timezone || 'UTC');

    // Intl.supportedValuesOf is the browser's own tz database, so the list cannot drift from what the runtime
    // will accept. The fallback covers engines that do not implement it — a free-text field would let through
    // zone names the server then rejects at trigger-registration time.
    const timezones = useMemo(() => {
        const supportedValuesOf = (Intl as typeof Intl & {supportedValuesOf?: (key: string) => string[]})
            .supportedValuesOf;

        const zones = supportedValuesOf ? supportedValuesOf('timeZone') : [];

        return zones.includes('UTC') ? zones : ['UTC', ...zones];
    }, []);

    const handleSubmit = () => {
        const cadenceErrors = validateAgentScheduleCadence(cadence);

        setErrors(cadenceErrors);

        if (Object.keys(cadenceErrors).length > 0) {
            return;
        }

        onSubmit({
            ...toCadenceParameters(cadence),
            expression: toCronExpression(cadence),
            name,
            prompt,
            timezone,
        });
    };

    return (
        <Dialog onOpenChange={(nextOpen) => !nextOpen && onClose()} open={open}>
            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>{schedule ? 'Edit Schedule' : 'Add Schedule'}</DialogTitle>

                        <DialogDescription>Run the agent on a schedule with its own prompt.</DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <fieldset className="space-y-4 border-0 p-0">
                    <div className="space-y-1">
                        <Label htmlFor="agent-schedule-name">Name</Label>

                        <Input
                            id="agent-schedule-name"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="Daily digest"
                            value={name}
                        />
                    </div>

                    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                        <div className="space-y-1">
                            <Label htmlFor="agent-schedule-frequency-kind">Frequency</Label>

                            <Select
                                onValueChange={(selected) =>
                                    setCadence({...cadence, frequencyKind: selected as ScheduleFrequencyKindType})
                                }
                                value={cadence.frequencyKind}
                            >
                                <SelectTrigger id="agent-schedule-frequency-kind">
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

                        <div className="space-y-1">
                            <Label htmlFor="agent-schedule-timezone">Timezone</Label>

                            <Select onValueChange={setTimezone} value={timezone}>
                                <SelectTrigger id="agent-schedule-timezone">
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
                    </div>

                    <AgentScheduleFrequencyFields cadence={cadence} errors={errors} onChange={setCadence} />

                    <div className="space-y-1">
                        <Label htmlFor="agent-schedule-prompt">Prompt</Label>

                        <Textarea
                            id="agent-schedule-prompt"
                            onChange={(event) => setPrompt(event.target.value)}
                            placeholder="What should the agent do on this schedule?"
                            rows={3}
                            value={prompt}
                        />
                    </div>
                </fieldset>

                <DialogFooter>
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button disabled={pending} label={schedule ? 'Save' : 'Add'} onClick={handleSubmit} />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AgentScheduleDialog;
```

- [ ] **Step 4: Update `toScheduleProperties` in the card**

In `client/src/pages/automation/agents/components/detail/AgentScheduleCard.tsx`, replace the `toScheduleProperties` function (currently lines 80–92) with one that passes the cadence keys through:

```tsx
    const toScheduleProperties = (channel: AiAgentChannel): AgentSchedulePropertiesI => {
        const parameters = (channel.parameters ?? {}) as Record<string, unknown>;

        const properties: AgentSchedulePropertiesI = {
            expression: String(parameters.expression ?? ''),
            name: String(parameters.name ?? ''),
            prompt: String(parameters.prompt ?? ''),
            timezone: String(parameters.timezone || 'UTC'),
        };

        // The cadence keys are carried through verbatim so the dialog can restore the picker; the dialog,
        // not this card, is what interprets them.
        for (const key of CADENCE_PARAMETER_KEYS) {
            if (parameters[key] != null) {
                properties[key] = String(parameters[key]);
            }
        }

        return properties;
    };
```

Add `CADENCE_PARAMETER_KEYS` to `agentScheduleCron.ts`, exported directly above `toCadenceParameters` — it
lists exactly the keys that function writes and `fromCadenceParameters` reads, so a future cadence field is
added in one visible place:

```ts
export const CADENCE_PARAMETER_KEYS = [
    'cronExpression',
    'dayOfMonth',
    'dayOfWeek',
    'frequencyKind',
    'intervalMinutes',
    'minuteOfHour',
    'timeOfDay',
] as const;
```

and import it in the card from `@/pages/automation/agents/utils/agentScheduleCron` (sorted alphabetically
within the braces).

- [ ] **Step 5: Run the tests to verify they pass**

Run: `cd client && npx vitest run src/pages/automation/agents/components/detail/`
Expected: PASS — including the pre-existing `AgentScheduleCard.test.tsx`. If that file asserts on the old raw-cron input, update its assertions to the picker.

- [ ] **Step 6: Run the full client check**

Run: `cd client && npm run check`

- [ ] **Step 7: Commit**

```bash
git commit -m "732 client - Pick a frequency instead of typing a cron for agent schedules" -- \
  client/src/pages/automation/agents/components/detail/AgentScheduleDialog.tsx \
  client/src/pages/automation/agents/components/detail/AgentScheduleDialog.test.tsx \
  client/src/pages/automation/agents/components/detail/AgentScheduleCard.tsx \
  client/src/pages/automation/agents/components/detail/AgentScheduleCard.test.tsx
```

---

### Task 4: Optional schedule on the create-agent dialog

**Files:**
- Modify: `client/src/pages/automation/agents/components/AgentDialog.tsx`
- Test: `client/src/pages/automation/agents/components/AgentDialog.test.tsx`

**Interfaces:**
- Consumes: Task 1's `toCadenceParameters`, `toCronExpression`, `validateAgentScheduleCadence`; Task 2's `AgentScheduleFrequencyFields`; the generated `useAddAiAgentChannelMutation`.
- Produces: nothing new — `AgentDialog`'s props are unchanged.

- [ ] **Step 1: Write the failing test**

Create `client/src/pages/automation/agents/components/AgentDialog.test.tsx`. Mock the generated GraphQL hooks with `vi.hoisted` (module-scope consts are not initialised when a `vi.mock` factory runs):

```tsx
import AgentDialog from '@/pages/automation/agents/components/AgentDialog';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {addChannelMock, createAgentMock, updateAgentMock} = vi.hoisted(() => ({
    addChannelMock: vi.fn(),
    createAgentMock: vi.fn(),
    updateAgentMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAddAiAgentChannelMutation: () => ({mutate: addChannelMock}),
    useCreateAiAgentMutation: () => ({mutate: createAgentMock}),
    useUpdateAiAgentMutation: () => ({mutate: updateAgentMock}),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

const renderDialog = () =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <MemoryRouter>
                <AgentDialog onOpenChange={vi.fn()} open={true} />
            </MemoryRouter>
        </QueryClientProvider>
    );

describe('AgentDialog', () => {
    beforeEach(() => {
        addChannelMock.mockReset();
        createAgentMock.mockReset();
    });

    it('creates only the agent when no schedule is filled in', async () => {
        renderDialog();

        fireEvent.change(screen.getByPlaceholderText('Enter agent title'), {target: {value: 'Agent1'}});
        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(createAgentMock).toHaveBeenCalled());

        expect(addChannelMock).not.toHaveBeenCalled();
    });

    it('adds a schedule channel after creating the agent when one is filled in', async () => {
        createAgentMock.mockImplementation((_variables, options) =>
            options?.onSuccess?.({createAiAgent: {id: '42'}})
        );

        renderDialog();

        fireEvent.change(screen.getByPlaceholderText('Enter agent title'), {target: {value: 'Agent1'}});
        fireEvent.click(screen.getByRole('button', {name: 'Add a schedule'}));
        fireEvent.change(screen.getByLabelText('Prompt'), {target: {value: 'Summarise yesterday'}});
        fireEvent.change(screen.getByLabelText('Time'), {target: {value: '09:30'}});
        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(addChannelMock).toHaveBeenCalled());

        expect(addChannelMock).toHaveBeenCalledWith(
            expect.objectContaining({
                input: expect.objectContaining({
                    agentId: '42',
                    channelType: 'schedule',
                    parameters: expect.objectContaining({expression: '30 9 * * ?', frequencyKind: 'DAILY'}),
                }),
            })
        );
    });

    it('renders no schedule block when editing an existing agent', () => {
        render(
            <QueryClientProvider client={new QueryClient()}>
                <MemoryRouter>
                    <AgentDialog agent={{id: '1', title: 'Agent1'}} onOpenChange={vi.fn()} open={true} />
                </MemoryRouter>
            </QueryClientProvider>
        );

        expect(screen.queryByRole('button', {name: 'Add a schedule'})).not.toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd client && npx vitest run src/pages/automation/agents/components/AgentDialog.test.tsx`
Expected: FAIL — no "Add a schedule" button exists.

- [ ] **Step 3: Add the schedule block to the dialog**

In `client/src/pages/automation/agents/components/AgentDialog.tsx`:

1. Add imports (alphabetically sorted within each `{}`, and `useAddAiAgentChannelMutation` alongside the existing two mutation hooks):

```tsx
import AgentScheduleFrequencyFields from '@/pages/automation/agents/components/detail/AgentScheduleFrequencyFields';
import {
    AgentScheduleCadenceErrorsI,
    AgentScheduleCadenceI,
    toCadenceParameters,
    toCronExpression,
    validateAgentScheduleCadence,
} from '@/pages/automation/agents/utils/agentScheduleCron';
import {
    useAddAiAgentChannelMutation,
    useCreateAiAgentMutation,
    useUpdateAiAgentMutation,
} from '@/shared/middleware/graphql';
```

2. Add state next to the existing `useState` (hook order: all `useState` together, before the store hook):

```tsx
    const [cadence, setCadence] = useState<AgentScheduleCadenceI>({frequencyKind: 'DAILY', timeOfDay: null});
    const [cadenceErrors, setCadenceErrors] = useState<AgentScheduleCadenceErrorsI>({});
    const [schedulePrompt, setSchedulePrompt] = useState('');
    const [scheduleShown, setScheduleShown] = useState(false);
```

3. Add the mutation beside the other two:

```tsx
    const addAgentChannelMutation = useAddAiAgentChannelMutation();
```

4. In `onSubmit`'s create branch, validate the cadence before creating and add the channel in `onSuccess`. Replace the existing `createAgentMutation.mutate(...)` call with:

```tsx
        // A schedule is optional here: filling it in is exactly equivalent to adding one afterwards on the
        // detail page's schedule card — the same agent_channel row either way. The channel can only be added
        // once the agent exists, so it is created in onSuccess rather than in one round trip.
        const scheduleRequested = scheduleShown;

        if (scheduleRequested) {
            const errors = validateAgentScheduleCadence(cadence);

            setCadenceErrors(errors);

            if (Object.keys(errors).length > 0) {
                return;
            }
        }

        createAgentMutation.mutate(
            {
                input: {
                    description: values.description,
                    title: values.title,
                    workspaceId: currentWorkspaceId + '',
                },
            },
            {
                onSuccess: (data) => {
                    if (scheduleRequested) {
                        addAgentChannelMutation.mutate({
                            input: {
                                agentId: data.createAiAgent.id,
                                channelType: 'schedule',
                                parameters: {
                                    ...toCadenceParameters(cadence),
                                    expression: toCronExpression(cadence),
                                    name: values.title,
                                    prompt: schedulePrompt,
                                    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC',
                                },
                            },
                        });
                    }

                    queryClient.invalidateQueries({queryKey: ['aiAgents']});

                    setOpen(false);

                    navigate('/automation/agents/' + data.createAiAgent.id);
                },
            }
        );
```

5. Render the block between the description field and `DialogFooter`, create-mode only:

```tsx
                        {!agent &&
                            (scheduleShown ? (
                                <fieldset className="space-y-4 border-0 p-0">
                                    <AgentScheduleFrequencyFields
                                        cadence={cadence}
                                        errors={cadenceErrors}
                                        onChange={setCadence}
                                    />

                                    <div className="space-y-1">
                                        <Label htmlFor="agent-schedule-prompt">Prompt</Label>

                                        <Textarea
                                            id="agent-schedule-prompt"
                                            onChange={(event) => setSchedulePrompt(event.target.value)}
                                            placeholder="What should the agent do on this schedule?"
                                            rows={3}
                                            value={schedulePrompt}
                                        />
                                    </div>
                                </fieldset>
                            ) : (
                                <Button
                                    label="Add a schedule"
                                    onClick={() => setScheduleShown(true)}
                                    type="button"
                                    variant="outline"
                                />
                            ))}
```

Add `import {Label} from '@/components/ui/label';` if it is not already imported.

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd client && npx vitest run src/pages/automation/agents/components/AgentDialog.test.tsx`
Expected: PASS, 3 tests.

- [ ] **Step 5: Run the full client check**

Run: `cd client && npm run check`

- [ ] **Step 6: Commit**

```bash
git commit -m "732 client - Allow defining a schedule when creating an agent" -- \
  client/src/pages/automation/agents/components/AgentDialog.tsx \
  client/src/pages/automation/agents/components/AgentDialog.test.tsx
```

---

### Task 5: Scheduled filter on the agents list

**Files:**
- Modify: `client/src/graphql/automation/agent/aiAgents.graphql`
- Modify: `client/src/shared/middleware/graphql.ts` (regenerated — do not hand-edit)
- Modify: `client/src/pages/automation/agents/Agents.tsx:71-95,174`
- Modify: `client/src/pages/automation/agents/components/AgentsLeftSidebarNav.tsx`
- Modify: `client/src/pages/automation/agents/components/AgentsFilterTitle.tsx`
- Create: `client/src/pages/automation/agents/utils/isScheduledAgent.ts`
- Test: `client/src/pages/automation/agents/Agents.test.tsx` (existing — extend)

**Interfaces:**
- Consumes: the `channels` field on the `AiAgent` GraphQL type (already declared in the schema; `aiAgent.graphql` selects it).
- Produces: `isScheduledAgent(agent: {channels?: {channelType: string}[] | null}): boolean`; the `?filter=scheduled` search param; `AgentsFilterTitle` prop `scheduled?: boolean`; `AgentsLeftSidebarNav` prop `currentFilter?: string | null`.

- [ ] **Step 1: Add `channels` to the list query and regenerate**

Edit `client/src/graphql/automation/agent/aiAgents.graphql`, adding after the `elements` block:

```graphql
        channels {
            id
            channelType
            parameters
        }
```

Run: `cd client && npx graphql-codegen`
Expected: `src/shared/middleware/graphql.ts` updates; `AiAgent` in the generated types now carries `channels`.

- [ ] **Step 2: Write the failing test**

`Agents.test.tsx` mocks `react-router-dom` with a **fixed** empty `URLSearchParams`, so the search param must
first be made settable. Change the `vi.hoisted` block to add a mutable holder and the router mock to read it:

```tsx
const {
    copilotEnabled,
    mockInvalidateAgentQueries,
    mockRegisterPostTurn,
    mockSetContext,
    mockSetCopilotPanelOpen,
    navigateMock,
    searchParams,
} = vi.hoisted(() => ({
    copilotEnabled: {value: true},
    mockInvalidateAgentQueries: vi.fn(),
    mockRegisterPostTurn: vi.fn(),
    mockSetContext: vi.fn(),
    mockSetCopilotPanelOpen: vi.fn(),
    navigateMock: vi.fn(),
    searchParams: {value: new URLSearchParams()},
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => navigateMock,
    useSearchParams: () => [searchParams.value, vi.fn()],
}));
```

and reset it in the existing `beforeEach` with `searchParams.value = new URLSearchParams();`. Then add the
test:

```tsx
    it('narrows the list to scheduled agents when the scheduled filter is active', () => {
        searchParams.value = new URLSearchParams('filter=scheduled');

        mockUseAiAgentsQuery.mockReturnValue(
            queryResult({
                aiAgents: [
                    {
                        channels: [{channelType: 'schedule', id: '10', parameters: {expression: '30 9 * * ?'}}],
                        elements: [],
                        id: '1',
                        tags: [],
                        title: 'Agent1',
                    },
                    {
                        channels: [{channelType: 'chat', id: '11', parameters: {}}],
                        elements: [],
                        id: '2',
                        tags: [],
                        title: 'Agent2',
                    },
                ],
            })
        );

        wrap(<Agents />);

        expect(screen.getByText('Agent1')).toBeInTheDocument();
        expect(screen.queryByText('Agent2')).not.toBeInTheDocument();
    });
```

Note that this file stubs `AgentsLeftSidebarNav` to `null`, so the Scheduled nav item itself is not covered
here — it is verified by hand in the manual verification section rather than by a test that would have to
un-stub the sidebar and provide a real router.

- [ ] **Step 3: Run the test to verify it fails**

Run: `cd client && npx vitest run src/pages/automation/agents/Agents.test.tsx`
Expected: FAIL — both agents render, because the filter is not implemented.

- [ ] **Step 4: Add the predicate**

Create `client/src/pages/automation/agents/utils/isScheduledAgent.ts`:

```ts
/**
 * A scheduled agent is one with at least one `schedule` channel — the agent_channel row that
 * AiAgentWorkflowGenerator turns into a `schedule/v1/cron` trigger on its workflow. There is no separate
 * scheduled flag anywhere; the channel is the whole of it.
 */
const isScheduledAgent = (agent: {channels?: ({channelType: string} | null)[] | null}): boolean =>
    (agent.channels ?? []).some((channel) => channel?.channelType === 'schedule');

export default isScheduledAgent;
```

- [ ] **Step 5: Wire the filter into the page**

In `client/src/pages/automation/agents/Agents.tsx`:

```tsx
    const filter = searchParams.get('filter');
```

beside the existing `agentIdFilter` / `tagIdFilter` reads, then inside the `filteredAgents` `useMemo`, before the tag branch:

```tsx
        if (filter === 'scheduled') {
            return agents.filter((agent) => isScheduledAgent(agent));
        }
```

adding `filter` to the `useMemo` dependency array and importing `isScheduledAgent from '@/pages/automation/agents/utils/isScheduledAgent'`. Pass `currentFilter={filter}` to `AgentsLeftSidebarNav` and `scheduled={filter === 'scheduled'}` to `AgentsFilterTitle`.

- [ ] **Step 6: Add the sidebar item and the filter title label**

In `AgentsLeftSidebarNav.tsx`, accept `currentFilter?: string | null` in its props interface, take it in the destructure, make the All Agents item's `current` also require `!currentFilter`, and render directly beneath it:

```tsx
                        {filterMode && (
                            <LeftSidebarNavItem
                                item={{
                                    current: currentFilter === 'scheduled',
                                    id: 'scheduled-agents',
                                    name: 'Scheduled',
                                }}
                                toLink="?filter=scheduled"
                            />
                        )}
```

In `AgentsFilterTitle.tsx`, add `scheduled?: boolean` to the props and render `Scheduled` as the badge label when it is set:

```tsx
const AgentsFilterTitle = ({agentName, scheduled, tagName}: AgentsFilterTitlePropsI) => (
    <div className="space-x-1">
        <span className="text-sm text-muted-foreground uppercase">{`Filter by ${tagName ? 'tag' : 'agent'}:`}</span>

        <Badge
            label={agentName ?? tagName ?? (scheduled ? 'Scheduled' : 'All Agents')}
            styleType="secondary-filled"
            weight="semibold"
        />
    </div>
);
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `cd client && npx vitest run src/pages/automation/agents/`
Expected: PASS.

- [ ] **Step 8: Run the full client check**

Run: `cd client && npm run check`

- [ ] **Step 9: Commit — operations and generated file separately**

```bash
git commit -m "732 client - Select agent channels in the agents list query" -- \
  client/src/graphql/automation/agent/aiAgents.graphql
git commit -m "732 client - Regenerate GraphQL types for agent channels" -- \
  client/src/shared/middleware/graphql.ts
git commit -m "732 client - Add a Scheduled filter to the agents list" -- \
  client/src/pages/automation/agents/Agents.tsx \
  client/src/pages/automation/agents/Agents.test.tsx \
  client/src/pages/automation/agents/components/AgentsLeftSidebarNav.tsx \
  client/src/pages/automation/agents/components/AgentsFilterTitle.tsx \
  client/src/pages/automation/agents/utils/isScheduledAgent.ts
```

---

### Task 6: Scheduled marker on list rows

**Files:**
- Modify: `client/src/pages/automation/agents/components/agent-list/AgentListItem.tsx:129-131`
- Test: `client/src/pages/automation/agents/components/agent-list/AgentListItem.test.tsx`

**Interfaces:**
- Consumes: `isScheduledAgent` from Task 5.
- Produces: nothing.

- [ ] **Step 1: Write the failing test**

Create `client/src/pages/automation/agents/components/agent-list/AgentListItem.test.tsx` following the mocking style of the sibling agent tests (mock `@/shared/middleware/graphql` hooks with `vi.hoisted`, wrap in `QueryClientProvider` + `MemoryRouter`):

```tsx
    it('marks an agent that has a schedule channel', () => {
        renderItem({channels: [{channelType: 'schedule', id: '1', parameters: {expression: '30 9 * * ?'}}]});

        expect(screen.getByLabelText('Scheduled')).toBeInTheDocument();
    });

    it('renders no marker for an agent with no schedule channel', () => {
        renderItem({channels: [{channelType: 'chat', id: '1', parameters: {}}]});

        expect(screen.queryByLabelText('Scheduled')).not.toBeInTheDocument();
    });
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd client && npx vitest run src/pages/automation/agents/components/agent-list/AgentListItem.test.tsx`
Expected: FAIL — no element labelled "Scheduled".

- [ ] **Step 3: Render the marker**

In `AgentListItem.tsx`, add `CalendarClockIcon` to the `lucide-react` import (keeping the named imports alphabetically sorted), import `isScheduledAgent`, and replace the title line:

```tsx
                <span className="flex items-center gap-1.5 font-semibold">
                    {agent.title}

                    {isScheduledAgent(agent) && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <CalendarClockIcon aria-label="Scheduled" className="size-4 text-muted-foreground" />
                            </TooltipTrigger>

                            <TooltipContent>{scheduleSummary}</TooltipContent>
                        </Tooltip>
                    )}
                </span>
```

with the summary derived above the `return` (after the other `useMemo` calls, per hook order):

```tsx
    const scheduleSummary = useMemo(
        () =>
            (agent.channels ?? [])
                .filter((channel) => channel?.channelType === 'schedule')
                .map((channel) => {
                    const parameters = (channel?.parameters ?? {}) as Record<string, unknown>;

                    return [parameters.name, parameters.expression].filter(Boolean).join(' — ');
                })
                .join(', '),
        [agent.channels]
    );
```

Import `Tooltip`, `TooltipContent`, `TooltipTrigger` from `@/components/ui/tooltip` if the file does not already.

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd client && npx vitest run src/pages/automation/agents/components/agent-list/AgentListItem.test.tsx`
Expected: PASS.

- [ ] **Step 5: Run the full client check**

Run: `cd client && npm run check`

- [ ] **Step 6: Commit**

```bash
git commit -m "732 client - Mark scheduled agents in the agents list" -- \
  client/src/pages/automation/agents/components/agent-list/AgentListItem.tsx \
  client/src/pages/automation/agents/components/agent-list/AgentListItem.test.tsx
```

---

### Task 7: Delete the AI Hub tasks client surface

Everything built above now exists, so the hub surface can go.

**Files:**
- Delete: `client/src/ee/pages/automation/ai-hub/tasks/` (all 17 files, including `schedules/`, `hooks/`, `dialogs/` and their tests)
- Delete: the task GraphQL operations under `client/src/graphql/` (find with `grep -rl "aiHubTask\|AiHubTask" client/src/graphql`)
- Modify: `client/src/routes.tsx:83-84,1115-1150`
- Modify: `client/src/ee/pages/automation/ai-hub/chats/AiHubChatsSidebar.tsx:680-745`
- Modify: `client/src/ee/pages/automation/ai-hub/AiHubPanel.tsx:15`
- Modify: `client/src/shared/middleware/graphql.ts` (regenerated)

**Interfaces:**
- Consumes: nothing.
- Produces: nothing — this is a pure removal.

- [ ] **Step 1: Find every referent**

Run: `cd client && grep -rn "ai-hub/tasks\|AiHubTask\|aiHubTask\|useAiHubTaskQuery" src | grep -v "^src/ee/pages/automation/ai-hub/tasks/"`
Record the list; every hit outside the tasks tree must be resolved in this task.

- [ ] **Step 2: Delete the tree and the operations**

```bash
rm -r client/src/ee/pages/automation/ai-hub/tasks
rm $(grep -rl "aiHubTask\|AiHubTask" client/src/graphql)
```

- [ ] **Step 3: Remove the routes**

In `client/src/routes.tsx`, delete the two lazy imports (`AiHubTasksPage`, `AiHubTaskFormPage`) and all three route objects with paths `ai-hub/tasks`, `ai-hub/tasks/new`, `ai-hub/tasks/:taskId/edit`.

- [ ] **Step 4: Remove the sidebar entry**

In `AiHubChatsSidebar.tsx`, delete the `isOnAiHubTasks` constant, the `<Link … to="/automation/ai-hub/tasks">` block containing the "Scheduled" label, and the comment paragraphs that describe them. In `AiHubPanel.tsx`, delete the `useAiHubTaskQuery` import and every use of it; if that leaves a variable unused, remove the variable rather than silencing the lint.

- [ ] **Step 5: Regenerate GraphQL types**

Run: `cd client && npx graphql-codegen`

Note: the generated file still declares the task types until the server schema files are deleted in Task 10. That is expected — unused generated types do not fail the build. Re-run codegen once more at the end of Task 10.

- [ ] **Step 6: Verify nothing references the deleted tree**

Run: `cd client && grep -rn "ai-hub/tasks\|AiHubTask\|aiHubTask" src ; npm run check`
Expected: the grep returns nothing; `npm run check` passes.

- [ ] **Step 7: Commit**

```bash
git commit -m "732 client - Remove the AI Hub tasks surface" -- \
  client/src/ee/pages/automation/ai-hub client/src/graphql client/src/routes.tsx \
  client/src/shared/middleware/graphql.ts
```

---

### Task 8: Delete the task tool callbacks and the task subagent

**Files:**
- Delete: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/{CreateAiHubTask,UpdateAiHubTask,DeleteAiHubTask,CloneAiHubTask,ListAiHubTasks,OpenAiHubTaskTab,SetAiHubTaskSchedule}ToolCallback.java`, `TaskScheduleToolSupport.java`, and their tests
- Delete: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/TaskSubAgentConfiguration.java` + `TaskSubAgentConfigurationTest.java`
- Delete: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_task_agent.txt`
- Modify: `…/ee/ai/hub/tool/AiHubAgentType.java` (drop `TASK_AGENT`)
- Modify: `…/ee/ai/hub/tool/OpenResourceTabToolCallback.java` (drop the task branch)
- Modify: `…/ee/ai/hub/config/AiHubConfiguration.java`, `…/config/AiHubSubAgentMcpContributorConfiguration.java` + its test
- Modify: the ASK/BUILD prompt resources that mention `task_agent` / `openAiHubTaskTab` / `createAiHubTask`

**Interfaces:**
- Consumes: nothing.
- Produces: nothing.

- [ ] **Step 1: Enumerate the referents**

Run:
```bash
grep -rln "TASK_AGENT\|task_agent\|AiHubTaskToolCallback\|openAiHubTaskTab\|createAiHubTask\|listAiHubTasks\|setAiHubTaskSchedule" \
  server --include='*.java' --include='*.txt' | grep -v '/bin/\|/build/'
```

- [ ] **Step 2: Delete the callbacks, the subagent config and the prompt**

```bash
cd server/ee/libs/ai/ai-hub/ai-hub-service/src
rm main/java/com/bytechef/ee/ai/hub/tool/{Create,Update,Delete,Clone}AiHubTaskToolCallback.java \
   main/java/com/bytechef/ee/ai/hub/tool/ListAiHubTasksToolCallback.java \
   main/java/com/bytechef/ee/ai/hub/tool/OpenAiHubTaskTabToolCallback.java \
   main/java/com/bytechef/ee/ai/hub/tool/SetAiHubTaskScheduleToolCallback.java \
   main/java/com/bytechef/ee/ai/hub/tool/TaskScheduleToolSupport.java \
   main/java/com/bytechef/ee/ai/hub/config/TaskSubAgentConfiguration.java \
   main/resources/prompt_task_agent.txt
rm test/java/com/bytechef/ee/ai/hub/tool/{CreateAiHubTaskToolCallbackTest,DeleteAiHubTaskToolCallbackTest,ListAiHubTasksToolCallbackTest,OpenAiHubTaskTabToolCallbackTest,TaskScheduleToolSupportTest}.java \
   test/java/com/bytechef/ee/ai/hub/config/TaskSubAgentConfigurationTest.java
```

- [ ] **Step 3: Remove every registration**

In `AiHubConfiguration.java`, delete the `toolCallbacks.add(...)` lines for the task callbacks, the `createTaskAgentToolCallback` / `wrapDelegate` wiring for the task specialist, and any `ObjectProvider<AiHubTaskService>` parameters that become unused. In `AiHubAgentType.java`, delete the `TASK_AGENT` constant. In `OpenResourceTabToolCallback.java`, delete the `task` case from the `type` dispatch and from its Javadoc list of supported types. In `AiHubSubAgentMcpContributorConfiguration.java` (+ test), drop the task specialist from the contributed list.

Delete the paragraphs naming `task_agent`, `openAiHubTaskTab`, `createAiHubTask`, `listAiHubTasks` and `setAiHubTaskSchedule` from the ASK/BUILD prompt files found in Step 1. A prompt naming a tool that no longer exists produces "No ToolCallback found" at runtime — this is not cosmetic.

- [ ] **Step 4: Compile**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1
./gradlew compileJava compileTestJava --continue > /tmp/compile.log 2>&1
echo "exit=$?"
grep '^> Task .* FAILED' /tmp/compile.log
```
Expected: `exit=0`, no FAILED lines. Errors here name the remaining registrations — fix and re-run.

- [ ] **Step 5: Commit**

```bash
git commit -m "732 Remove the AI Hub task tools and the task subagent" -- server/ee/libs/ai/ai-hub
```

---

### Task 9: Remove the TASK chat kind and the task overlay

**Files:**
- Modify: `…/ee/ai/hub/chat/AiHubChatKind.java` (delete `TASK`; `AGENT_CHAT` becomes ordinal 2)
- Modify: `…/ee/ai/hub/chat/AiHubChat.java` (drop `aiHubTaskId`), `AiHubChatService.java` + `AiHubChatServiceImpl.java` (drop `createAiHubTaskChat`)
- Modify: `…/ee/ai/hub/agent/AiHubRoutingAgent.java` (drop `applyAiHubTaskOverlay`), `…/agent/AiHubSpringAIAgent.java` (drop `appendAiHubTaskContext`), `…/util/AiHubStateKeys.java` (drop the two task keys)
- Delete: `…/ee/ai/hub/agent/AiHubScheduledChatDispatcher.java`
- Delete: `…/src/test/java/com/bytechef/ee/ai/hub/agent/AiHubSpringAIAgentTaskContextTest.java`
- Modify: the `EnumOrdinalStabilityTest` entry for `AiHubChatKind`; `AiHubChatServiceTest`, `AiHubChatRepositoryIntTest`, `AiHubRoutingAgentTest`
- Modify: `…/src/main/resources/graphql/ai-hub-chat.graphqls` (drop the `TASK` enum value and the task fields)

**Interfaces:**
- Consumes: nothing.
- Produces: `AiHubChatKind` = `STANDARD(0), WORKFLOW_CHAT(1), AGENT_CHAT(2)`; `isWebhookBridged()` unchanged in behaviour.

- [ ] **Step 1: Confirm the unreleased premise still holds**

Run: `git ls-tree -r --name-only v0.31.3 | grep -c "ai/hub"`
Expected: `0`. If this is non-zero, STOP — the ordinal shift is then illegal and needs a different approach.

- [ ] **Step 2: Update the enum and the stability test**

In `AiHubChatKind.java` delete the `TASK` constant and every doc sentence describing it. In `EnumOrdinalStabilityTest`, update the pinned ordinals for `AiHubChatKind` to `STANDARD=0, WORKFLOW_CHAT=1, AGENT_CHAT=2`.

- [ ] **Step 3: Run the stability test to verify it passes**

```bash
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-api:test --tests '*EnumOrdinalStabilityTest' > /tmp/enum.log 2>&1
echo "exit=$?"
```
Expected: `exit=0`.

- [ ] **Step 4: Remove the field, the factory method and the overlay**

Delete `aiHubTaskId` and its accessors from `AiHubChat`; delete `createAiHubTaskChat` from `AiHubChatService` and its impl; delete `applyAiHubTaskOverlay` from `AiHubRoutingAgent` and its call site; delete `appendAiHubTaskContext` from `AiHubSpringAIAgent` and its call site; delete the `aiHubTaskInstructions` / `aiHubTaskTitle` keys from `AiHubStateKeys`; `rm` `AiHubScheduledChatDispatcher.java` and `AiHubSpringAIAgentTaskContextTest.java`. Remove the TASK cases from `AiHubChatServiceTest`, `AiHubChatRepositoryIntTest` and `AiHubRoutingAgentTest`, and the `TASK` value plus task fields from `ai-hub-chat.graphqls`.

- [ ] **Step 5: Compile and run the module's tests**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --continue > /tmp/test.log 2>&1
echo "exit=$?"
grep '^> Task .* FAILED' /tmp/test.log
```
Expected: `exit=0`.

- [ ] **Step 6: Commit**

```bash
git commit -m "732 Remove the TASK chat kind and its system-prompt overlay" -- server/ee/libs/ai/ai-hub
```

---

### Task 10: Delete the task package, GraphQL and audit events

**Files:**
- Delete: `server/ee/libs/ai/ai-hub/ai-hub-api/src/main/java/com/bytechef/ee/ai/hub/task/` and `…/ai-hub-api/src/test/java/com/bytechef/ee/ai/hub/task/`
- Delete: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/task/` and `…/src/test/java/com/bytechef/ee/ai/hub/task/`
- Delete: `…/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/web/graphql/AiHubTaskGraphQlController.java`
- Delete: `…/ai-hub-service/src/main/resources/graphql/ai-hub-task.graphqls`, `ai-hub-task-schedule.graphqls`
- Modify: `…/ee/ai/hub/audit/AiHubAuditEvent.java` (drop the `AI_HUB_TASK_*` values)
- Modify: `client/codegen.ts` if it lists the deleted `.graphqls` paths; `client/src/shared/middleware/graphql.ts` (regenerated)

**Interfaces:**
- Consumes: nothing.
- Produces: nothing.

- [ ] **Step 1: Delete the package and the GraphQL surface**

```bash
rm -r server/ee/libs/ai/ai-hub/ai-hub-api/src/main/java/com/bytechef/ee/ai/hub/task \
      server/ee/libs/ai/ai-hub/ai-hub-api/src/test/java/com/bytechef/ee/ai/hub/task \
      server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/task \
      server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/task
rm server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/web/graphql/AiHubTaskGraphQlController.java \
   server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/graphql/ai-hub-task.graphqls \
   server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/graphql/ai-hub-task-schedule.graphqls
```

- [ ] **Step 2: Remove the audit events**

In `AiHubAuditEvent.java`, delete the `AI_HUB_TASK_CREATED`, `AI_HUB_TASK_UPDATED`, `AI_HUB_TASK_DELETED` and `AI_HUB_TASK_SCHEDULE_FIRED` constants. Keep the class Javadoc paragraph explaining that rows written by builds predating the conversation→chat rename carry `AI_HUB_TASK_*` values — those historical rows still exist and the note is why an unknown value can appear.

- [ ] **Step 3: Compile**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1
./gradlew compileJava compileTestJava --continue > /tmp/compile.log 2>&1
echo "exit=$?"
grep '^> Task .* FAILED' /tmp/compile.log
```
Expected: `exit=0`.

- [ ] **Step 4: Regenerate client types now the schema is gone**

Run: `cd client && npx graphql-codegen && npm run check`
Expected: the task types disappear from `graphql.ts`; the check passes. If codegen errors on a missing schema path, remove that path from `client/codegen.ts`.

- [ ] **Step 5: Commit**

```bash
git commit -m "732 Remove the AI Hub task package and its GraphQL surface" -- \
  server/ee/libs/ai/ai-hub client/codegen.ts client/src/shared/middleware/graphql.ts
```

---

### Task 11: Delete the task Liquibase changelogs

**Files:**
- Delete: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/config/liquibase/changelog/automation/aihub/*ai_hub_task*.xml` and `20260504000002_ai_hub_chat_add_task_id.xml`
- Delete: the `*ai_hub_task*` changelogs under `…/changelog/automation/aihub_execution/`

**Interfaces:**
- Consumes: nothing.
- Produces: a schema with no `ai_hub_task*` tables and no `ai_hub_chat.ai_hub_task_id` column.

- [ ] **Step 1: List what will be deleted**

Run: `ls server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/config/liquibase/changelog/automation/aihub{,_execution}/ | grep task`

- [ ] **Step 2: Delete them, plus their stale build copies**

```bash
cd server/ee/libs/ai/ai-hub/ai-hub-service
rm src/main/resources/config/liquibase/changelog/automation/aihub/*ai_hub_task*.xml \
   src/main/resources/config/liquibase/changelog/automation/aihub/20260504000002_ai_hub_chat_add_task_id.xml
rm -f src/main/resources/config/liquibase/changelog/automation/aihub_execution/*ai_hub_task*.xml
rm -rf build/resources bin/main/config
```

The build/bin copies matter: Liquibase reads from the build output, so a deleted changelog still on the classpath is still applied.

`master.xml` needs no edit — the `aihub` and `aihub_execution` directories are pulled in with `includeAll` and `errorIfMissingOrEmpty="false"`.

- [ ] **Step 3: Verify the schema still builds from scratch**

```bash
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:testIntegration --continue > /tmp/int.log 2>&1
echo "exit=$?"
grep '^> Task .* FAILED' /tmp/int.log
```
Expected: `exit=0`. Testcontainers builds the schema from nothing, so a green run is direct evidence that no remaining changelog references the dropped tables. (Do not try to verify this with `bootRun --spring.profiles.active=liquibase` — that profile exits 0 having applied nothing.)

- [ ] **Step 4: Resync the local dev database**

Run: `scripts/dev/sync-local-schema-after-collapse.sh`
This patches both the schema drift and the stale changelog md5sums; it is idempotent.

- [ ] **Step 5: Commit**

```bash
git commit -m "732 Drop the AI Hub task changelogs" -- \
  server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/config/liquibase
```

---

### Task 12: Delete the Quartz hub scheduler

**Files:**
- Delete: `server/libs/platform/platform-scheduler/platform-scheduler-api/src/main/java/com/bytechef/platform/scheduler/AgentScheduler.java`
- Delete: `…/platform-scheduler-api/src/main/java/com/bytechef/platform/scheduler/event/AgentScheduleFiredEvent.java`
- Delete: `…/platform-scheduler-impl/src/main/java/com/bytechef/platform/scheduler/QuartzAgentScheduler.java`
- Delete: `…/platform-scheduler-impl/src/main/java/com/bytechef/platform/scheduler/job/AgentScheduleJob.java`
- Delete: `…/platform-scheduler-impl/src/test/java/com/bytechef/platform/scheduler/QuartzAgentSchedulerIntTest.java`
- Modify: any scheduler `*Configuration` that declares the `AgentScheduler` bean

**Interfaces:**
- Consumes: nothing.
- Produces: nothing. `TriggerScheduler`, `AlertScheduler`, `ConnectionRefreshScheduler` and `ExportScheduler` are untouched.

- [ ] **Step 1: Prove there are no remaining consumers**

Run: `grep -rn "AgentScheduler\|AgentScheduleFiredEvent\|AgentScheduleJob" server --include='*.java' | grep -v '/bin/\|/build/'`
Expected: hits only in the five files above and any bean declaration. If anything else appears, resolve it before deleting.

- [ ] **Step 2: Delete them and unregister the bean**

Remove the five files, then delete the `AgentScheduler` `@Bean` method (and its now-unused imports) from the scheduler configuration class the grep surfaced.

- [ ] **Step 3: Compile and run the module's tests**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1
./gradlew :server:libs:platform:platform-scheduler:platform-scheduler-impl:test --continue > /tmp/sched.log 2>&1
echo "exit=$?"
grep '^> Task .* FAILED' /tmp/sched.log
```
Expected: `exit=0`.

- [ ] **Step 4: Commit**

```bash
git commit -m "732 Remove the Quartz agent scheduler" -- server/libs/platform/platform-scheduler
```

---

### Task 13: Update the documentation and run the full verification

**Files:**
- Modify: `CLAUDE.md` — the "Tasks (\"Scheduled\" in the UI)" section, the "Chat kinds and threadId conventions" section, the "Task system-prompt overlay" section, and every subagent list naming `task_agent`
- Modify: `.agents/agents.md` — document that a `schedule` channel is how an agent runs on a cron, and that the frequency picker generates a 5-field expression
- Modify: the spec's own status line

**Interfaces:**
- Consumes: nothing.
- Produces: nothing.

- [ ] **Step 1: Find every doc reference**

Run: `grep -rn "AiHubTask\|ai_hub_task\|task_agent\|Scheduled\" in the UI\|openAiHubTaskTab" CLAUDE.md docs/ | grep -v superpowers/specs/2026-08-17`

- [ ] **Step 2: Rewrite the CLAUDE.md sections**

Delete the whole `#### Tasks ("Scheduled" in the UI)` and `#### Task system-prompt overlay` subsections. In "Chat kinds and threadId conventions", remove the `TASK` bullet and renumber `AGENT_CHAT` to ordinal 2, keeping the warning about testing bridged kinds via `isWebhookBridged()`. Remove `task_agent` from the three subagent lists (the tier-3 architecture list, the "wired for asking" list, and the management-MCP contributor list). Add one line under the Agents section:

```markdown
An agent runs on a cron through a `schedule` channel row (`ChannelDefinitions.schedule()` →
`schedule/v1/cron`), never through a platform scheduler — the client's frequency picker
(`agentScheduleCron.ts`) generates the **5-field** expression the trigger expects, because
`ScheduleCronTrigger` prepends the seconds field before Quartz parses it. The picker's own cadence
fields are stored beside `expression` on the same channel row so the choice round-trips.
```

- [ ] **Step 3: Mark the spec implemented**

Change the spec's `**Status:**` line to `Implemented 2026-08-17`.

- [ ] **Step 4: Run the full verification**

```bash
cd client && npm run check
cd .. && ./gradlew spotlessApply > /tmp/spotless.log 2>&1
./gradlew check --continue > /tmp/check.log 2>&1
echo "exit=$?"
grep '^> Task .* FAILED' /tmp/check.log
```
Expected: the client check passes and `exit=0` with no FAILED lines.

- [ ] **Step 5: Confirm nothing survived**

```bash
grep -rn "AiHubTask\|ai_hub_task\|TASK_AGENT\|task_agent\|AgentScheduler\|AiHubScheduledChatDispatcher" \
  server client/src --include='*.java' --include='*.ts' --include='*.tsx' --include='*.xml' --include='*.graphqls' \
  | grep -v '/bin/\|/build/'
```
Expected: no output.

- [ ] **Step 6: Commit**

```bash
git commit -m "732 Document scheduling as an AI Agent schedule channel" -- \
  CLAUDE.md .agents/agents.md docs/superpowers/specs/2026-08-17-delegate-scheduled-to-ai-agent-design.md
```

---

## Manual verification

The automated checks cover code shape, not runtime behaviour. Before calling this done, run the server
(`./gradlew -p server/apps/server-app bootRun` with the dev infra up) and:

1. Create an agent from the Agents list with a schedule of "Every 2 minutes" and a prompt. Confirm it lands
   on the detail page with one schedule listed.
2. Reopen that schedule from `AgentScheduleCard`. Confirm the dialog shows "Every X minutes" with `2`, not a
   raw cron string — this is the round-trip the stored cadence fields exist for.
3. Publish and deploy the agent. Confirm the generated workflow carries a `schedule/v1/cron` trigger whose
   expression is `0/2 * * * ?`, and that a run appears in Executions within two minutes. A trigger-registration
   failure here means the expression has the wrong number of fields.
4. Click **Scheduled** in the agents sidebar. Confirm only that agent is listed and its row shows the clock
   marker with the cron in its tooltip.
5. Confirm `/automation/ai-hub` no longer shows a "Scheduled" entry and that `/automation/ai-hub/tasks`
   resolves to the app's not-found handling rather than a blank page.
