import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    useAcknowledgeAiObservabilityAlertEventMutation,
    useAiObservabilityAlertEventsQuery,
    useAiObservabilityAlertRulesQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {BellOffIcon, CheckIcon, HistoryIcon} from 'lucide-react';
import {useCallback, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

const STATUS_CLASSES: Record<string, string> = {
    ACKNOWLEDGED: 'bg-yellow-100 text-yellow-800',
    RESOLVED: 'bg-green-100 text-green-800',
    TRIGGERED: 'bg-red-100 text-red-800',
};

const AiObservabilityAlertHistory = () => {
    const [selectedRuleId, setSelectedRuleId] = useState<string | undefined>(undefined);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const {data: rulesData, isLoading: rulesIsLoading} = useAiObservabilityAlertRulesQuery({
        workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
    });

    const rules = useMemo(() => rulesData?.aiObservabilityAlertRules ?? [], [rulesData?.aiObservabilityAlertRules]);

    const ruleNameMap = useMemo(() => {
        const nameMap: Record<string, string> = {};

        for (const rule of rules) {
            if (rule) {
                nameMap[rule.id] = rule.name;
            }
        }

        return nameMap;
    }, [rules]);

    const {data: eventsData, isLoading: eventsIsLoading} = useAiObservabilityAlertEventsQuery(
        {alertRuleId: selectedRuleId!},
        {enabled: !!selectedRuleId}
    );

    const acknowledgeMutation = useAcknowledgeAiObservabilityAlertEventMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityAlertEvents']});
        },
    });

    const events = eventsData?.aiObservabilityAlertEvents ?? [];

    const handleAcknowledge = useCallback(
        (eventId: string) => {
            acknowledgeMutation.mutate({id: eventId});
        },
        [acknowledgeMutation]
    );

    if (rulesIsLoading) {
        return <PageLoader loading={true} />;
    }

    if (rules.length === 0) {
        return (
            <EmptyList
                icon={<BellOffIcon className="size-12 text-muted-foreground" />}
                message="Create alert rules first to see alert history."
                title="No Alert Rules"
            />
        );
    }

    return (
        <div>
            <div className="mb-4 flex items-center gap-3">
                <select
                    className="rounded border bg-muted px-3 py-1 text-sm text-muted-foreground"
                    onChange={(event) => setSelectedRuleId(event.target.value || undefined)}
                    value={selectedRuleId || ''}
                >
                    <option value="">Select a rule...</option>

                    {rules.map((rule) =>
                        rule ? (
                            <option key={rule.id} value={rule.id}>
                                {rule.name}
                            </option>
                        ) : null
                    )}
                </select>
            </div>

            {!selectedRuleId ? (
                <EmptyList
                    icon={<HistoryIcon className="size-12 text-muted-foreground" />}
                    message="Select an alert rule above to view its event history."
                    title="Select a Rule"
                />
            ) : eventsIsLoading ? (
                <PageLoader loading={true} />
            ) : events.length === 0 ? (
                <EmptyList
                    icon={<HistoryIcon className="size-12 text-muted-foreground" />}
                    message="No alert events have been triggered for this rule."
                    title="No Alert Events"
                />
            ) : (
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead>
                            <tr className="border-b text-muted-foreground">
                                <th className="pb-2 font-medium">Time</th>

                                <th className="pb-2 font-medium">Rule</th>

                                <th className="pb-2 font-medium">Triggered Value</th>

                                <th className="pb-2 font-medium">Message</th>

                                <th className="pb-2 font-medium">Status</th>

                                <th className="pb-2 font-medium">Actions</th>
                            </tr>
                        </thead>

                        <tbody>
                            {events.map((event) =>
                                event ? (
                                    <tr className="border-b" key={event.id}>
                                        <td className="py-3 text-muted-foreground">
                                            {event.createdDate ? new Date(event.createdDate).toLocaleString() : '--'}
                                        </td>

                                        <td className="py-3 font-medium">
                                            {ruleNameMap[event.alertRuleId] || event.alertRuleId}
                                        </td>

                                        <td className="py-3">
                                            {event.triggeredValue != null ? event.triggeredValue.toFixed(4) : '--'}
                                        </td>

                                        <td className="py-3 text-muted-foreground">{event.message || '--'}</td>

                                        <td className="py-3">
                                            <span
                                                className={twMerge(
                                                    'rounded-full px-2 py-0.5 text-xs font-medium',
                                                    STATUS_CLASSES[event.status] || 'bg-gray-100 text-gray-800'
                                                )}
                                            >
                                                {event.status}
                                            </span>
                                        </td>

                                        <td className="py-3">
                                            {event.status === 'TRIGGERED' && (
                                                <button
                                                    className="flex items-center gap-1 rounded px-2 py-1 text-xs font-medium text-muted-foreground hover:bg-muted hover:text-foreground"
                                                    onClick={() => handleAcknowledge(event.id)}
                                                >
                                                    <CheckIcon className="size-3" />
                                                    Acknowledge
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                ) : null
                            )}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default AiObservabilityAlertHistory;
