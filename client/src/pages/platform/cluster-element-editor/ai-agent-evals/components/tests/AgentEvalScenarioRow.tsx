import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import CreateJudgeDialog from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/judges/CreateJudgeDialog';
import CreateToolSimulationDialog from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/tests/CreateToolSimulationDialog';
import {
    type AgentEvalTestQuery,
    AgentJudgeType,
    AgentScenarioType,
    useCreateAgentScenarioJudgeMutation,
    useCreateAgentScenarioToolSimulationMutation,
    useDeleteAgentScenarioJudgeMutation,
    useDeleteAgentScenarioToolSimulationMutation,
    useUpdateAgentScenarioJudgeMutation,
    useUpdateAgentScenarioToolSimulationMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon, ChevronRightIcon, GavelIcon, PencilIcon, PlusIcon, TrashIcon, WrenchIcon} from 'lucide-react';
import {useCallback, useState} from 'react';

type AgentEvalScenarioFromQueryType = NonNullable<AgentEvalTestQuery['agentEvalTest']>['scenarios'][number];
type ScenarioJudgeType = AgentEvalScenarioFromQueryType['judges'][number];
type ToolSimulationType = AgentEvalScenarioFromQueryType['toolSimulations'][number];

interface AgentEvalScenarioRowProps {
    onDelete: (id: string) => void;
    onEdit: (scenario: AgentEvalScenarioFromQueryType) => void;
    scenario: AgentEvalScenarioFromQueryType;
}

