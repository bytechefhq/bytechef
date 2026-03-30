import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useCreateJudgeDialog, {
    type JudgeEditDataI,
} from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/judges/hooks/useCreateJudgeDialog';
import {AgentJudgeType} from '@/shared/middleware/graphql';
import {InfoIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

const JUDGE_TYPE_OPTIONS: Array<{label: string; value: AgentJudgeType}> = [
    {label: 'LLM Rule', value: AgentJudgeType.LlmRule},
    {label: 'Contains Text', value: AgentJudgeType.ContainsText},
    {label: 'Regex Match', value: AgentJudgeType.RegexMatch},
    {label: 'Response Length', value: AgentJudgeType.ResponseLength},
    {label: 'JSON Schema', value: AgentJudgeType.JsonSchema},
    {label: 'Similarity', value: AgentJudgeType.Similarity},
    {label: 'String Equals', value: AgentJudgeType.StringEquals},
    {label: 'Tool Usage', value: AgentJudgeType.ToolUsage},
];

interface CreateJudgeDialogProps {
    editData?: JudgeEditDataI;
    onClose: () => void;
    onCreate: (name: string, type: AgentJudgeType, configuration: Record<string, unknown>) => void;
    onUpdate?: (id: string, name?: string, configuration?: Record<string, unknown>) => void;
}

const CreateJudgeDialog = ({editData, onClose, onCreate, onUpdate}: CreateJudgeDialogProps) => {
    const {
        algorithm,
        allConnections,
        caseSensitive,
        connectionId,
        expectedOutput,
        expectedValue,
        handleProviderChange,
        handleSubmit,
        isEditing,
        isLlmRule,
        jsonSchema,
        jsonSchemaError,
        judgeType,
        maxLength,
        minLength,
        model,
        modelOptions,
        modelProviders,
        mustNotContain,
        mustNotMatch,
        name,
        provider,
        regexPattern,
        rule,
        searchText,
        setAlgorithm,
        setCaseSensitive,
        setConnectionId,
        setExpectedOutput,
        setExpectedValue,
        setJsonSchema,
        setJsonSchemaError,
        setJudgeType,
        setMaxLength,
        setMinLength,
        setModel,
        setMustNotContain,
        setMustNotMatch,
        setName,
        setRegexPattern,
        setRule,
        setSearchText,
        setThreshold,
        setToolComparison,
        setToolCount,
        setToolName,
        setToolPosition,
        threshold,
        toolComparison,
        toolCount,
        toolName,
        toolPosition,
    } = useCreateJudgeDialog({editData, onClose, onCreate, onUpdate});

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open={true}>
            <DialogContent className="max-w-lg">
                <DialogHeader className="flex flex-row items-center justify-between">
                    <DialogTitle>{isEditing ? 'Edit Judge' : 'Create Judge'}</DialogTitle>

                    <DialogCloseButton />
                </DialogHeader>

                <fieldset className="flex max-h-[60vh] flex-col gap-4 overflow-y-auto border-0 p-0">
                    <div className="flex flex-col gap-2">
                        <div className="flex items-center gap-1">
                            <Label htmlFor="judge-name">Name</Label>

                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <InfoIcon className="size-3.5 text-muted-foreground" />
                                </TooltipTrigger>

                                <TooltipContent className="max-w-64" side="right">
                                    A descriptive name for this judge. It appears in evaluation results alongside the
                                    verdict so you can identify which criterion was applied.
                                </TooltipContent>
                            </Tooltip>
                        </div>

                        <Input
                            id="judge-name"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="Enter judge name"
                            value={name}
                        />
                    </div>

                    {!isEditing && (
                        <div className="flex flex-col gap-2">
                            <div className="flex items-center gap-1">
                                <Label>Type</Label>

                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <InfoIcon className="size-3.5 text-muted-foreground" />
                                    </TooltipTrigger>

                                    <TooltipContent className="max-w-64" side="right">
                                        The evaluation method the judge uses. LLM Rule uses an AI model to assess the
                                        response. Contains Text and Regex Match check for specific patterns. Response
                                        Length validates output size. JSON Schema validates structure. Similarity
                                        compares against expected output.
                                    </TooltipContent>
                                </Tooltip>
                            </div>

                            <div className="flex flex-wrap gap-2">
                                {JUDGE_TYPE_OPTIONS.map((option) => (
                                    <button
                                        className={twMerge(
                                            'rounded-md border px-3 py-1.5 text-sm',
                                            judgeType === option.value
                                                ? 'border-blue-500 bg-blue-50 text-blue-700'
                                                : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                                        )}
                                        key={option.value}
                                        onClick={() => setJudgeType(option.value)}
                                        type="button"
                                    >
                                        {option.label}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    {isLlmRule && (
                        <>
                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label>Provider</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            The AI provider whose model will evaluate the agent's response. Choose the
                                            provider that hosts the LLM you want to use as a judge.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <Select onValueChange={handleProviderChange} value={provider}>
                                    <SelectTrigger>
                                        <SelectValue placeholder="Select a provider..." />
                                    </SelectTrigger>

                                    <SelectContent>
                                        {modelProviders.map((modelProvider) => (
                                            <SelectItem
                                                key={modelProvider.componentName}
                                                value={modelProvider.componentName}
                                            >
                                                {modelProvider.title || modelProvider.componentName}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>

                            {provider && (
                                <div className="flex flex-col gap-2">
                                    <div className="flex items-center gap-1">
                                        <Label htmlFor="judge-model">Model</Label>

                                        <Tooltip>
                                            <TooltipTrigger asChild>
                                                <InfoIcon className="size-3.5 text-muted-foreground" />
                                            </TooltipTrigger>

                                            <TooltipContent className="max-w-64" side="right">
                                                The specific LLM that will act as the judge. More capable models
                                                generally produce more accurate and nuanced evaluations but may cost
                                                more per run.
                                            </TooltipContent>
                                        </Tooltip>
                                    </div>

                                    {modelOptions.length > 0 ? (
                                        <Select onValueChange={setModel} value={model}>
                                            <SelectTrigger>
                                                <SelectValue placeholder="Select a model..." />
                                            </SelectTrigger>

                                            <SelectContent>
                                                {modelOptions.map((option) => (
                                                    <SelectItem key={String(option.value)} value={String(option.value)}>
                                                        {option.label || String(option.value)}
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                    ) : (
                                        <Input
                                            id="judge-model"
                                            onChange={(event) => setModel(event.target.value)}
                                            placeholder="e.g. gpt-4o, claude-sonnet-4-20250514"
                                            value={model}
                                        />
                                    )}
                                </div>
                            )}

                            {provider && (
                                <div className="flex flex-col gap-2">
                                    <div className="flex items-center gap-1">
                                        <Label>Connection</Label>

                                        <Tooltip>
                                            <TooltipTrigger asChild>
                                                <InfoIcon className="size-3.5 text-muted-foreground" />
                                            </TooltipTrigger>

                                            <TooltipContent className="max-w-64" side="right">
                                                The API connection used to authenticate with the selected provider. Make
                                                sure the connection has valid credentials and sufficient quota for
                                                running evaluations.
                                            </TooltipContent>
                                        </Tooltip>
                                    </div>

                                    <Select onValueChange={setConnectionId} value={connectionId}>
                                        <SelectTrigger>
                                            <SelectValue placeholder="Select a connection..." />
                                        </SelectTrigger>

                                        <SelectContent>
                                            {allConnections.map((connection) => (
                                                <SelectItem key={connection.id} value={String(connection.id)}>
                                                    {connection.name}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>
                            )}

                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label htmlFor="judge-rule">Evaluation Rule</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            A natural-language instruction that tells the LLM judge how to evaluate the
                                            agent's response. Be specific about what constitutes a pass or fail to get
                                            consistent results.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <Textarea
                                    id="judge-rule"
                                    onChange={(event) => setRule(event.target.value)}
                                    placeholder="Describe the rule for evaluating the agent response"
                                    rows={4}
                                    value={rule}
                                />
                            </div>
                        </>
                    )}

                    {judgeType === AgentJudgeType.ContainsText && (
                        <>
                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label htmlFor="judge-text">Text</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            The exact text string to look for in the agent's response. The check is
                                            case-sensitive and looks for a substring match anywhere in the output.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <Input
                                    id="judge-text"
                                    onChange={(event) => setSearchText(event.target.value)}
                                    placeholder="Enter text to search for"
                                    value={searchText}
                                />
                            </div>

                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label>Mode</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            "Must Contain" passes when the text is found in the response. "Must Not
                                            Contain" passes when the text is absent, useful for checking that the agent
                                            avoids forbidden words or phrases.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <div className="flex gap-2">
                                    <button
                                        className={twMerge(
                                            'rounded-md border px-3 py-1.5 text-sm',
                                            !mustNotContain
                                                ? 'border-blue-500 bg-blue-50 text-blue-700'
                                                : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                                        )}
                                        onClick={() => setMustNotContain(false)}
                                        type="button"
                                    >
                                        Must Contain
                                    </button>

                                    <button
                                        className={twMerge(
                                            'rounded-md border px-3 py-1.5 text-sm',
                                            mustNotContain
                                                ? 'border-blue-500 bg-blue-50 text-blue-700'
                                                : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                                        )}
                                        onClick={() => setMustNotContain(true)}
                                        type="button"
                                    >
                                        Must Not Contain
                                    </button>
                                </div>
                            </div>
                        </>
                    )}

                    {judgeType === AgentJudgeType.RegexMatch && (
                        <>
                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label htmlFor="judge-pattern">Pattern</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            A regular expression pattern to test against the agent's response. Use
                                            standard regex syntax (e.g., \d+ for digits, ^Hello for starts with
                                            "Hello").
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <Input
                                    id="judge-pattern"
                                    onChange={(event) => setRegexPattern(event.target.value)}
                                    placeholder="Enter regex pattern"
                                    value={regexPattern}
                                />
                            </div>

                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label>Mode</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            "Must Match" passes when the regex finds a match in the response. "Must Not
                                            Match" passes when no match is found, useful for ensuring the agent avoids
                                            certain output patterns.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <div className="flex gap-2">
                                    <button
                                        className={twMerge(
                                            'rounded-md border px-3 py-1.5 text-sm',
                                            !mustNotMatch
                                                ? 'border-blue-500 bg-blue-50 text-blue-700'
                                                : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                                        )}
                                        onClick={() => setMustNotMatch(false)}
                                        type="button"
                                    >
                                        Must Match
                                    </button>

                                    <button
                                        className={twMerge(
                                            'rounded-md border px-3 py-1.5 text-sm',
                                            mustNotMatch
                                                ? 'border-blue-500 bg-blue-50 text-blue-700'
                                                : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                                        )}
                                        onClick={() => setMustNotMatch(true)}
                                        type="button"
                                    >
                                        Must Not Match
                                    </button>
                                </div>
                            </div>
                        </>
                    )}

                    {judgeType === AgentJudgeType.ResponseLength && (
                        <div className="flex gap-4">
                            <div className="flex flex-1 flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label htmlFor="judge-min-length">Min Length (optional)</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            The minimum number of characters the agent's response must contain to pass.
                                            Leave empty to allow any length. Useful for ensuring the agent provides
                                            sufficiently detailed answers.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <Input
                                    id="judge-min-length"
                                    min={0}
                                    onChange={(event) => setMinLength(event.target.value)}
                                    placeholder="0"
                                    type="number"
                                    value={minLength}
                                />
                            </div>

                            <div className="flex flex-1 flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label htmlFor="judge-max-length">Max Length (optional)</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            The maximum number of characters allowed in the agent's response. Leave
                                            empty for no upper limit. Useful for enforcing concise answers.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <Input
                                    id="judge-max-length"
                                    min={0}
                                    onChange={(event) => setMaxLength(event.target.value)}
                                    placeholder="No limit"
                                    type="number"
                                    value={maxLength}
                                />
                            </div>
                        </div>
                    )}

                    {judgeType === AgentJudgeType.JsonSchema && (
                        <div className="flex flex-col gap-2">
                            <div className="flex items-center gap-1">
                                <Label htmlFor="judge-schema">JSON Schema</Label>

                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <InfoIcon className="size-3.5 text-muted-foreground" />
                                    </TooltipTrigger>

                                    <TooltipContent className="max-w-64" side="right">
                                        A JSON Schema definition that the agent's response must conform to. The response
                                        is parsed as JSON and validated against this schema. Useful for ensuring
                                        structured output with required fields and correct types.
                                    </TooltipContent>
                                </Tooltip>
                            </div>

                            <Textarea
                                id="judge-schema"
                                onChange={(event) => {
                                    setJsonSchema(event.target.value);
                                    setJsonSchemaError('');
                                }}
                                placeholder='{"type": "object", "properties": {...}}'
                                rows={6}
                                value={jsonSchema}
                            />

                            {jsonSchemaError && <p className="text-xs text-red-500">{jsonSchemaError}</p>}
                        </div>
                    )}

                    {judgeType === AgentJudgeType.Similarity && (
                        <>
                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label htmlFor="judge-expected-output">Expected Output</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            The reference text to compare the agent's response against. The similarity
                                            score measures how closely the actual response matches this expected output.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <Textarea
                                    id="judge-expected-output"
                                    onChange={(event) => setExpectedOutput(event.target.value)}
                                    placeholder="Enter the expected output to compare against"
                                    rows={3}
                                    value={expectedOutput}
                                />
                            </div>

                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label htmlFor="judge-threshold">Threshold (0-1)</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            The minimum similarity score required to pass. A value of 1.0 requires an
                                            exact match, while lower values allow more variation. 0.8 is a good starting
                                            point for most use cases.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <Input
                                    id="judge-threshold"
                                    max={1}
                                    min={0}
                                    onChange={(event) => setThreshold(event.target.value)}
                                    step={0.05}
                                    type="number"
                                    value={threshold}
                                />
                            </div>

                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label>Algorithm</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            The method used to compute similarity. Cosine measures the angle between
                                            text vectors and works well for semantic similarity. Edit Distance counts
                                            the character-level changes needed and is better for exact wording
                                            comparisons.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <div className="flex gap-2">
                                    <button
                                        className={twMerge(
                                            'rounded-md border px-3 py-1.5 text-sm',
                                            algorithm === 'COSINE'
                                                ? 'border-blue-500 bg-blue-50 text-blue-700'
                                                : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                                        )}
                                        onClick={() => setAlgorithm('COSINE')}
                                        type="button"
                                    >
                                        Cosine
                                    </button>

                                    <button
                                        className={twMerge(
                                            'rounded-md border px-3 py-1.5 text-sm',
                                            algorithm === 'EDIT_DISTANCE'
                                                ? 'border-blue-500 bg-blue-50 text-blue-700'
                                                : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                                        )}
                                        onClick={() => setAlgorithm('EDIT_DISTANCE')}
                                        type="button"
                                    >
                                        Edit Distance
                                    </button>
                                </div>
                            </div>
                        </>
                    )}

                    {judgeType === AgentJudgeType.StringEquals && (
                        <>
                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label htmlFor="judge-expected-value">Expected Value</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            The exact text the agent response must match.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <Input
                                    id="judge-expected-value"
                                    onChange={(event) => setExpectedValue(event.target.value)}
                                    placeholder="Enter the expected value"
                                    value={expectedValue}
                                />
                            </div>

                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label>Case Sensitive</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            When enabled, the comparison is case-sensitive.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <div className="flex gap-2">
                                    <button
                                        className={twMerge(
                                            'rounded-md border px-3 py-1.5 text-sm',
                                            caseSensitive
                                                ? 'border-blue-500 bg-blue-50 text-blue-700'
                                                : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                                        )}
                                        onClick={() => setCaseSensitive(true)}
                                        type="button"
                                    >
                                        Yes
                                    </button>

                                    <button
                                        className={twMerge(
                                            'rounded-md border px-3 py-1.5 text-sm',
                                            !caseSensitive
                                                ? 'border-blue-500 bg-blue-50 text-blue-700'
                                                : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                                        )}
                                        onClick={() => setCaseSensitive(false)}
                                        type="button"
                                    >
                                        No
                                    </button>
                                </div>
                            </div>
                        </>
                    )}

                    {judgeType === AgentJudgeType.ToolUsage && (
                        <>
                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label htmlFor="judge-tool-name">Tool Name</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            The name of the tool that should be used during the conversation.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <Input
                                    id="judge-tool-name"
                                    onChange={(event) => setToolName(event.target.value)}
                                    placeholder="Enter tool name"
                                    value={toolName}
                                />
                            </div>

                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label>Position</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            Where in the conversation the tool should appear.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <Select onValueChange={setToolPosition} value={toolPosition}>
                                    <SelectTrigger>
                                        <SelectValue placeholder="Select position..." />
                                    </SelectTrigger>

                                    <SelectContent>
                                        <SelectItem value="ANYWHERE">Anywhere</SelectItem>

                                        <SelectItem value="FIRST">First</SelectItem>

                                        <SelectItem value="LAST">Last</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>

                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label>Comparison</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            How to compare the actual tool usage count against the expected count.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <Select onValueChange={setToolComparison} value={toolComparison}>
                                    <SelectTrigger>
                                        <SelectValue placeholder="Select comparison..." />
                                    </SelectTrigger>

                                    <SelectContent>
                                        <SelectItem value="AT_LEAST">At Least</SelectItem>

                                        <SelectItem value="EXACTLY">Exactly</SelectItem>

                                        <SelectItem value="AT_MOST">At Most</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>

                            <div className="flex flex-col gap-2">
                                <div className="flex items-center gap-1">
                                    <Label htmlFor="judge-tool-count">Count</Label>

                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoIcon className="size-3.5 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-64" side="right">
                                            The expected number of times the tool should be used.
                                        </TooltipContent>
                                    </Tooltip>
                                </div>

                                <Input
                                    id="judge-tool-count"
                                    min={1}
                                    onChange={(event) => setToolCount(Number(event.target.value))}
                                    type="number"
                                    value={toolCount}
                                />
                            </div>
                        </>
                    )}
                </fieldset>

                <DialogFooter>
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button disabled={!name.trim()} label={isEditing ? 'Save' : 'Create'} onClick={handleSubmit} />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default CreateJudgeDialog;
