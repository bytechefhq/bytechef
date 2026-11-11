import Button from '@/components/Button/Button';
import {ChevronDownIcon} from 'lucide-react';

import EditorGuideItemRow from './EditorGuideItemRow';
import {EditorGuideItemIdType, isDeploymentLive} from './getEditorGuideItems';
import useEditorGettingStarted from './useEditorGettingStarted';
import useEditorGuideActions from './useEditorGuideActions';

interface EditorGettingStartedCardProps {
    projectId: number;
    projectWorkflowId: number;
}

const EditorGettingStartedCard = ({projectId, projectWorkflowId}: EditorGettingStartedCardProps) => {
    const {
        collapsed,
        completed,
        deployment,
        doneCount,
        firstRunExists,
        firstRunState,
        items,
        status,
        toggleCollapsed,
        visible,
    } = useEditorGettingStarted({projectId, projectWorkflowId});

    const actions = useEditorGuideActions({deployment});

    if (!visible) {
        return null;
    }

    const counter = `${doneCount}/${items.length}`;

    if (collapsed) {
        return (
            <Button
                aria-label="Expand getting started"
                className="rounded-full border bg-background shadow-md dark:shadow-black/40"
                label={`Getting started ${counter}`}
                onClick={toggleCollapsed}
                size="sm"
                variant="outline"
            />
        );
    }

    const isDone = (itemId: EditorGuideItemIdType) => !!items.find((item) => item.id === itemId)?.done;

    const itemActions: Partial<Record<EditorGuideItemIdType, {disabled: boolean; label: string; onClick: () => void}>> =
        {
            deploy: {disabled: !isDone('publish'), label: 'Deploy', onClick: actions.openDeploy},
            enable: {disabled: !isDone('deploy') || actions.enablePending, label: 'Enable', onClick: actions.enable},
            executions: {disabled: !isDone('run'), label: 'View executions', onClick: actions.openExecutions},
            publish: {disabled: false, label: 'Publish', onClick: actions.openPublish},
        };

    const deploymentLive = isDeploymentLive(deployment);
    const runFinished = firstRunExists || completed;

    return (
        <section
            aria-label="Getting started"
            className="flex max-h-full w-80 flex-col rounded-lg border bg-background p-3 shadow-md dark:shadow-black/40"
        >
            <header className="flex shrink-0 items-center gap-2">
                <h2 className="text-sm font-semibold">
                    {doneCount === items.length ? "You're set up" : 'Getting started'}
                </h2>

                <span className="text-xs text-content-neutral-secondary">{counter}</span>

                <div className="ml-auto flex items-center gap-1">
                    <Button
                        aria-label="Collapse getting started"
                        icon={<ChevronDownIcon />}
                        onClick={toggleCollapsed}
                        size="iconXs"
                        variant="ghost"
                    />
                </div>
            </header>

            <div className="min-h-0 overflow-y-auto">
                <ul className="mt-3 space-y-3">
                    {items.map((item) => {
                        const itemAction = itemActions[item.id];
                        const isRunItem = item.id === 'run';

                        return (
                            <EditorGuideItemRow
                                actionDisabled={itemAction?.disabled ?? false}
                                actionLabel={itemAction?.label}
                                firstRunState={runFinished ? 'done' : firstRunState}
                                item={item}
                                key={item.id}
                                onAction={itemAction?.onClick}
                                onRunNow={actions.runNow}
                                runNowVisible={isRunItem && deploymentLive && !runFinished}
                                runPending={actions.runPending}
                                runWaiting={actions.runWaiting}
                                showFirstRun={isRunItem && (deploymentLive || runFinished || !!status?.lastFailedRun)}
                                status={status}
                            />
                        );
                    })}
                </ul>
            </div>
        </section>
    );
};

export default EditorGettingStartedCard;
