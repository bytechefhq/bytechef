import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {BrainCircuitIcon} from 'lucide-react';

import {AiEvalRuleType} from '../../types';

interface AiEvalRulesProps {
    evalRules: AiEvalRuleType[];
    isLoading: boolean;
}

const AiEvalRules = ({evalRules, isLoading}: AiEvalRulesProps) => {
    if (isLoading) {
        return <PageLoader loading={true} />;
    }

    if (evalRules.length === 0) {
        return (
            <EmptyList
                icon={<BrainCircuitIcon className="size-12 text-muted-foreground" />}
                message="Create eval rules to automatically score traces using LLM-as-judge."
                title="No Eval Rules"
            />
        );
    }

    return (
        <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
                <thead>
                    <tr className="border-b text-muted-foreground">
                        <th className="px-3 py-2 font-medium">Name</th>

                        <th className="px-3 py-2 font-medium">Model</th>

                        <th className="px-3 py-2 font-medium">Sampling Rate</th>

                        <th className="px-3 py-2 font-medium">Delay</th>

                        <th className="px-3 py-2 font-medium">Status</th>
                    </tr>
                </thead>

                <tbody>
                    {evalRules.map((rule) => (
                        <tr className="border-b hover:bg-muted/50" key={rule.id}>
                            <td className="px-3 py-2 font-medium">{rule.name}</td>

                            <td className="px-3 py-2">{rule.model}</td>

                            <td className="px-3 py-2">{(Number(rule.samplingRate) * 100).toFixed(0)}%</td>

                            <td className="px-3 py-2">{rule.delaySeconds != null ? `${rule.delaySeconds}s` : '-'}</td>

                            <td className="px-3 py-2">
                                <span
                                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                                        rule.enabled
                                            ? 'bg-surface-success-secondary text-content-success-primary'
                                            : 'bg-surface-neutral-secondary text-content-neutral-primary'
                                    }`}
                                >
                                    {rule.enabled ? 'Enabled' : 'Disabled'}
                                </span>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default AiEvalRules;