const AgentEvalScenarioRow = ({onDelete, onEdit, scenario}: AgentEvalScenarioRowProps) => {
    const [editingJudge, setEditingJudge] = useState<ScenarioJudgeType | null>(null);
    const [editingToolSimulation, setEditingToolSimulation] = useState<ToolSimulationType | null>(null);
    const [expanded, setExpanded] = useState(false);
    const [showCreateJudgeDialog, setShowCreateJudgeDialog] = useState(false);
    const [showCreateToolSimulationDialog, setShowCreateToolSimulationDialog] = useState(false);

    const queryClient = useQueryClient();

    const isSingleTurn = scenario.type === AgentScenarioType.SingleTurn;

    const invalidateTest = useCallback(
        () => queryClient.invalidateQueries({queryKey: ['agentEvalTest']}),
        [queryClient]
    );

    const createJudgeMutation = useCreateAgentScenarioJudgeMutation({onSuccess: invalidateTest});
    const deleteJudgeMutation = useDeleteAgentScenarioJudgeMutation({onSuccess: invalidateTest});
    const updateJudgeMutation = useUpdateAgentScenarioJudgeMutation({onSuccess: invalidateTest});

    const createToolSimulationMutation = useCreateAgentScenarioToolSimulationMutation({onSuccess: invalidateTest});
    const deleteToolSimulationMutation = useDeleteAgentScenarioToolSimulationMutation({onSuccess: invalidateTest});
    const updateToolSimulationMutation = useUpdateAgentScenarioToolSimulationMutation({onSuccess: invalidateTest});

    const handleCreateJudge = useCallback(
        (name: string, type: AgentJudgeType, configuration: Record<string, unknown>) => {
            createJudgeMutation.mutate({
                agentEvalScenarioId: scenario.id,
                configuration,
                name,
                type,
            });
        },
        [createJudgeMutation, scenario.id]
    );

    const handleUpdateJudge = useCallback(
        (id: string, name?: string, configuration?: Record<string, unknown>) => {
            updateJudgeMutation.mutate({configuration, id, name});
        },
        [updateJudgeMutation]
    );

    const handleDeleteJudge = useCallback(
        (id: string) => {
            deleteJudgeMutation.mutate({id});
        },
        [deleteJudgeMutation]
    );

    const handleCreateToolSimulation = useCallback(
        (toolName: string, responsePrompt: string, simulationModel?: string) => {
            createToolSimulationMutation.mutate({
                agentEvalScenarioId: scenario.id,
                responsePrompt,
                simulationModel,
                toolName,
            });
        },
        [createToolSimulationMutation, scenario.id]
    );

    const handleDeleteToolSimulation = useCallback(
        (id: string) => {
            deleteToolSimulationMutation.mutate({id});
        },
        [deleteToolSimulationMutation]
    );

    const handleUpdateToolSimulation = useCallback(
        (id: string, toolName?: string, responsePrompt?: string, simulationModel?: string) => {
            updateToolSimulationMutation.mutate({id, responsePrompt, simulationModel, toolName});
        },
        [updateToolSimulationMutation]
    );

    return (
        <div className="py-1">
            <div className="flex items-center justify-between rounded-md px-3 py-2 hover:bg-gray-50">
                <div className="flex flex-1 cursor-pointer items-center gap-3" onClick={() => setExpanded(!expanded)}>
                    {expanded ? (
                        <ChevronDownIcon className="size-3.5 text-gray-400" />
                    ) : (
                        <ChevronRightIcon className="size-3.5 text-gray-400" />
                    )}

                    <span className="text-sm">{scenario.name}</span>

                    <Badge
                        className="text-xs"
                        label={isSingleTurn ? 'Single-turn' : 'Multi-turn'}
                        styleType={isSingleTurn ? 'primary-outline' : 'outline-outline'}
                    />

                    {scenario.judges.length > 0 && (
                        <Badge
                            className="text-xs"
                            label={`${scenario.judges.length} ${scenario.judges.length === 1 ? 'judge' : 'judges'}`}
                            styleType="secondary-filled"
                        />
                    )}
                </div>

                <div className="flex items-center gap-1">
                    <Button
                        icon={<PencilIcon className="size-3.5" />}
                        onClick={() => onEdit(scenario)}
                        size="icon"
                        variant="ghost"
                    />

                    <Button
                        className="text-red-500 hover:text-red-600"
                        icon={<TrashIcon className="size-3.5" />}
                        onClick={() => onDelete(scenario.id)}
                        size="icon"
                        variant="ghost"
                    />
                </div>
            </div>

            {expanded && (
                <>
                    <div className="ml-8 mt-1 rounded-md border border-border/30 bg-gray-50/50 p-2">
                        {scenario.judges.length > 0 ? (
                            <div className="flex flex-col gap-1">
                                {scenario.judges.map((judge) => (
                                    <div
                                        className="flex items-center justify-between rounded px-2 py-1.5 hover:bg-white"
                                        key={judge.id}
                                    >
                                        <div className="flex items-center gap-2">
                                            <GavelIcon className="size-3.5 text-gray-400" />

                                            <span className="text-xs font-medium">{judge.name}</span>

                                            <span className="text-xs text-gray-400">
                                                {judge.type.replace('_', ' ').toLowerCase()}
                                            </span>
                                        </div>

                                        <div className="flex items-center gap-1">
                                            <Button
                                                icon={<PencilIcon className="size-3" />}
                                                onClick={() => setEditingJudge(judge)}
                                                size="icon"
                                                variant="ghost"
                                            />

                                            <Button
                                                className="text-red-500 hover:text-red-600"
                                                icon={<TrashIcon className="size-3" />}
                                                onClick={() => handleDeleteJudge(judge.id)}
                                                size="icon"
                                                variant="ghost"
                                            />
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <div className="py-2 text-center text-xs text-gray-400">No scenario-level judges</div>
                        )}

                        <button
                            className="mt-1 flex w-full items-center justify-center gap-1 rounded border border-dashed border-gray-300 py-1.5 text-xs text-gray-500 hover:border-gray-400 hover:text-gray-600"
                            onClick={() => setShowCreateJudgeDialog(true)}
                        >
                            <PlusIcon className="size-3" />
                            Add Judge
                        </button>
                    </div>

                    <div className="ml-8 mt-1 rounded-md border border-border/30 bg-gray-50/50 p-2">
                        {scenario.toolSimulations.length > 0 ? (
                            <div className="flex flex-col gap-1">
                                {scenario.toolSimulations.map((simulation) => (
                                    <div
                                        className="flex items-center justify-between rounded px-2 py-1.5 hover:bg-white"
                                        key={simulation.id}
                                    >
                                        <div className="flex items-center gap-2">
                                            <WrenchIcon className="size-3.5 text-gray-400" />

                                            <span className="text-xs font-medium">{simulation.toolName}</span>

                                            <span className="max-w-48 truncate text-xs text-gray-400">
                                                {simulation.responsePrompt}
                                            </span>

                                            {simulation.simulationModel && (
                                                <span className="rounded-full border border-violet-200 bg-violet-50 px-1.5 py-0.5 text-[10px] font-medium text-violet-700">
                                                    {simulation.simulationModel}
                                                </span>
                                            )}
                                        </div>

                                        <div className="flex items-center gap-1">
                                            <Button
                                                icon={<PencilIcon className="size-3" />}
                                                onClick={() => setEditingToolSimulation(simulation)}
                                                size="icon"
                                                variant="ghost"
                                            />

                                            <Button
                                                className="text-red-500 hover:text-red-600"
                                                icon={<TrashIcon className="size-3" />}
                                                onClick={() => handleDeleteToolSimulation(simulation.id)}
                                                size="icon"
                                                variant="ghost"
                                            />
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <div className="py-2 text-center text-xs text-gray-400">No tool simulations</div>
                        )}

                        <button
                            className="mt-1 flex w-full items-center justify-center gap-1 rounded border border-dashed border-gray-300 py-1.5 text-xs text-gray-500 hover:border-gray-400 hover:text-gray-600"
                            onClick={() => setShowCreateToolSimulationDialog(true)}
                        >
                            <PlusIcon className="size-3" />
                            Add Tool Simulation
                        </button>
                    </div>
                </>
            )}

            {showCreateJudgeDialog && (
                <CreateJudgeDialog onClose={() => setShowCreateJudgeDialog(false)} onCreate={handleCreateJudge} />
            )}

            {editingJudge && (
                <CreateJudgeDialog
                    editData={editingJudge}
                    onClose={() => setEditingJudge(null)}
                    onCreate={handleCreateJudge}
                    onUpdate={handleUpdateJudge}
                />
            )}

            {showCreateToolSimulationDialog && (
                <CreateToolSimulationDialog
                    onClose={() => setShowCreateToolSimulationDialog(false)}
                    onCreate={handleCreateToolSimulation}
                />
            )}

            {editingToolSimulation && (
                <CreateToolSimulationDialog
                    editData={editingToolSimulation}
                    onClose={() => setEditingToolSimulation(null)}
                    onCreate={handleCreateToolSimulation}
                    onUpdate={handleUpdateToolSimulation}
                />
            )}
        </div>
    );
};

export default AgentEvalScenarioRow;
