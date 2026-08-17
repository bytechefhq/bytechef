import Badge from '@/components/Badge/Badge';

interface AgentsFilterTitlePropsI {
    agentName?: string;
    tagName?: string;
}

/**
 * "FILTER BY AGENT: <name>" above the agents list, matching ProjectsFilterTitle and the other list pages.
 *
 * Agents filter by tag OR by a single agent, never both — picking one clears the other (see Agents.tsx), so
 * the label names whichever is active rather than combining them. It says "tag" only when a tag is actually
 * selected: the unfiltered state reads "All Agents", so calling that a tag filter describes a filter that is
 * not applied.
 */
const AgentsFilterTitle = ({agentName, tagName}: AgentsFilterTitlePropsI) => (
    <div className="space-x-1">
        <span className="text-sm text-muted-foreground uppercase">{`Filter by ${tagName ? 'tag' : 'agent'}:`}</span>

        <Badge label={agentName ?? tagName ?? 'All Agents'} styleType="secondary-filled" weight="semibold" />
    </div>
);

export default AgentsFilterTitle;
