import Button from '@/components/Button/Button';
import {Dialog, DialogBody, DialogContent, DialogFooter, DialogHeader, DialogMain} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
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
    schedule?: AgentSchedulePropertiesI | null;
}

const AgentScheduleDialog = ({onClose, onSubmit, open, pending, schedule}: AgentScheduleDialogPropsI) => {
    const [cadence, setCadence] = useState<AgentScheduleCadenceI>(() =>
        schedule ? fromCadenceParameters(schedule) : {frequencyKind: 'DAILY', timeOfDay: null}
    );
    const [errors, setErrors] = useState<AgentScheduleCadenceErrorsI>({});
    const [name, setName] = useState(schedule?.name ?? '');
    const [prompt, setPrompt] = useState(schedule?.prompt ?? '');
    const [promptError, setPromptError] = useState<string | undefined>(undefined);
    const [timezone, setTimezone] = useState(schedule?.timezone || 'UTC');

    const timezones = useMemo(() => {
        const supportedValuesOf = (Intl as typeof Intl & {supportedValuesOf?: (key: string) => string[]})
            .supportedValuesOf;

        const zones = supportedValuesOf ? supportedValuesOf('timeZone') : [];

        return zones.includes('UTC') ? zones : ['UTC', ...zones];
    }, []);

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
                <DialogMain>
                    <DialogHeader
                        description="Run the agent on a schedule with its own prompt."
                        title={schedule ? 'Edit Schedule' : 'Add Schedule'}
                    />

                    <DialogBody>
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
                                            setCadence({
                                                ...cadence,
                                                frequencyKind: selected as ScheduleFrequencyKindType,
                                            })
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
                    </DialogBody>

                    <DialogFooter>
                        <Button label="Cancel" onClick={onClose} variant="outline" />

                        <Button disabled={pending} label={schedule ? 'Save' : 'Add'} onClick={handleSubmit} />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AgentScheduleDialog;
