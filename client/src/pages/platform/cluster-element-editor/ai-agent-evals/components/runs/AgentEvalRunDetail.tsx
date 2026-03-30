import Button from '@/components/Button/Button';
import RunSummaryCards from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/runs/RunSummaryCards';
import ScenarioResultsTable from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/runs/ScenarioResultsTable';
import {
    RUN_STATUS_COLORS,
    RUN_STATUS_LABELS,
} from '@/pages/platform/cluster-element-editor/ai-agent-evals/utils/evalRunUtils';
import {type AgentEvalRunQuery, AgentEvalRunStatus} from '@/shared/middleware/graphql';
import {ArrowLeftIcon, Loader2Icon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

type SelectedRunType = NonNullable<AgentEvalRunQuery['agentEvalRun']>;

interface RunSummaryI {
    averageScore?: number | null;
    errorCount: number;
    failedCount: number;
    passedCount: number;
    totalInputTokens?: number | null;
    totalOutputTokens?: number | null;
    totalScenarios: number;
}

interface AgentEvalRunDetailProps {
    onBack: () => void;
    run: SelectedRunType;
    summary: RunSummaryI | null;
}

const AgentEvalRunDetail = ({onBack, run, summary}: AgentEvalRunDetailProps) => {
    return (
        <div className="flex flex-1 flex-col gap-4 px-4">
            <div className="flex items-center gap-2">
                <Button icon={<ArrowLeftIcon />} onClick={onBack} size="iconXs" variant="ghost" />

                <Button label="All Runs" onClick={onBack} size="sm" variant="ghost" />

                <span className="text-sm text-gray-400">/</span>

                <span className="text-sm font-semibold">{run.name}</span>

                <span
                    className={twMerge(
                        'ml-1 inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs font-medium',
                        RUN_STATUS_COLORS[run.status]
                    )}
                >
                    {run.status === AgentEvalRunStatus.Running && <Loader2Icon className="size-3 animate-spin" />}

                    {RUN_STATUS_LABELS[run.status]}
                </span>
            </div>

            {summary && <RunSummaryCards summary={summary} />}

            <ScenarioResultsTable results={run.results} />
        </div>
    );
};

export default AgentEvalRunDetail;
