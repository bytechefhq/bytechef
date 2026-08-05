import Button from '@/components/Button/Button';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Skeleton} from '@/components/ui/skeleton';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Textarea} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import ModelPicker from '@/shared/components/ai/model-picker/ModelPicker';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {AiHubPersonalAgentResourceKind, ScheduleLifecycleKind} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ArrowLeftIcon, InfoIcon} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import {toast} from 'sonner';

import AiHubTasksSidebar from '../tasks/AiHubTasksSidebar';
import AiHubPersonalAgentResourcesCard, {
    AiHubPersonalAgentPendingResourceI,
    AiHubPersonalAgentPendingToolI,
} from './AiHubPersonalAgentResourcesCard';
import {
    useAddAiHubPersonalAgentResourceMutation,
    useAddAiHubPersonalAgentToolMutation,
    useAiHubPersonalAgentQuery,
    useCreateAiHubPersonalAgentMutation,
    useUpdateAiHubPersonalAgentMutation,
} from './hooks/useAiHubPersonalAgents';
import AiHubPersonalAgentScheduleTab, {
    AiHubPersonalAgentScheduleTabValueI,
    buildDefaultScheduleValue,
    fromExistingSchedule,
} from './schedules/AiHubPersonalAgentScheduleTab';
import {useSetAiHubPersonalAgentSchedule} from './schedules/hooks/useAiHubPersonalAgentSchedule';

/**
 * Server-side regex for the slug. Mirrored client-side so the slug-preview matches the canonical form before
 * submit; the server still has the final say (the {@code @Check} constraint catches anything the client misses).
 */
const NAME_REGEX = /^[a-z0-9_-]{1,64}$/;

/**
 * Client-side mirror of the server's slugify routine. Lowercases, replaces non-slug chars with hyphens, trims
 * hyphens, and truncates to 64 chars. Surfaced as the auto-derived slug on the create form so the user knows
 * the canonical identifier their title produces.
 */
function slugify(input: string): string {
    const lower = input.toLowerCase();
    const collapsed = lower.replace(/[^a-z0-9_-]+/g, '-');
    const trimmed = collapsed.replace(/^-+|-+$/g, '');
    const truncated = trimmed.length > 64 ? trimmed.substring(0, 64).replace(/-+$/g, '') : trimmed;

    return truncated;
}

const buildSchedulePayload = (value: AiHubPersonalAgentScheduleTabValueI) => ({
    cronExpression: value.cronExpression ?? null,
    dayOfMonth: value.dayOfMonth ?? null,
    dayOfWeek: value.dayOfWeek ?? null,
    enabled: value.enabled,
    frequencyKind: value.frequencyKind,
    intervalMinutes: value.intervalMinutes ?? null,
    lifecycleKind: value.lifecycleKind,
    maxRuns: value.lifecycleKind === ScheduleLifecycleKind.NumberOfRuns ? (value.maxRuns ?? null) : null,
    minuteOfHour: value.minuteOfHour ?? null,
    prompt: value.prompt,
    startDate: value.startDate || null,
    timeOfDay: value.timeOfDay ?? null,
    title: value.title,
    zoneId: value.zoneId,
});

const scheduleChanged = (
    initial: AiHubPersonalAgentScheduleTabValueI | null,
    current: AiHubPersonalAgentScheduleTabValueI
): boolean => {
    if (initial == null) {
        return current.enabled || current.title.length > 0 || current.prompt.length > 0;
    }

    return JSON.stringify(buildSchedulePayload(initial)) !== JSON.stringify(buildSchedulePayload(current));
};

