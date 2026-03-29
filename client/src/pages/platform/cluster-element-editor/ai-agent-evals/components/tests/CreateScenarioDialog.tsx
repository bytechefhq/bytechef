import Button from '@/components/Button/Button';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {AgentScenarioType} from '@/shared/middleware/graphql';
import {InfoIcon} from 'lucide-react';
import {useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface CreateScenarioDialogProps {
    agentEvalTestId: string;
    editData?: {
        expectedOutput?: string | null;
        id: string;
        maxTurns?: number | null;
        name: string;
        numberOfRuns?: number | null;
        personaPrompt?: string | null;
        type: AgentScenarioType;
        userMessage?: string | null;
    };
    onClose: () => void;
    onCreate: (
        agentEvalTestId: string,
        name: string,
        type: AgentScenarioType,
        fields: {
            expectedOutput?: string;
            maxTurns?: number;
            numberOfRuns?: number;
            personaPrompt?: string;
            userMessage?: string;
        }
    ) => void;
    onUpdate?: (
        id: string,
        name: string,
        fields: {
            expectedOutput?: string;
            maxTurns?: number;
            personaPrompt?: string;
            userMessage?: string;
        }
    ) => void;
}

const CreateScenarioDialog = ({agentEvalTestId, editData, onClose, onCreate, onUpdate}: CreateScenarioDialogProps) => {
    const [expectedOutput, setExpectedOutput] = useState(editData?.expectedOutput ?? '');
    const [maxTurns, setMaxTurns] = useState(editData?.maxTurns ?? 10);
    const [name, setName] = useState(editData?.name ?? '');
    const [numberOfRuns, setNumberOfRuns] = useState(editData?.numberOfRuns ?? 1);
    const [personaPrompt, setPersonaPrompt] = useState(editData?.personaPrompt ?? '');
    const [scenarioType, setScenarioType] = useState<AgentScenarioType>(editData?.type ?? AgentScenarioType.SingleTurn);
    const [userMessage, setUserMessage] = useState(editData?.userMessage ?? '');

    const isEditing = !!editData;

    const handleSubmit = () => {
        if (!name.trim()) {
            return;
        }

        const fields =
            scenarioType === AgentScenarioType.SingleTurn
                ? {
                      expectedOutput: expectedOutput.trim() || undefined,
                      numberOfRuns: numberOfRuns > 1 ? numberOfRuns : undefined,
                      userMessage: userMessage.trim() || undefined,
                  }
                : {
                      maxTurns,
                      numberOfRuns: numberOfRuns > 1 ? numberOfRuns : undefined,
                      personaPrompt: personaPrompt.trim() || undefined,
                      userMessage: userMessage.trim() || undefined,
                  };

        if (isEditing && editData && onUpdate) {
            onUpdate(editData.id, name.trim(), fields);
        } else {
            onCreate(agentEvalTestId, name.trim(), scenarioType, fields);
        }

        onClose();
    };

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open={true}>
            <DialogContent className="max-w-lg">
                <DialogHeader>
                    <DialogTitle>{isEditing ? 'Edit Scenario' : 'Create Scenario'}</DialogTitle>
                </DialogHeader>

                <fieldset className="flex flex-col gap-4 border-0 p-0">
                    <div className="flex flex-col gap-2">
                        <div className="flex items-center gap-1">
                            <Label htmlFor="scenario-name">Name</Label>

                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <InfoIcon className="size-3.5 text-muted-foreground" />
                                </TooltipTrigger>

                                <TooltipContent className="max-w-64" side="right">
                                    A short, descriptive name for this scenario. It appears in test results so you can
                                    quickly identify which scenario passed or failed.
                                </TooltipContent>
                            </Tooltip>
                        </div>

                        <Input
                            id="scenario-name"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="Enter scenario name"
                            value={name}
                        />
                    </div>

                    <div className="flex flex-col gap-2">
                        <div className="flex items-center gap-1">
                            <Label>Type</Label>

                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <InfoIcon className="size-3.5 text-muted-foreground" />
                                </TooltipTrigger>

                                <TooltipContent className="max-w-64" side="right">
                                    Single-turn sends one message and evaluates the response. Multi-turn simulates an
                                    ongoing conversation using a persona that interacts with the agent over multiple
                                    exchanges.
                                </TooltipContent>
                            </Tooltip>
                        </div>

                        <div className="flex gap-2">
                            <button
                                className={twMerge(
                                    'rounded-md border px-3 py-1.5 text-sm',
                                    scenarioType === AgentScenarioType.SingleTurn
                                        ? 'border-blue-500 bg-blue-50 text-blue-700'
                                        : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                                )}
                                onClick={() => setScenarioType(AgentScenarioType.SingleTurn)}
                                type="button"
                            >
                                Single-turn
                            </button>

                            <button
                                className={twMerge(
                                    'rounded-md border px-3 py-1.5 text-sm',
                                    scenarioType === AgentScenarioType.MultiTurn
                                        ? 'border-purple-500 bg-purple-50 text-purple-700'
                                        : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                                )}
                                onClick={() => setScenarioType(AgentScenarioType.MultiTurn)}
                                type="button"
                            >
                                Multi-turn
                            </button>
                        </div>
                    </div>

                    {scenarioType === AgentScenarioType.SingleTurn ? (
                        <>
                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label htmlFor="scenario-user-message">User Message</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            The exact message sent to the agent as user input. This is the prompt the
                                            agent will receive and respond to during the evaluation.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <Textarea
                                    id="scenario-user-message"
                                    onChange={(event) => setUserMessage(event.target.value)}
                                    placeholder="Enter the user message to send to the agent"
                                    rows={3}
                                    value={userMessage}
                                />
                            </div>

                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label htmlFor="scenario-expected-output">Expected Output (optional)</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            The ideal or reference response you expect from the agent. Judges can
                                            compare the actual agent output against this to measure accuracy,
                                            similarity, or correctness.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <Textarea
                                    id="scenario-expected-output"
                                    onChange={(event) => setExpectedOutput(event.target.value)}
                                    placeholder="Enter the expected agent response"
                                    rows={3}
                                    value={expectedOutput}
                                />
                            </div>
                        </>
                    ) : (
                        <>
                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label htmlFor="scenario-persona-prompt">Persona Prompt</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            Instructions that define a simulated user persona. This persona will
                                            autonomously converse with the agent across multiple turns, testing how well
                                            the agent handles an extended interaction.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <Textarea
                                    id="scenario-persona-prompt"
                                    onChange={(event) => setPersonaPrompt(event.target.value)}
                                    placeholder="Describe the persona that will interact with the agent"
                                    rows={3}
                                    value={personaPrompt}
                                />
                            </div>

                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label htmlFor="scenario-first-message">First Message (Optional)</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            Optional fixed opening message. If set, this is sent as the first user
                                            message before the persona takes over. If empty, the user simulator
                                            generates the first message.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <Textarea
                                    id="scenario-first-message"
                                    onChange={(event) => setUserMessage(event.target.value)}
                                    placeholder="Enter an optional first message"
                                    rows={2}
                                    value={userMessage}
                                />
                            </div>

                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label htmlFor="scenario-max-turns">Max Turns</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            The maximum number of conversation exchanges (turns) between the persona and
                                            the agent before the scenario ends. Higher values allow deeper conversations
                                            but take longer to run.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <Input
                                    id="scenario-max-turns"
                                    max={50}
                                    min={1}
                                    onChange={(event) => setMaxTurns(Number(event.target.value))}
                                    type="number"
                                    value={maxTurns}
                                />
                            </div>
                        </>
                    )}

                    <div className="flex flex-col gap-2">
                        <div className="flex items-center gap-1">
                            <Label htmlFor="scenario-number-of-runs">Number of Runs</Label>

                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <InfoIcon className="size-3.5 text-muted-foreground" />
                                </TooltipTrigger>

                                <TooltipContent className="max-w-64" side="right">
                                    Run this scenario multiple times to detect non-deterministic behavior. Each run
                                    produces an independent result.
                                </TooltipContent>
                            </Tooltip>
                        </div>

                        <Input
                            id="scenario-number-of-runs"
                            max={20}
                            min={1}
                            onChange={(event) => setNumberOfRuns(Number(event.target.value))}
                            type="number"
                            value={numberOfRuns}
                        />
                    </div>
                </fieldset>

                <DialogFooter>
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button disabled={!name.trim()} label={isEditing ? 'Save' : 'Create'} onClick={handleSubmit} />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default CreateScenarioDialog;
