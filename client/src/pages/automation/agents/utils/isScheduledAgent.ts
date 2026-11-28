const isScheduledAgent = (agent: {channels?: ({channelType: string} | null)[] | null}): boolean =>
    (agent.channels ?? []).some((channel) => channel?.channelType === 'schedule');

export default isScheduledAgent;
