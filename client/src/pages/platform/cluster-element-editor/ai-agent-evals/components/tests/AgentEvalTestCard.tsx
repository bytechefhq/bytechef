import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import AgentEvalScenarioRow from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/tests/AgentEvalScenarioRow';
import CreateScenarioDialog from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/tests/CreateScenarioDialog';
import DeleteTestAlertDialog from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/tests/DeleteTestAlertDialog';
import {useAiAgentEvalsStore} from '@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore';
import {
    AgentEvalScenario,
    AgentEvalTestsQuery,
    AgentScenarioType,
    useAgentEvalTestQuery,
} from '@/shared/middleware/graphql';
import {
    ChevronDownIcon,
    ChevronRightIcon,
    EllipsisVerticalIcon,
    FlaskConicalIcon,
    PlayIcon,
    PlusIcon,
    TrashIcon,
} from 'lucide-react';
import {useState} from 'react';

type AgentEvalTestListItemType = AgentEvalTestsQuery['agentEvalTests'][number];

interface AgentEvalTestCardProps {
    onCreateScenario: (
        agentEvalTestId: string,
        name: string,
        type: AgentScenarioType,
        fields: {
            expectedOutput?: string;
            maxTurns?: number;
            personaPrompt?: string;
            userMessage?: string;
        }
    ) => void;
    onDeleteScenario: (id: string) => void;
    onDeleteTest: (id: string) => void;
    onUpdateScenario: (
        id: string,
        name: string,
        fields: {
            expectedOutput?: string;
            maxTurns?: number;
            personaPrompt?: string;
            userMessage?: string;
        }
    ) => void;
    test: AgentEvalTestListItemType;
}

const AgentEvalTestCard = ({
    onCreateScenario,
    onDeleteScenario,
    onDeleteTest,
    onUpdateScenario,
    test,
}: AgentEvalTestCardProps) => {
    const {setEvalsTab, setSelectedTestId} = useAiAgentEvalsStore();
    const [editingScenario, setEditingScenario] = useState<AgentEvalScenario | null>(null);
    const [expanded, setExpanded] = useState(false);
    const [showCreateScenarioDialog, setShowCreateScenarioDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const {data: testDetailData} = useAgentEvalTestQuery({id: test.id}, {enabled: expanded});

    const scenarios = testDetailData?.agentEvalTest?.scenarios ?? [];

    return (
        <>
            <div className="rounded-lg border border-border/50">
                <div
                    className="flex cursor-pointer items-center justify-between px-3 py-3 hover:bg-gray-50"
                    onClick={() => setExpanded(!expanded)}
                >
                    <div className="flex flex-1 items-center gap-3">
                        {expanded ? (
                            <ChevronDownIcon className="size-4 text-gray-400" />
                        ) : (
                            <ChevronRightIcon className="size-4 text-gray-400" />
                        )}

                        <div className="flex size-8 items-center justify-center rounded bg-emerald-500">
                            <FlaskConicalIcon className="size-4 text-white" />
                        </div>

                        <div className="flex-1">
                            <div className="text-sm font-semibold">{test.name}</div>

                            {test.description && (
                                <div className="line-clamp-1 text-xs text-gray-500">{test.description}</div>
                            )}
                        </div>
                    </div>

                    <div className="flex items-center gap-2">
                        <span className="text-xs text-gray-500">
                            {scenarios.length} {scenarios.length === 1 ? 'scenario' : 'scenarios'}
                        </span>

                        <Button
                            disabled={scenarios.length === 0}
                            icon={<PlayIcon className="size-3.5" />}
                            onClick={(event) => {
                                event.stopPropagation();
                                setSelectedTestId(test.id);
                                setEvalsTab('runs');
                            }}
                            size="icon"
                            variant="ghost"
                        />

                        <DropdownMenu>
                            <DropdownMenuTrigger asChild onClick={(event) => event.stopPropagation()}>
                                <Button
                                    icon={<EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />}
                                    size="icon"
                                    variant="ghost"
                                />
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end" onClick={(event) => event.stopPropagation()}>
                                <DropdownMenuItem
                                    onClick={() => {
                                        setExpanded(true);
                                        setShowCreateScenarioDialog(true);
                                    }}
                                >
                                    <PlusIcon className="mr-2 size-4" />
                                    Add Scenario
                                </DropdownMenuItem>

                                <DropdownMenuSeparator />

                                <DropdownMenuItem className="text-red-600" onClick={() => setShowDeleteDialog(true)}>
                                    <TrashIcon className="mr-2 size-4" />
                                    Delete
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>

                {expanded && (
                    <div className="border-t border-border/50 px-3 py-2">
                        {scenarios.length > 0 ? (
                            <div className="divide-y divide-border/30">
                                {scenarios.map((scenario) => (
                                    <AgentEvalScenarioRow
                                        key={scenario.id}
                                        onDelete={onDeleteScenario}
                                        onEdit={(editScenario) => setEditingScenario(editScenario)}
                                        scenario={scenario}
                                    />
                                ))}
                            </div>
                        ) : (
                            <div className="py-4 text-center text-sm text-gray-500">No scenarios yet</div>
                        )}

                        <button
                            className="mt-2 flex w-full items-center justify-center gap-1 rounded-md border border-dashed border-gray-300 py-2 text-sm text-gray-500 hover:border-gray-400 hover:text-gray-600"
                            onClick={() => setShowCreateScenarioDialog(true)}
                        >
                            <PlusIcon className="size-4" />
                            Add Scenario
                        </button>
                    </div>
                )}
            </div>

            {showDeleteDialog && (
                <DeleteTestAlertDialog
                    onClose={() => setShowDeleteDialog(false)}
                    onDelete={() => {
                        onDeleteTest(test.id);
                        setShowDeleteDialog(false);
                    }}
                />
            )}

            {showCreateScenarioDialog && (
                <CreateScenarioDialog
                    agentEvalTestId={test.id}
                    onClose={() => setShowCreateScenarioDialog(false)}
                    onCreate={onCreateScenario}
                />
            )}

            {editingScenario && (
                <CreateScenarioDialog
                    agentEvalTestId={test.id}
                    editData={{
                        expectedOutput: editingScenario.expectedOutput,
                        id: editingScenario.id,
                        maxTurns: editingScenario.maxTurns,
                        name: editingScenario.name,
                        personaPrompt: editingScenario.personaPrompt,
                        type: editingScenario.type,
                        userMessage: editingScenario.userMessage,
                    }}
                    onClose={() => setEditingScenario(null)}
                    onCreate={onCreateScenario}
                    onUpdate={onUpdateScenario}
                />
            )}
        </>
    );
};

export default AgentEvalTestCard;
