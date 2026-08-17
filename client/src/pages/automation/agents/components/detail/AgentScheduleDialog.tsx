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
import {useMemo, useState} from 'react';

export interface AgentSchedulePropertiesI {
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
 * A dialog rather than the inline fields this replaced: a schedule carries four values, and rendering all of
 * them per row made a list of schedules unreadable — the row now shows only what identifies it, its name and
 * cron expression.
 */
const AgentScheduleDialog = ({onClose, onSubmit, open, pending, schedule}: AgentScheduleDialogPropsI) => {
    const [expression, setExpression] = useState(schedule?.expression ?? '');
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

    return (
        <Dialog onOpenChange={(nextOpen) => !nextOpen && onClose()} open={open}>
            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>{schedule ? 'Edit Schedule' : 'Add Schedule'}</DialogTitle>

                        <DialogDescription>Run the agent on a cron expression with its own prompt.</DialogDescription>
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
                            <Label htmlFor="agent-schedule-expression">Cron expression</Label>

                            <Input
                                id="agent-schedule-expression"
                                onChange={(event) => setExpression(event.target.value)}
                                placeholder="0 9 * * *"
                                value={expression}
                            />
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

                    <Button
                        disabled={!expression.trim() || pending}
                        label={schedule ? 'Save' : 'Add'}
                        onClick={() => onSubmit({expression, name, prompt, timezone})}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AgentScheduleDialog;