/**
 * Standalone full-page form for Personal Agents — both create ({@code /automation/ai-hub/personal-agents/new})
 * and edit ({@code /automation/ai-hub/personal-agents/:agentId/edit}). Replaces the previous dialog flow
 * so the form has full-page real estate for fields + tools.
 *
 * <p>The form is constrained to a centered, max-w-2xl column rather than stretching to the body width — at
 * desktop widths long {@code Input} / {@code Textarea} fields running edge-to-edge read as a wall of grey
 * boxes, and the controls become harder to scan when the eye has to traverse 1500+ pixels per row. Centering
 * keeps the affordance compact and matches the reading-width conventions of the rest of the platform's
 * settings forms.</p>
 *
 * <p>Tools picker: the previous {@code AiHubPersonalAgentToolsPicker} component was removed; this form now renders
 * existing attached tools as a read-only list with per-row remove buttons (edit mode only). Adding tools is
 * deferred to a future revision that will plug in the workflow-editor's component picker — the connector
 * grid with filter + descriptions — for parity with the AI Agent simple-mode tool selector.</p>
 */
const AiHubPersonalAgentForm = () => {
    const navigate = useNavigate();
    const {agentId: agentIdParam} = useParams<{agentId?: string}>();

    const agentIdFromRoute = agentIdParam ? Number(agentIdParam) : undefined;
    const isEditMode = agentIdFromRoute !== undefined && Number.isFinite(agentIdFromRoute);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data: agent, isLoading: isAgentLoading} = useAiHubPersonalAgentQuery(
        agentIdFromRoute,
        currentWorkspaceId ?? 0,
        isEditMode && currentWorkspaceId != null
    );

    const [title, setTitle] = useState('');
    const [description, setDescription] = useState('');
    const [instructions, setInstructions] = useState('');
    // Per-agent LLM override (provider, model). Either both empty (use workspace default) or both set. The server
    // validates this strictly; the form mirrors the constraint visually but accepts user input freely so we don't
    // block on typos — server response surfaces the validation error if the typed pair is rejected.
    const [llmProvider, setLlmProvider] = useState('');
    const [llmModel, setLlmModel] = useState('');
    // Local tools state used only in create mode. The picker hands new entries here; on save we bulk-attach
    // via addToolMutation. Edit mode reads tools off the agent query and the picker mutates server-side
    // directly, so this stays empty in that flow.
    const [pendingTools, setPendingTools] = useState<AiHubPersonalAgentPendingToolI[]>([]);
    // Local resources state used only in create mode — analogous to pendingTools above.
    const [pendingResources, setPendingResources] = useState<AiHubPersonalAgentPendingResourceI[]>([]);
    const [schedule, setSchedule] = useState<AiHubPersonalAgentScheduleTabValueI>(buildDefaultScheduleValue());
    const [initialSchedule, setInitialSchedule] = useState<AiHubPersonalAgentScheduleTabValueI | null>(null);

    const createMutation = useCreateAiHubPersonalAgentMutation();
    const setScheduleMutation = useSetAiHubPersonalAgentSchedule();
    const updateMutation = useUpdateAiHubPersonalAgentMutation();
    const addToolMutation = useAddAiHubPersonalAgentToolMutation();
    const addResourceMutation = useAddAiHubPersonalAgentResourceMutation();

    useEffect(() => {
        if (isEditMode && agent) {
            // Hydrate the form with the loaded agent. We don't reset on dependency changes the way the dialog
            // had to — the route remount cycle handles state lifetimes for us.
            setTitle(agent.title ?? '');
            setDescription(agent.description ?? '');
            setInstructions(agent.instructions ?? '');
            setLlmProvider(agent.llmProvider ?? '');
            setLlmModel(agent.llmModel ?? '');

            if (agent.schedule) {
                const hydrated = fromExistingSchedule(agent.schedule);

                setSchedule(hydrated);
                setInitialSchedule(hydrated);
            } else {
                setSchedule(buildDefaultScheduleValue());
                setInitialSchedule(null);
            }
        }
    }, [agent, isEditMode]);

    // Slug is auto-derived from the title and never surfaced as an editable field. The user picks a display
    // title and the server stores a slug for routing; the auto-preview confirms what the slug will be.
    const computedSlug = useMemo(() => {
        if (isEditMode) {
            return agent?.name ?? '';
        }

        return slugify(title.trim());
    }, [agent, isEditMode, title]);

    const titleValid = isEditMode || (computedSlug.length > 0 && NAME_REGEX.test(computedSlug));

    const goBackToList = () => {
        navigate('/automation/ai-hub/personal-agents');
    };

    const handleSave = async () => {
        if (currentWorkspaceId == null) {
            return;
        }

        // The (llmProvider, llmModel) pair must both be empty or both filled. Block submit otherwise — the server
        // returns 400 with a typed message but catching it client-side avoids the round-trip + cryptic toast.
        const providerTrimmed = llmProvider.trim();
        const modelTrimmed = llmModel.trim();

        if ((providerTrimmed.length === 0) !== (modelTrimmed.length === 0)) {
            toast.error('LLM provider and LLM model must both be set or both empty.');

            return;
        }

        try {
            if (isEditMode && agent) {
                // Tri-state on the override pair:
                //   - existing override + both fields empty in the form → CLEAR (pass empty strings).
                //   - existing override + same values in the form → preserve (pass null).
                //   - existing override + new values → apply (pass new strings).
                //   - no existing override + values entered → apply (pass new strings).
                // We encode "preserve" as null; everything else as the user-typed values.
                const existingProvider = agent.llmProvider ?? '';
                const existingModel = agent.llmModel ?? '';
                const overridePairChanged = providerTrimmed !== existingProvider || modelTrimmed !== existingModel;

                await updateMutation.mutateAsync({
                    input: {
                        description: description.trim() || null,
                        id: String(agent.id),
                        instructions: instructions.trim() || null,
                        llmModel: overridePairChanged ? modelTrimmed : null,
                        llmProvider: overridePairChanged ? providerTrimmed : null,
                        title: title.trim() || null,
                        workspaceId: String(currentWorkspaceId),
                    },
                });

                if (scheduleChanged(initialSchedule, schedule)) {
                    await setScheduleMutation.mutateAsync({
                        input: {
                            aiHubPersonalAgentId: String(agent.id),
                            schedule: buildSchedulePayload(schedule),
                            workspaceId: String(currentWorkspaceId),
                        },
                    });
                }

                toast.success(`Agent "${title.trim() || agent.name}" updated`);

                goBackToList();

                return;
            }

            if (!titleValid) {
                toast.error('Display title must contain at least one letter or digit');

                return;
            }

            const createResult = await createMutation.mutateAsync({
                input: {
                    description: description.trim() || null,
                    environment: currentEnvironmentId,
                    instructions: instructions.trim() || null,
                    llmModel: modelTrimmed || null,
                    llmProvider: providerTrimmed || null,
                    name: computedSlug,
                    title: title.trim() || null,
                    workspaceId: String(currentWorkspaceId),
                },
            });

            // The codegen result shape is the union of create-personal-agent fragments — the row sits on
            // `createAiHubPersonalAgent`. Cast minimally so we can read the new agent's id without dragging the
            // generated GraphQL types into this file.
            const createdAgentId = (createResult as {createAiHubPersonalAgent: {id: string}}).createAiHubPersonalAgent
                .id;
            const failedAttaches: string[] = [];

            // Bulk-attach pending tools sequentially so any individual failure surfaces with the offending
            // (component, operation) pair. Promise.all would consolidate errors and lose attribution. The
            // cost is N round-trips, but typical agents have a handful of tools and create isn't a hot path.
            for (const tool of pendingTools) {
                try {
                    await addToolMutation.mutateAsync({
                        input: {
                            aiHubPersonalAgentId: createdAgentId,
                            componentName: tool.componentName,
                            componentVersion: tool.componentVersion,
                            operationName: tool.operationName,
                            workspaceId: String(currentWorkspaceId),
                        },
                    });
                } catch (toolError) {
                    const message = toolError instanceof Error ? toolError.message : String(toolError);

                    console.error(
                        `[AiHubPersonalAgentForm] Failed to attach tool ${tool.componentName}:${tool.operationName} to agent ${createdAgentId}:`,
                        toolError
                    );

                    failedAttaches.push(`${tool.componentName}:${tool.operationName} (${message})`);
                }
            }

            // Bulk-attach pending resources sequentially — same pattern and failure-aggregation as the
            // pending-tools loop above. Individual failures push to the same failedAttaches array so the
            // partial-failure toast reports both tool and resource failures in one message.
            for (const pendingResource of pendingResources) {
                try {
                    await addResourceMutation.mutateAsync({
                        input: {
                            aiHubPersonalAgentId: createdAgentId,
                            kind: pendingResource.kind as AiHubPersonalAgentResourceKind,
                            resourceId: pendingResource.resourceId,
                            resourceName: pendingResource.resourceName,
                            workspaceId: String(currentWorkspaceId),
                        },
                    });
                } catch (resourceError) {
                    const message = resourceError instanceof Error ? resourceError.message : String(resourceError);

                    console.error(
                        `[AiHubPersonalAgentForm] Failed to attach resource ${pendingResource.kind}:${pendingResource.resourceId} to agent ${createdAgentId}:`,
                        resourceError
                    );

                    failedAttaches.push(`${pendingResource.kind}:${pendingResource.resourceName} (${message})`);
                }
            }

            let scheduleError: string | null = null;

            if (schedule.enabled || schedule.title.length > 0 || schedule.prompt.length > 0) {
                try {
                    await setScheduleMutation.mutateAsync({
                        input: {
                            aiHubPersonalAgentId: createdAgentId,
                            schedule: buildSchedulePayload(schedule),
                            workspaceId: String(currentWorkspaceId),
                        },
                    });
                } catch (error) {
                    scheduleError = error instanceof Error ? error.message : String(error);
                }
            }

            if (failedAttaches.length > 0 || scheduleError != null) {
                const parts: string[] = [];

                if (failedAttaches.length > 0) {
                    parts.push(
                        `${failedAttaches.length} item${failedAttaches.length === 1 ? '' : 's'} failed to attach: ${failedAttaches.join(', ')}`
                    );
                }

                if (scheduleError != null) {
                    parts.push(`schedule failed to save: ${scheduleError}`);
                }

                toast.error(`Agent created, but ${parts.join('; ')}. Edit the agent to retry.`);
            } else {
                toast.success(`Agent "${title.trim() || computedSlug}" created`);
            }

            goBackToList();
        } catch (error) {
            toast.error(error instanceof Error ? error.message : 'Failed to save personal agent');
        }
    };

    const handleAddPendingTool = (tool: AiHubPersonalAgentPendingToolI) => {
        setPendingTools((previous) => {
            // Dedup against already-pending (componentName, componentVersion, operationName). Without this,
            // the same action picked twice produces two attach attempts and the second 409s on the server's
            // unique constraint, surfacing as a confusing partial-failure toast on save.
            const alreadyPending = previous.some(
                (entry) =>
                    entry.componentName === tool.componentName &&
                    entry.componentVersion === tool.componentVersion &&
                    entry.operationName === tool.operationName
            );

            if (alreadyPending) {
                return previous;
            }

            return [...previous, tool];
        });
    };

    const handleRemovePendingTool = (index: number) => {
        setPendingTools((previous) => previous.filter((_, i) => i !== index));
    };

    const handleAddPendingResource = (resource: AiHubPersonalAgentPendingResourceI) => {
        setPendingResources((previous) => {
            // Dedup against already-pending (kind, resourceId). Without this, picking the same resource twice
            // produces two attach attempts and the second 409s on the server's unique constraint.
            const alreadyPending = previous.some(
                (entry) => entry.kind === resource.kind && entry.resourceId === resource.resourceId
            );

            if (alreadyPending) {
                return previous;
            }

            return [...previous, resource];
        });
    };

    const handleRemovePendingResource = (index: number) => {
        setPendingResources((previous) => previous.filter((_, i) => i !== index));
    };

    const isSubmitDisabled =
        createMutation.isPending ||
        updateMutation.isPending ||
        addToolMutation.isPending ||
        addResourceMutation.isPending ||
        (!isEditMode && !titleValid);

    const submitLabel =
        createMutation.isPending ||
        updateMutation.isPending ||
        addToolMutation.isPending ||
        addResourceMutation.isPending
            ? 'Saving...'
            : isEditMode
              ? 'Save changes'
              : 'Create agent';

    const headerTitle = isEditMode ? `Edit ${agent?.title ?? agent?.name ?? 'agent'}` : 'New personal agent';

    return (
        <LayoutContainer
            header={
                <div className="flex w-full items-center gap-2 px-6 py-3">
                    <Button
                        aria-label="Back to personal agents"
                        icon={<ArrowLeftIcon />}
                        onClick={goBackToList}
                        size="icon"
                        variant="ghost"
                    />

                    <div className="flex min-w-0 flex-1 items-center gap-1.5">
                        <h2 className="truncate text-base font-medium">{headerTitle}</h2>

                        <Tooltip>
                            <TooltipTrigger asChild>
                                <InfoIcon
                                    aria-label="What are personal agents?"
                                    className="size-4 shrink-0 text-muted-foreground"
                                />
                            </TooltipTrigger>

                            <TooltipContent className="max-w-sm" side="bottom">
                                Personal agents apply their instructions on every turn of a chat. Use them to define a
                                focused assistant — code reviewer, research helper, copywriter — once and reuse it
                                across sessions.
                            </TooltipContent>
                        </Tooltip>
                    </div>

                    <EnvironmentSelect />
                </div>
            }
            leftSidebarBody={<AiHubTasksSidebar />}
            leftSidebarHeader={<Header position="sidebar" title="AI Hub" />}
            leftSidebarWidth="64"
        >
            <div className="flex w-full flex-1 flex-col p-6">
                {isEditMode && isAgentLoading ? (
                    // Loading skeleton stays inside the same centered max-w-2xl column the form uses so the
                    // page doesn't jump when the data lands and the real form renders.
                    <div className="mx-auto flex w-full max-w-2xl flex-col gap-3">
                        <Skeleton className="h-8 w-1/3" />

                        <Skeleton className="h-8 w-full" />

                        <Skeleton className="h-32 w-full" />
                    </div>
                ) : isEditMode && !agent ? (
                    <p className="mx-auto max-w-2xl text-sm text-muted-foreground">
                        Agent not found in this workspace. It may have been deleted.
                    </p>
                ) : (
                    <Tabs defaultValue="overview">
                        <TabsList>
                            <TabsTrigger value="overview">Overview</TabsTrigger>

                            <TabsTrigger value="schedule">Schedule</TabsTrigger>
                        </TabsList>

                        <TabsContent value="overview">
                            {/* `mx-auto max-w-2xl` centers the form on the page and constrains it to a comfortable
                        reading width. Without max-w the inputs would stretch edge-to-edge on widescreens
                        and the form would lose its visual identity as a focused settings surface. */}

                            <fieldset className="mx-auto flex w-full max-w-2xl flex-col gap-5 border-0">
                                <div className="flex flex-col gap-1.5">
                                    <Label htmlFor="personal-agent-title">Display title</Label>

                                    <Input
                                        id="personal-agent-title"
                                        maxLength={255}
                                        onChange={(event) => setTitle(event.target.value)}
                                        placeholder="Research Assistant"
                                        value={title}
                                    />

                                    {!isEditMode && computedSlug && (
                                        <p className="text-xs text-muted-foreground">
                                            Slug: <code className="font-mono">{computedSlug}</code>
                                        </p>
                                    )}
                                </div>

                                <div className="flex flex-col gap-1.5">
                                    <Label htmlFor="personal-agent-description">Description</Label>

                                    <Input
                                        id="personal-agent-description"
                                        maxLength={1024}
                                        onChange={(event) => setDescription(event.target.value)}
                                        placeholder="Helps with literature reviews and citation lookups."
                                        value={description}
                                    />
                                </div>

                                <div className="flex flex-col gap-1.5">
                                    <Label htmlFor="personal-agent-instructions">Instructions</Label>

                                    <Textarea
                                        className="min-h-40 font-mono text-xs"
                                        id="personal-agent-instructions"
                                        maxLength={65536}
                                        onChange={(event) => setInstructions(event.target.value)}
                                        placeholder="Always cite sources. Prefer peer-reviewed publications. Reply in plain English."
                                        value={instructions}
                                    />

                                    <p className="text-xs text-muted-foreground">
                                        Appended to the agent&apos;s system prompt every turn. Workspace defaults still
                                        apply.
                                    </p>
                                </div>

                                {/*
                                 * Per-agent LLM provider + model — uses the shared {@link ModelPicker} (also used in Copilot +
                                 * AI Hub chat panels). The picker reads the workspace's enabled AI Gateway providers + models
                                 * from the same GraphQL queries the old two-Select setup used; the visible affordance is a
                                 * single cascading dropdown instead of two side-by-side selects. The workspace-default sentinel
                                 * is intentionally omitted here so the agent always names a concrete model (the trigger reads
                                 * "Select model" until one is picked).
                                 */}

                                <fieldset className="flex flex-col gap-2 border-0">
                                    <Label htmlFor="personal-agent-llm-picker">LLM provider and model</Label>

                                    <ModelPicker
                                        environment={currentEnvironmentId ?? 0}
                                        layout="full"
                                        onChange={(provider, model) => {
                                            setLlmProvider(provider ?? '');
                                            setLlmModel(model ?? '');
                                        }}
                                        selectedModel={llmModel || null}
                                        selectedProvider={llmProvider || null}
                                    />

                                    <p className="text-xs text-muted-foreground">
                                        The LLM provider and model this agent uses.
                                    </p>
                                </fieldset>

                                {/*
                                 * Resources section: unified component picker + reference-kind resource picker.
                                 * In create mode the picker hands pending entries to local state; on save we
                                 * bulk-attach after the agent row exists. In edit mode the picker mutates
                                 * server-side directly. See AiHubPersonalAgentResourcesCard for the
                                 * discriminated props.
                                 */}

                                {isEditMode && agent != null && currentWorkspaceId != null ? (
                                    <AiHubPersonalAgentResourcesCard
                                        aiHubPersonalAgentId={agent.id}
                                        environmentId={currentEnvironmentId ?? 0}
                                        resources={agent.resources ?? []}
                                        tools={agent.tools ?? []}
                                        workspaceId={currentWorkspaceId}
                                    />
                                ) : (
                                    <AiHubPersonalAgentResourcesCard
                                        environmentId={currentEnvironmentId ?? 0}
                                        onAddPendingResource={handleAddPendingResource}
                                        onAddPendingTool={handleAddPendingTool}
                                        onRemovePendingResource={handleRemovePendingResource}
                                        onRemovePendingTool={handleRemovePendingTool}
                                        pendingResources={pendingResources}
                                        pendingTools={pendingTools}
                                        workspaceId={currentWorkspaceId ?? 0}
                                    />
                                )}

                                <div className="flex justify-end gap-2 pt-2">
                                    <Button
                                        disabled={createMutation.isPending || updateMutation.isPending}
                                        label="Cancel"
                                        onClick={goBackToList}
                                        variant="outline"
                                    />

                                    <Button disabled={isSubmitDisabled} label={submitLabel} onClick={handleSave} />
                                </div>
                            </fieldset>
                        </TabsContent>

                        <TabsContent value="schedule">
                            <AiHubPersonalAgentScheduleTab
                                existingSchedule={agent?.schedule ?? null}
                                onChange={setSchedule}
                                onRemove={
                                    isEditMode && agent
                                        ? async () => {
                                              await setScheduleMutation.mutateAsync({
                                                  input: {
                                                      aiHubPersonalAgentId: String(agent.id),
                                                      schedule: null,
                                                      workspaceId: String(currentWorkspaceId ?? ''),
                                                  },
                                              });

                                              setSchedule(buildDefaultScheduleValue());
                                              setInitialSchedule(null);

                                              toast.success('Schedule removed');
                                          }
                                        : undefined
                                }
                                value={schedule}
                            />
                        </TabsContent>
                    </Tabs>
                )}
            </div>
        </LayoutContainer>
    );
};

export default AiHubPersonalAgentForm;
