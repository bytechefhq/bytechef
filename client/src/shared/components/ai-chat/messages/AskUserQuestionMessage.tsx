import Button from '@/components/Button/Button';
import ComboBox from '@/components/ComboBox/ComboBox';
import {MultiSelect} from '@/components/MultiSelect/MultiSelect';
import {Input} from '@/components/ui/input';
import {useAiChatAskedQuestionsStore} from '@/shared/components/ai-chat/stores/useAiChatAskedQuestionsStore';
import {DataMessagePartProps, useThreadRuntime} from '@assistant-ui/react';
import {ArrowLeftIcon, CheckIcon, XIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

export interface AskUserQuestionOptionDataI {
    description?: string;
    label: string;
}

export interface AskUserQuestionDataI {
    awaitingAnswer?: boolean;
    kind: 'ask-user-question';
    questions: Array<{
        header?: string;
        multiSelect: boolean;
        options: AskUserQuestionOptionDataI[];
        question: string;
    }>;
}

const OTHER_OPTION_LABEL = '__other__';

/** Above this many options, single/multi select render a searchable control instead of a stacked list. */
export const COMBOBOX_OPTION_THRESHOLD = 8;

/** Match LLM-supplied option labels that should trigger the free-form input instead of submitting the literal text. */
const isOtherLabel = (label: string) => /^other\b/i.test(label.trim());

/**
 * Renders the LLM's askUserQuestion tool result: one question is a single card, multiple become a wizard
 * that collects all answers and submits them as one combined user message (persisted to Spring AI chat
 * memory, so picks survive a refresh). An "Other" option opens a free-form input, deduplicated against any
 * "Other" the LLM already supplied. Answers are keyed in {@link useAiChatAskedQuestionsStore} by a
 * fingerprint of the questions array so re-mounting replays the summary instead of re-prompting.
 */
const AskUserQuestionMessage = ({data}: DataMessagePartProps<AskUserQuestionDataI>) => {
    const questions = useMemo(() => data.questions ?? [], [data.questions]);

    const fingerprint = useMemo(() => fingerprintQuestions(questions), [questions]);

    const isAnswered = useAiChatAskedQuestionsStore((state) => state.hasAnswered(fingerprint));
    const persistedAnswer = useAiChatAskedQuestionsStore((state) => state.getAnswer(fingerprint));
    const markAnswered = useAiChatAskedQuestionsStore((state) => state.markAnswered);

    const [stepIndex, setStepIndex] = useState(0);
    const [answers, setAnswers] = useState<Record<number, string>>({});

    const threadRuntime = useThreadRuntime();

    if (questions.length === 0) {
        return null;
    }

    if (isAnswered) {
        return <AnsweredSummary persistedAnswer={persistedAnswer} />;
    }

    const totalSteps = questions.length;
    const currentQuestion = questions[stepIndex];
    const isLastStep = stepIndex === totalSteps - 1;

    const submitStep = (answer: string) => {
        const nextAnswers = {...answers, [stepIndex]: answer};

        setAnswers(nextAnswers);

        if (!isLastStep) {
            setStepIndex(stepIndex + 1);

            return;
        }

        const summary = buildAnswerSummary(questions, nextAnswers);
        const messageText = totalSteps === 1 ? `User picked: ${answer}` : `User picked:\n${summary}`;

        markAnswered(fingerprint, summary);

        threadRuntime.append({
            content: [{text: messageText, type: 'text'}],
            role: 'user',
        });
    };

    const goBack = () => {
        if (stepIndex > 0) {
            setStepIndex(stepIndex - 1);
        }
    };

    return (
        <div className="mt-2 flex flex-col gap-3 rounded-md border border-border bg-muted/30 p-3">
            <WizardHeader header={currentQuestion.header} stepIndex={stepIndex} totalSteps={totalSteps} />

            <div className="text-sm">{currentQuestion.question}</div>

            <StepBody
                initialAnswer={answers[stepIndex]}
                isLastStep={isLastStep}
                onSubmit={submitStep}
                question={currentQuestion}
            />

            {stepIndex > 0 && (
                <div className="flex justify-end">
                    <button
                        className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground"
                        onClick={goBack}
                        type="button"
                    >
                        <ArrowLeftIcon className="size-3" />
                        Previous
                    </button>
                </div>
            )}
        </div>
    );
};

type WizardHeaderPropsType = {
    header?: string;
    stepIndex: number;
    totalSteps: number;
};

const WizardHeader = ({header, stepIndex, totalSteps}: WizardHeaderPropsType) => {
    if (totalSteps === 1) {
        if (!header) {
            return null;
        }

        return <div className="text-xs font-semibold text-muted-foreground uppercase">{header}</div>;
    }

    return (
        <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span>
                Question {stepIndex + 1} of {totalSteps}
            </span>

            {header && <span className="font-semibold uppercase">{header}</span>}
        </div>
    );
};

type StepBodyPropsType = {
    initialAnswer?: string;
    isLastStep: boolean;
    onSubmit: (answer: string) => void;
    question: AskUserQuestionDataI['questions'][number];
};

const StepBody = ({initialAnswer, isLastStep, onSubmit, question}: StepBodyPropsType) => {
    if (question.multiSelect) {
        return (
            <MultiSelectStep
                initialAnswer={initialAnswer}
                isLastStep={isLastStep}
                onSubmit={onSubmit}
                question={question}
            />
        );
    }

    return <SingleSelectStep isLastStep={isLastStep} onSubmit={onSubmit} question={question} />;
};

type SingleSelectStepPropsType = {
    isLastStep: boolean;
    onSubmit: (answer: string) => void;
    question: AskUserQuestionDataI['questions'][number];
};

const SingleSelectStep = ({isLastStep, onSubmit, question}: SingleSelectStepPropsType) => {
    const [otherTyping, setOtherTyping] = useState(false);
    const [otherValue, setOtherValue] = useState('');

    // Suppress our injected "Other…" affordance if the LLM already supplied an Other-style option — keeps the
    // UI from showing two duplicate "Other" entries side by side.
    const llmSuppliedOther = question.options.some((option) => isOtherLabel(option.label));

    const handleClick = (label: string) => {
        if (label === OTHER_OPTION_LABEL || isOtherLabel(label)) {
            setOtherTyping(true);

            return;
        }

        onSubmit(label);
    };

    const handleOtherSubmit = () => {
        const trimmed = otherValue.trim();

        if (trimmed.length === 0) {
            return;
        }

        onSubmit(trimmed);
    };

    if (otherTyping) {
        return (
            <div className="flex gap-2">
                <Input
                    autoFocus
                    className="flex-1"
                    onChange={(event) => setOtherValue(event.target.value)}
                    onKeyDown={(event) => {
                        if (event.key === 'Enter') {
                            event.preventDefault();
                            handleOtherSubmit();
                        }

                        if (event.key === 'Escape') {
                            event.preventDefault();

                            setOtherTyping(false);
                            setOtherValue('');
                        }
                    }}
                    placeholder="Type your answer…"
                    value={otherValue}
                />

                <Button
                    disabled={otherValue.trim().length === 0}
                    label={isLastStep ? 'Submit all' : 'Next'}
                    onClick={handleOtherSubmit}
                />

                <Button
                    icon={<XIcon />}
                    onClick={() => {
                        setOtherTyping(false);
                        setOtherValue('');
                    }}
                    size="icon"
                    title="Cancel and go back to the option buttons"
                    variant="outline"
                />
            </div>
        );
    }

    if (question.options.length > COMBOBOX_OPTION_THRESHOLD) {
        const comboBoxItems = question.options.map((option) => ({label: option.label, value: option.label}));

        if (!llmSuppliedOther) {
            comboBoxItems.push({label: 'Other…', value: OTHER_OPTION_LABEL});
        }

        return (
            <ComboBox
                emptyMessage="No match"
                items={comboBoxItems}
                onChange={(item) => {
                    if (item) {
                        handleClick(item.value as string);
                    }
                }}
                value={undefined}
            />
        );
    }

    return (
        <div className="flex flex-col items-start gap-2">
            {question.options.map((option) => (
                <Button
                    key={option.label}
                    label={option.label}
                    onClick={() => handleClick(option.label)}
                    title={option.description}
                    variant="outline"
                />
            ))}

            {!llmSuppliedOther && (
                <Button
                    label="Other…"
                    onClick={() => setOtherTyping(true)}
                    title="Type a free-form answer if none of the listed options fit"
                    variant="outline"
                />
            )}
        </div>
    );
};

type MultiSelectStepPropsType = {
    initialAnswer?: string;
    isLastStep: boolean;
    onSubmit: (answer: string) => void;
    question: AskUserQuestionDataI['questions'][number];
};

const MultiSelectStep = ({initialAnswer, isLastStep, onSubmit, question}: MultiSelectStepPropsType) => {
    // Restore checkbox state when the user navigates Previous → Next without changing their selection.
    const initialSet = useMemo(
        () => (initialAnswer ? new Set(initialAnswer.split(', ')) : new Set<string>()),
        [initialAnswer]
    );

    const [selectedLabels, setSelectedLabels] = useState<Set<string>>(initialSet);

    const toggle = (label: string) => {
        setSelectedLabels((previous) => {
            const next = new Set(previous);

            if (next.has(label)) {
                next.delete(label);
            } else {
                next.add(label);
            }

            return next;
        });
    };

    const handleSubmit = () => {
        if (selectedLabels.size === 0) {
            return;
        }

        onSubmit(Array.from(selectedLabels).join(', '));
    };

    if (question.options.length > COMBOBOX_OPTION_THRESHOLD) {
        const multiSelectOptions = question.options.map((option) => ({label: option.label, value: option.label}));

        return (
            <div className="flex flex-col gap-2">
                <MultiSelect
                    onValueChange={(values) => setSelectedLabels(new Set(values))}
                    options={multiSelectOptions}
                    value={Array.from(selectedLabels)}
                />

                <div className="flex justify-end">
                    <Button
                        disabled={selectedLabels.size === 0}
                        label={isLastStep ? 'Submit all' : 'Next'}
                        onClick={handleSubmit}
                    />
                </div>
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-2">
            {question.options.map((option) => (
                <label className="flex cursor-pointer items-start gap-2 text-sm hover:bg-muted/50" key={option.label}>
                    <input
                        checked={selectedLabels.has(option.label)}
                        className="mt-0.5"
                        onChange={() => toggle(option.label)}
                        type="checkbox"
                    />

                    <span className="flex flex-col">
                        <span className="font-medium">{option.label}</span>

                        {option.description && (
                            <span className="text-xs text-muted-foreground">{option.description}</span>
                        )}
                    </span>
                </label>
            ))}

            <div className="flex justify-end">
                <Button
                    disabled={selectedLabels.size === 0}
                    label={isLastStep ? 'Submit all' : 'Next'}
                    onClick={handleSubmit}
                />
            </div>
        </div>
    );
};

const AnsweredSummary = ({persistedAnswer}: {persistedAnswer?: string}) => {
    if (!persistedAnswer) {
        return (
            <div className="mt-2 flex items-center gap-2 rounded-md border border-border bg-muted/30 p-3 text-sm">
                <CheckIcon className="size-4 text-emerald-600" />

                <span>
                    Picked: <span className="font-medium">(answered)</span>
                </span>
            </div>
        );
    }

    // Multi-line summary means the wizard had multiple questions and we stashed the labeled
    // "- Q → A" list as the persisted answer. Render as a block so each Q/A pair is on its own line.
    const isMultiLine = persistedAnswer.includes('\n');

    if (!isMultiLine) {
        return (
            <div className="mt-2 flex items-center gap-2 rounded-md border border-border bg-muted/30 p-3 text-sm">
                <CheckIcon className="size-4 text-emerald-600" />

                <span>
                    Picked: <span className="font-medium">{persistedAnswer}</span>
                </span>
            </div>
        );
    }

    return (
        <div className="mt-2 flex items-start gap-2 rounded-md border border-border bg-muted/30 p-3 text-sm">
            <CheckIcon className="mt-0.5 size-4 shrink-0 text-emerald-600" />

            <div className="flex flex-col gap-1">
                <span className="font-medium">Picked:</span>

                <pre className="font-sans text-xs whitespace-pre-wrap text-muted-foreground">{persistedAnswer}</pre>
            </div>
        </div>
    );
};

function buildAnswerSummary(questions: AskUserQuestionDataI['questions'], answers: Record<number, string>): string {
    return questions.map((question, index) => `- ${question.question} → ${answers[index] ?? ''}`).join('\n');
}

function fingerprintQuestions(questions: AskUserQuestionDataI['questions']): string {
    return questions
        .map((question) => `${question.question}::${question.options.map((option) => option.label).join(',')}`)
        .join('||');
}

export default AskUserQuestionMessage;
