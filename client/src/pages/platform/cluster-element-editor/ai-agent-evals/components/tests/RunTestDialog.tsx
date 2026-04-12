import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import useRunTestDialog from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/tests/hooks/useRunTestDialog';
import {type AiAgentEvalTestsQuery, AiAgentJudgeType, AiAgentScenarioType} from '@/shared/middleware/graphql';
import {Loader2Icon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

type AiAgentEvalTestListItemType = AiAgentEvalTestsQuery['aiAgentEvalTests'][number];

const SCENARIO_TYPE_COLORS: Record<AiAgentScenarioType, string> = {
    [AiAgentScenarioType.MultiTurn]: 'border-purple-200 bg-purple-50 text-purple-700',
    [AiAgentScenarioType.SingleTurn]: 'border-blue-200 bg-blue-50 text-blue-700',
};

const JUDGE_TYPE_COLORS: Record<AiAgentJudgeType, string> = {
    [AiAgentJudgeType.ContainsText]: 'border-amber-200 bg-amber-50 text-amber-700',
    [AiAgentJudgeType.JsonSchema]: 'border-indigo-200 bg-indigo-50 text-indigo-700',
    [AiAgentJudgeType.LlmRule]: 'border-blue-200 bg-blue-50 text-blue-700',
    [AiAgentJudgeType.RegexMatch]: 'border-purple-200 bg-purple-50 text-purple-700',
    [AiAgentJudgeType.ResponseLength]: 'border-green-200 bg-green-50 text-green-700',
    [AiAgentJudgeType.Similarity]: 'border-teal-200 bg-teal-50 text-teal-700',
    [AiAgentJudgeType.StringEquals]: 'border-cyan-200 bg-cyan-50 text-cyan-700',
    [AiAgentJudgeType.ToolUsage]: 'border-orange-200 bg-orange-50 text-orange-700',
};

interface RunTestDialogProps {
    onClose: () => void;
    test: AiAgentEvalTestListItemType;
    workflowId: string;
    workflowNodeName: string;
}

const RunTestDialog = ({onClose, test, workflowId, workflowNodeName}: RunTestDialogProps) => {
    const {
        handleRunTest,
        judges,
        scenarios,
        selectedJudgeIds,
        selectedScenarioIds,
        startRunMutation,
        toggleJudgeId,
        toggleScenarioId,
    } = useRunTestDialog({onClose, test, workflowId, workflowNodeName});

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open={true}>
            <DialogContent className="max-w-lg">
                <DialogHeader className="flex flex-row items-center justify-between">
                    <DialogTitle>Run Test — {test.name}</DialogTitle>

                    <DialogCloseButton />
                </DialogHeader>

                <fieldset className="flex flex-col gap-4 border-0 p-0">
                    {scenarios.length > 0 && (
                        <div className="flex flex-col gap-2">
                            <div className="text-xs font-medium text-gray-500">Scenarios</div>

                            <div className="flex flex-col gap-1 rounded-md border border-border/50 p-2">
                                {scenarios.map((scenario) => (
                                    <label className="flex items-center gap-2 text-sm" key={scenario.id}>
                                        <input
                                            checked={selectedScenarioIds.has(scenario.id)}
                                            onChange={() => toggleScenarioId(scenario.id)}
                                            type="checkbox"
                                        />

                                        <span>{scenario.name}</span>

                                        <span
                                            className={twMerge(
                                                'rounded-full border px-1.5 py-0.5 text-[10px] font-medium',
                                                SCENARIO_TYPE_COLORS[scenario.type]
                                            )}
                                        >
                                            {scenario.type === AiAgentScenarioType.SingleTurn
                                                ? 'Single-turn'
                                                : 'Multi-turn'}
                                        </span>
                                    </label>
                                ))}
                            </div>
                        </div>
                    )}

                    {judges.length > 0 && (
                        <div className="flex flex-col gap-2">
                            <div className="text-xs font-medium text-gray-500">Global Judges</div>

                            <div className="flex flex-col gap-1 rounded-md border border-border/50 p-2">
                                {judges.map((judge) => (
                                    <label className="flex items-center gap-2 text-sm" key={judge.id}>
                                        <input
                                            checked={selectedJudgeIds.has(judge.id)}
                                            onChange={() => toggleJudgeId(judge.id)}
                                            type="checkbox"
                                        />

                                        <span>{judge.name}</span>

                                        <span
                                            className={twMerge(
                                                'rounded-full border px-1.5 py-0.5 text-[10px] font-medium',
                                                JUDGE_TYPE_COLORS[judge.type]
                                            )}
                                        >
                                            {judge.type.replace(/_/g, ' ')}
                                        </span>
                                    </label>
                                ))}
                            </div>
                        </div>
                    )}
                </fieldset>

                <DialogFooter>
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={selectedScenarioIds.size === 0 || startRunMutation.isPending}
                        icon={startRunMutation.isPending ? <Loader2Icon className="animate-spin" /> : undefined}
                        label={startRunMutation.isPending ? 'Starting...' : 'Run Test'}
                        onClick={handleRunTest}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default RunTestDialog;
