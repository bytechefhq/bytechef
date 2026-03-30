import {useAiAgentEvalsStore} from '@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore';
import {
    type AgentEvalTestsQuery,
    useAgentEvalTestQuery,
    useAgentJudgesQuery,
    useStartAgentEvalRunMutation,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo, useRef, useState} from 'react';
import {toast} from 'sonner';

type AgentEvalTestListItemType = AgentEvalTestsQuery['agentEvalTests'][number];

function generateRunName(): string {
    const now = new Date();
    const date = now.toLocaleDateString('en-US', {day: 'numeric', month: 'short'});
    const time = now.toLocaleTimeString('en-US', {hour: 'numeric', hour12: true, minute: '2-digit'});

    return `Run — ${date}, ${time}`;
}

interface UseRunTestDialogProps {
    onClose: () => void;
    test: AgentEvalTestListItemType;
    workflowId: string;
    workflowNodeName: string;
}

export default function useRunTestDialog({onClose, test, workflowId, workflowNodeName}: UseRunTestDialogProps) {
    const [selectedJudgeIds, setSelectedJudgeIds] = useState<Set<string>>(new Set());
    const [selectedScenarioIds, setSelectedScenarioIds] = useState<Set<string>>(new Set());

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {setEvalsTab, setSelectedRunId, setSelectedTestId} = useAiAgentEvalsStore();

    const queryClient = useQueryClient();

    const {data: testDetailData} = useAgentEvalTestQuery({id: test.id});
    const {data: judgesData} = useAgentJudgesQuery({workflowId, workflowNodeName});

    const scenariosInitializedRef = useRef(false);
    const judgesInitializedRef = useRef(false);

    const scenarios = useMemo(() => testDetailData?.agentEvalTest?.scenarios ?? [], [testDetailData]);
    const judges = useMemo(() => judgesData?.agentJudges ?? [], [judgesData]);

    useEffect(() => {
        if (scenarios.length > 0 && !scenariosInitializedRef.current) {
            scenariosInitializedRef.current = true;

            setSelectedScenarioIds(new Set(scenarios.map((scenario) => scenario.id)));
        }
    }, [scenarios]);

    useEffect(() => {
        if (judges.length > 0 && !judgesInitializedRef.current) {
            judgesInitializedRef.current = true;

            setSelectedJudgeIds(new Set(judges.map((judge) => judge.id)));
        }
    }, [judges]);

    const startRunMutation = useStartAgentEvalRunMutation({
        onError: (error: Error) => {
            toast.error('Failed to start eval run: ' + error.message);
        },
        onSuccess: (data) => {
            queryClient.invalidateQueries({queryKey: ['agentEvalRuns']});

            setSelectedTestId(test.id);
            setEvalsTab('runs');

            if (data.startAgentEvalRun?.id) {
                setSelectedRunId(data.startAgentEvalRun.id);
            } else {
                toast.warning('Run started but the run ID was not returned. Check the Runs tab for status.');
            }

            onClose();
        },
    });

    const toggleScenarioId = (scenarioId: string) => {
        setSelectedScenarioIds((previous) => {
            const next = new Set(previous);

            if (next.has(scenarioId)) {
                next.delete(scenarioId);
            } else {
                next.add(scenarioId);
            }

            return next;
        });
    };

    const toggleJudgeId = (judgeId: string) => {
        setSelectedJudgeIds((previous) => {
            const next = new Set(previous);

            if (next.has(judgeId)) {
                next.delete(judgeId);
            } else {
                next.add(judgeId);
            }

            return next;
        });
    };

    const handleRunTest = () => {
        startRunMutation.mutate({
            agentEvalTestId: test.id,
            agentJudgeIds: Array.from(selectedJudgeIds),
            environmentId: String(currentEnvironmentId),
            name: generateRunName(),
            scenarioIds: Array.from(selectedScenarioIds),
        });
    };

    return {
        handleRunTest,
        judges,
        scenarios,
        selectedJudgeIds,
        selectedScenarioIds,
        startRunMutation,
        toggleJudgeId,
        toggleScenarioId,
    };
}
