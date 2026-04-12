import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import CreateJudgeDialog from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/judges/CreateJudgeDialog';
import CreateToolSimulationDialog from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/tests/CreateToolSimulationDialog';
import useAiAgentEvalScenarioRow from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/tests/hooks/useAiAgentEvalScenarioRow';
import {type AiAgentEvalTestQuery, AiAgentScenarioType} from '@/shared/middleware/graphql';
import {ChevronDownIcon, ChevronRightIcon, GavelIcon, PencilIcon, PlusIcon, TrashIcon, WrenchIcon} from 'lucide-react';
import {useState} from 'react';

type AiAgentEvalScenarioFromQueryType = NonNullable<AiAgentEvalTestQuery['aiAgentEvalTest']>['scenarios'][number];
type ScenarioJudgeType = AiAgentEvalScenarioFromQueryType['judges'][number];
type ToolSimulationType = AiAgentEvalScenarioFromQueryType['toolSimulations'][number];

interface AiAgentEvalScenarioRowProps {
    onDelete: (id: string) => void;
    onEdit: (scenario: AiAgentEvalScenarioFromQueryType) => void;
    scenario: AiAgentEvalScenarioFromQueryType;
}

const AiAgentEvalScenarioRow = ({onDelete, onEdit, scenario}: AiAgentEvalScenarioRowProps) => {
    const [editingJudge, setEditingJudge] = useState<ScenarioJudgeType | null>(null);
    const [editingToolSimulation, setEditingToolSimulation] = useState<ToolSimulationType | null>(null);
    const [expanded, setExpanded] = useState(false);
    const [showCreateJudgeDialog, setShowCreateJudgeDialog] = useState(false);
    const [showCreateToolSimulationDialog, setShowCreateToolSimulationDialog] = useState(false);

    const {
        handleCreateJudge,
        handleCreateToolSimulation,
        handleDeleteJudge,
        handleDeleteToolSimulation,
        handleUpdateJudge,
        handleUpdateToolSimulation,
    } = useAiAgentEvalScenarioRow(scenario.id);

    const isSingleTurn = scenario.type === AiAgentScenarioType.SingleTurn;

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

                        <Button
                            className="mt-1 w-full border-dashed py-4"
                            icon={<PlusIcon />}
                            label="Add Judge"
                            onClick={() => setShowCreateJudgeDialog(true)}
                            size="xs"
                            variant="outline"
                        />
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

                        <Button
                            className="mt-1 w-full border-dashed py-4"
                            icon={<PlusIcon />}
                            label="Add Tool Simulation"
                            onClick={() => setShowCreateToolSimulationDialog(true)}
                            size="xs"
                            variant="outline"
                        />
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

export default AiAgentEvalScenarioRow;
