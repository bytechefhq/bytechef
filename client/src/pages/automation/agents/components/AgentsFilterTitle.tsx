import Badge from '@/components/Badge/Badge';

interface AgentsFilterTitlePropsI {
    agentName?: string;
    scheduled?: boolean;
    tagName?: string;
}

/**
 * "FILTER BY AGENT: <name>" above the agents list, matching ProjectsFilterTitle and the other list pages.
 *
 * Agents filter by tag OR by a single agent OR by carrying a schedule, never two at once — picking one clears
 * the others (see Agents.tsx), so the label names whichever is active rather than combining them. It says
 * "tag" only when a tag is actually selected: the unfiltered state reads "All Agents", so calling that a tag
 * filter describes a filter that is not applied. The scheduled filter drops the noun entirely — "Filter by
 * agent: Scheduled" would claim an agent named Scheduled was picked.
 */
const AgentsFilterTitle = ({agentName, scheduled, tagName}: AgentsFilterTitlePropsI) => (
    <div className="space-x-1">
        <span className="text-sm text-muted-foreground uppercase">
            {scheduled ? 'Filter by:' : `Filter by ${tagName ? 'tag' : 'agent'}:`}
        </span>

        <Badge
            label={agentName ?? (scheduled ? 'Scheduled' : (tagName ?? 'All Agents'))}
            styleType="secondary-filled"
            weight="semibold"
        />
    </div>
);

export default AgentsFilterTitle;
