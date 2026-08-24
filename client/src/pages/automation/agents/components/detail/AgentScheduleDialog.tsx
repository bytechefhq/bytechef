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
    const [promptError, setPromptError] = useState<string | undefined>(undefined);
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

    // The elements, not just the zone names. Radix renders SelectContent's children even while the select is
    // closed (into a detached fragment, so each item can register its native option), so without a stable
    // element identity to bail out on, every keystroke in Name/Cron expression/Prompt re-reconciles ~420
    // nodes — measured at ~110ms per character. Memoising costs one array and leaves only the one-time mount,
    // which is what stopped this dialog's specs from timing out under a loaded machine.
    const timezoneItems = useMemo(
        () =>
            timezones.map((zone) => (
                <SelectItem key={zone} value={zone}>
                    {zone}
                </SelectItem>
            )),
        [timezones]
    );

    const handleSubmit = () => {
        const cadenceErrors = validateAgentScheduleCadence(cadence);
        // A blank prompt is the one invalid schedule the server accepts: AiAgentWorkflowGenerator bakes the
        // prompt into branch_in's envelope as a literal, and MapUtils.getRequiredString rejects only a MISSING
        // key — "" passes. The schedule then fires on time and hands the agent nothing, which reads as the cron
        // being broken. This check is the only gate.
        const promptMissing = prompt.trim() === '';

        setErrors(cadenceErrors);
        setPromptError(promptMissing ? 'Prompt is required' : undefined);

        if (promptMissing || Object.keys(cadenceErrors).length > 0) {
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

                                <SelectContent>{timezoneItems}</SelectContent>
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

                        {promptError && <p className="text-xs text-destructive">{promptError}</p>}
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
