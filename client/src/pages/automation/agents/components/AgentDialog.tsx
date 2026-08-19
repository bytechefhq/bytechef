import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import AgentScheduleFrequencyFields from '@/pages/automation/agents/components/detail/AgentScheduleFrequencyFields';
import {
    AgentScheduleCadenceErrorsI,
    AgentScheduleCadenceI,
    SCHEDULE_FREQUENCY_KINDS,
    SCHEDULE_FREQUENCY_LABELS,
    ScheduleFrequencyKindType,
    toCadenceParameters,
    toCronExpression,
    validateAgentScheduleCadence,
} from '@/pages/automation/agents/utils/agentScheduleCron';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    useAddAiAgentChannelMutation,
    useCreateAiAgentMutation,
    useUpdateAiAgentMutation,
} from '@/shared/middleware/graphql';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {useNavigate} from 'react-router-dom';
import {z} from 'zod';

const formSchema = z.object({
    description: z.string(),
    title: z.string().min(1, {message: 'Title is required'}),
});

type FormValuesType = z.infer<typeof formSchema>;

// Shared by the initial state and the on-close reset. Safe as a module-level constant because every cadence
// update replaces the object rather than mutating it.
const INITIAL_CADENCE: AgentScheduleCadenceI = {frequencyKind: 'DAILY', timeOfDay: null};

interface AgentDialogProps {
    /** Present = edit an existing agent's name and description; absent = create a new one. */
    agent?: {description?: string | null; id: string; title: string};
    onOpenChange?: (open: boolean) => void;
    open?: boolean;
    triggerNode?: ReactNode;
}

const AgentDialog = ({agent, onOpenChange, open: controlledOpen, triggerNode}: AgentDialogProps) => {
    const [uncontrolledOpen, setUncontrolledOpen] = useState(false);
    const [cadence, setCadence] = useState<AgentScheduleCadenceI>(INITIAL_CADENCE);
    const [cadenceErrors, setCadenceErrors] = useState<AgentScheduleCadenceErrorsI>({});
    const [schedulePrompt, setSchedulePrompt] = useState('');
    const [schedulePromptError, setSchedulePromptError] = useState<string | undefined>(undefined);
    const [scheduleShown, setScheduleShown] = useState(false);

    // Controlled when opened from a menu item (which unmounts its own trigger on select), uncontrolled when it
    // owns a trigger node of its own.
    const open = controlledOpen ?? uncontrolledOpen;

    const resetSchedule = () => {
        setCadence(INITIAL_CADENCE);
        setCadenceErrors({});
        setSchedulePrompt('');
        setSchedulePromptError(undefined);
        setScheduleShown(false);
    };

    // This component stays mounted alongside its trigger, so closing the dialog only unmounts Radix's content —
    // the schedule state would otherwise survive into the next agent created from the same trigger.
    const setOpen = (nextOpen: boolean) => {
        if (!nextOpen) {
            resetSchedule();
        }

        (onOpenChange ?? setUncontrolledOpen)(nextOpen);
    };

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const form = useForm<FormValuesType>({
        defaultValues: {
            description: agent?.description ?? '',
            title: agent?.title ?? '',
        },
        resolver: zodResolver(formSchema),
    });

    // Declared on the hook rather than passed to mutate(): the dialog navigates to the new agent as soon as it
    // is created, and TanStack Query v5 drops mutate()-level callbacks once the observer unmounts. The request
    // itself still completes, but without this the detail page would render an empty Schedules card.
    const addAgentChannelMutation = useAddAiAgentChannelMutation({
        onSuccess: () => invalidateAgentQueries(queryClient),
    });
    const createAgentMutation = useCreateAiAgentMutation();
    const updateAgentMutation = useUpdateAiAgentMutation();

    const onSubmit = (values: FormValuesType) => {
        if (agent) {
            updateAgentMutation.mutate(
                {input: {description: values.description, id: agent.id, title: values.title}},
                {
                    onSuccess: () => {
                        queryClient.invalidateQueries({queryKey: ['aiAgent']});
                        queryClient.invalidateQueries({queryKey: ['aiAgents']});

                        setOpen(false);
                    },
                }
            );

            return;
        }

        // A schedule is optional here: filling it in is exactly equivalent to adding one afterwards on the
        // detail page's schedule card — the same agent_channel row either way. The channel can only be added
        // once the agent exists, so it is created in onSuccess rather than in one round trip.
        const scheduleRequested = scheduleShown;

        if (scheduleRequested) {
            const errors = validateAgentScheduleCadence(cadence);
            const promptMissing = schedulePrompt.trim() === '';

            setCadenceErrors(errors);
            setSchedulePromptError(promptMissing ? 'Prompt is required' : undefined);

            if (promptMissing || Object.keys(errors).length > 0) {
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

        form.reset({});
    };

    return (
        <Dialog onOpenChange={setOpen} open={open}>
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>{agent ? 'Edit Agent' : 'Create Agent'}</DialogTitle>

                        <DialogDescription>
                            {agent
                                ? "Change the agent's name and description."
                                : 'Create a new agent by filling out the form below.'}
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <Form {...form}>
                    <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)}>
                        <FormField
                            control={form.control}
                            name="title"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Title</FormLabel>

                                    <FormControl>
                                        <Input placeholder="Enter agent title" {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="description"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Description</FormLabel>

                                    <FormControl>
                                        <Textarea placeholder="Enter a description for the agent" rows={3} {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        {!agent &&
                            (scheduleShown ? (
                                <fieldset className="space-y-4 border-0 p-0">
                                    <div className="flex items-center justify-between">
                                        <Label htmlFor="agent-schedule-frequency-kind">Frequency</Label>

                                        <Button
                                            label="Remove schedule"
                                            onClick={resetSchedule}
                                            size="sm"
                                            type="button"
                                            variant="ghost"
                                        />
                                    </div>

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

                                        {schedulePromptError && (
                                            <p className="text-xs text-destructive">{schedulePromptError}</p>
                                        )}
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

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button label="Cancel" type="button" variant="outline" />
                            </DialogClose>

                            <Button label="Save" type="submit" />
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default AgentDialog;
