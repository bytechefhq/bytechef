import Button from '@/components/Button/Button';
import {AutomationGettingStartedStatusType} from '@/pages/automation/getting-started/automationGettingStartedStatus';
import FirstRunStepContent from '@/pages/automation/getting-started/components/FirstRunStepContent';
import {FirstRunStateType} from '@/pages/automation/getting-started/getAutomationGettingStartedSteps';
import WorkflowExecutionContentClipboardButton from '@/shared/components/workflow-executions/WorkflowExecutionContentClipboardButton';
import {CheckIcon, CircleIcon, Loader2Icon, PlayIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

import {EditorGuideItemI} from './getEditorGuideItems';

interface EditorGuideItemRowProps {
    actionDisabled: boolean;
    actionLabel?: string;
    firstRunState: FirstRunStateType;
    item: EditorGuideItemI;
    onAction?: () => void;
    onRunNow: () => void;
    runNowVisible: boolean;
    runPending: boolean;
    runWaiting: boolean;
    showFirstRun: boolean;
    status?: AutomationGettingStartedStatusType;
}

const EditorGuideItemRow = ({
    actionDisabled,
    actionLabel,
    firstRunState,
    item,
    onAction,
    onRunNow,
    runNowVisible,
    runPending,
    runWaiting,
    showFirstRun,
    status,
}: EditorGuideItemRowProps) => (
    <li className="space-y-1">
        <div className="flex items-center gap-2 text-sm">
            {item.done ? (
                <CheckIcon aria-label="Done" className="size-4 shrink-0 text-content-success-primary" />
            ) : (
                <CircleIcon className="size-4 shrink-0 text-content-neutral-tertiary" />
            )}

            <span className={twMerge('font-medium', item.done && 'text-content-neutral-secondary line-through')}>
                {item.label}
            </span>

            {!item.done && actionLabel && onAction && (
                <div className="ml-auto shrink-0">
                    <Button
                        disabled={actionDisabled}
                        label={actionLabel}
                        onClick={onAction}
                        size="xs"
                        variant="outline"
                    />
                </div>
            )}
        </div>

        {!item.done && <p className="pl-6 text-xs text-content-neutral-secondary">{item.hint}</p>}

        {!item.done && item.copyValue && (
            <div className="ml-6 flex items-center gap-1 rounded-md border border-stroke-neutral-secondary bg-surface-neutral-secondary py-0.5 pr-0.5 pl-2">
                {item.copyLabel && (
                    <span className="shrink-0 text-xs text-content-neutral-secondary">{item.copyLabel}</span>
                )}

                <code className="min-w-0 flex-1 truncate text-xs text-content-neutral-primary">{item.copyValue}</code>

                <WorkflowExecutionContentClipboardButton value={item.copyValue} />
            </div>
        )}

        {showFirstRun && (
            <div className="pl-6">
                {runWaiting && firstRunState === 'waiting' ? (
                    <div
                        className="mt-2 flex items-center gap-2 rounded-md border border-stroke-neutral-secondary p-3 text-sm font-medium text-content-neutral-primary"
                        data-testid="run-waiting"
                    >
                        <Loader2Icon className="size-4 animate-spin text-content-neutral-secondary" />
                        Waiting...
                    </div>
                ) : (
                    <>
                        <FirstRunStepContent
                            executionsLinkVisible={false}
                            firstRun={status?.firstRun ?? null}
                            firstRunState={firstRunState}
                            lastFailedRun={status?.lastFailedRun ?? null}
                            waitingHint=""
                        />

                        {runNowVisible && (
                            <Button
                                className="mt-2 [&_svg]:size-3"
                                disabled={runPending}
                                icon={<PlayIcon className="text-success" />}
                                label="Run now"
                                onClick={onRunNow}
                                size="xs"
                                variant="outline"
                            />
                        )}
                    </>
                )}
            </div>
        )}
    </li>
);

export default EditorGuideItemRow;
