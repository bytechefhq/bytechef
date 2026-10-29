import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogClose,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
    DialogTrigger,
} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
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
import getAgentPath from '@/pages/automation/agents/utils/getAgentPath';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    useAddAiAgentChannelMutation,
    useCreateAiAgentMutation,
    useUpdateAiAgentMutation,
} from '@/shared/middleware/graphql';
import {ProjectKeys, useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {useNavigate} from 'react-router-dom';
import {z} from 'zod';

const formSchema = z.object({
    description: z.string(),
    projectId: z.string().optional(),
    title: z.string().min(1, {message: 'Title is required'}),
});

type FormValuesType = z.infer<typeof formSchema>;

const INITIAL_CADENCE: AgentScheduleCadenceI = {frequencyKind: 'DAILY', timeOfDay: null};

interface AgentDialogProps {
    agent?: {description?: string | null; id: string; title: string};
    createdRedirectPath?: string;
    onOpenChange?: (open: boolean) => void;
    open?: boolean;
    projectId?: number;
    scheduleRequired?: boolean;
    triggerNode?: ReactNode;
}

const AgentDialog = ({
    agent,
    createdRedirectPath,
    onOpenChange,
    open: controlledOpen,
    projectId,
    scheduleRequired = false,
    triggerNode,
}: AgentDialogProps) => {
    const [uncontrolledOpen, setUncontrolledOpen] = useState(false);
    const [cadence, setCadence] = useState<AgentScheduleCadenceI>(INITIAL_CADENCE);
    const [cadenceErrors, setCadenceErrors] = useState<AgentScheduleCadenceErrorsI>({});
    const [schedulePrompt, setSchedulePrompt] = useState('');
    const [schedulePromptError, setSchedulePromptError] = useState<string | undefined>(undefined);
    const [scheduleShown, setScheduleShown] = useState(scheduleRequired);

    const open = controlledOpen ?? uncontrolledOpen;

    const resetSchedule = () => {
        setCadence(INITIAL_CADENCE);
        setCadenceErrors({});
        setSchedulePrompt('');
        setSchedulePromptError(undefined);
        setScheduleShown(scheduleRequired);
    };

    const setOpen = (nextOpen: boolean) => {
        if (!nextOpen) {
            resetSchedule();
        }

        (onOpenChange ?? setUncontrolledOpen)(nextOpen);
    };

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const {data: projects} = useGetWorkspaceProjectsQuery({id: currentWorkspaceId!}, !agent && !projectId);

    const form = useForm<FormValuesType>({
        defaultValues: {
            description: agent?.description ?? '',
            projectId: projectId ? String(projectId) : 'new',
            title: agent?.title ?? '',
        },
        resolver: zodResolver(formSchema),
    });

    const addAgentChannelMutation = useAddAiAgentChannelMutation({
        onSuccess: () => invalidateAgentQueries(queryClient),
    });
    const createAgentMutation = useCreateAiAgentMutation();
    const updateAgentMutation = useUpdateAiAgentMutation();

    const createTitle = scheduleRequired ? 'Create Scheduled Agent' : 'Create Agent';
    const createDescription = scheduleRequired
        ? 'Create an agent that runs on its own schedule by filling out the form below.'
        : 'Create a new agent by filling out the form below.';

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
                    projectId: values.projectId && values.projectId !== 'new' ? values.projectId : undefined,
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

                    queryClient.invalidateQueries({queryKey: ProjectKeys.filteredProjects({id: currentWorkspaceId!})});

                    setOpen(false);

                    navigate(createdRedirectPath ?? getAgentPath(data.createAiAgent));
                },
            }
        );

        form.reset({});
    };

    return (
        <Dialog onOpenChange={setOpen} open={open}>
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent>
                <DialogMain>
                    <DialogHeader
                        description={agent ? "Change the agent's name and description." : createDescription}
                        title={agent ? 'Edit Agent' : createTitle}
                    />

                    <Form {...form}>
                        <form className="flex min-h-0 flex-1 flex-col" onSubmit={form.handleSubmit(onSubmit)}>
                            <DialogBody>
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

                                {!agent && !projectId && (
                                    <FormField
                                        control={form.control}
                                        name="projectId"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Project</FormLabel>

                                                <FormControl>
                                                    <Select onValueChange={field.onChange} value={field.value}>
                                                        <SelectTrigger>
                                                            <SelectValue />
                                                        </SelectTrigger>

                                                        <SelectContent>
                                                            <SelectItem value="new">
                                                                New project named after this agent
                                                            </SelectItem>

                                                            {projects?.map((project) => (
                                                                <SelectItem key={project.id} value={String(project.id)}>
                                                                    {project.name}
                                                                </SelectItem>
                                                            ))}
                                                        </SelectContent>
                                                    </Select>
                                                </FormControl>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />
                                )}

                                <FormField
                                    control={form.control}
                                    name="description"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel>Description</FormLabel>

                                            <FormControl>
                                                <Textarea
                                                    placeholder="Enter a description for the agent"
                                                    rows={3}
                                                    {...field}
                                                />
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

                                                {!scheduleRequired && (
                                                    <Button
                                                        label="Remove schedule"
                                                        onClick={resetSchedule}
                                                        size="sm"
                                                        type="button"
                                                        variant="ghost"
                                                    />
                                                )}
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
                            </DialogBody>

                            <DialogFooter>
                                <DialogClose asChild>
                                    <Button label="Cancel" type="button" variant="outline" />
                                </DialogClose>

                                <Button label="Save" type="submit" />
                            </DialogFooter>
                        </form>
                    </Form>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AgentDialog;
