/**
 * A scheduled agent is one with at least one `schedule` channel — the agent_channel row that
 * AiAgentWorkflowGenerator turns into a `schedule/v1/cron` trigger on its workflow. There is no separate
 * scheduled flag anywhere; the channel is the whole of it.
 */
const isScheduledAgent = (agent: {channels?: ({channelType: string} | null)[] | null}): boolean =>
    (agent.channels ?? []).some((channel) => channel?.channelType === 'schedule');

export default isScheduledAgent;
