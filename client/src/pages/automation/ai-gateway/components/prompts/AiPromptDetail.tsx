import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import {useAiPromptQuery, useSetActiveAiPromptVersionMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ArrowLeftIcon, GitCompareIcon, PlusIcon, RocketIcon, XIcon} from 'lucide-react';
import {useCallback, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {AiPromptVersionType} from '../../types';
import AiPromptVersionDialog from './AiPromptVersionDialog';

interface AiPromptDetailProps {
    onBack: () => void;
    promptId: string;
}

interface DiffLineI {
    line: string;
    type: 'added' | 'removed' | 'same';
}

function simpleDiff(a: string, b: string): DiffLineI[] {
    const aLines = a.split('\n');
    const bLines = b.split('\n');
    const result: DiffLineI[] = [];
    const maxLen = Math.max(aLines.length, bLines.length);

    for (let lineIndex = 0; lineIndex < maxLen; lineIndex++) {
        if (aLines[lineIndex] === bLines[lineIndex]) {
            result.push({line: aLines[lineIndex] ?? '', type: 'same'});
        } else {
            if (lineIndex < aLines.length) {
                result.push({line: aLines[lineIndex], type: 'removed'});
            }

            if (lineIndex < bLines.length) {
                result.push({line: bLines[lineIndex], type: 'added'});
            }
        }
    }

    return result;
}

const ENVIRONMENTS = ['production', 'staging', 'development'];

const AiPromptDetail = ({onBack, promptId}: AiPromptDetailProps) => {
    const [compareSelection, setCompareSelection] = useState<string[]>([]);
    const [showCompareDialog, setShowCompareDialog] = useState(false);
    const [showVersionDialog, setShowVersionDialog] = useState(false);

    const queryClient = useQueryClient();

    const {data: promptData, isLoading: promptIsLoading} = useAiPromptQuery({id: promptId});

    const setActiveMutation = useSetActiveAiPromptVersionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiPrompt']});
            queryClient.invalidateQueries({queryKey: ['aiPrompts']});
        },
    });

    const prompt = promptData?.aiPrompt;

    const versions = useMemo(() => prompt?.versions ?? [], [prompt?.versions]);

    const activeVersionsByEnvironment = useMemo(() => {
        const result: Record<string, AiPromptVersionType | undefined> = {};

        for (const env of ENVIRONMENTS) {
            result[env] = versions.find((version) => version && version.environment === env && version.active) as
                | AiPromptVersionType
                | undefined;
        }

        return result;
    }, [versions]);

    const handleDeploy = useCallback(
        (version: AiPromptVersionType, environment: string) => {
            setActiveMutation.mutate({
                environment,
                promptVersionId: version.id,
            });
        },
        [setActiveMutation]
    );

    const handleToggleCompare = useCallback((versionId: string) => {
        setCompareSelection((previous) => {
            if (previous.includes(versionId)) {
                return previous.filter((selectedId) => selectedId !== versionId);
            }

            if (previous.length >= 2) {
                return [previous[1], versionId];
            }

            return [...previous, versionId];
        });
    }, []);

    const compareVersions = useMemo(() => {
        if (compareSelection.length !== 2) {
            return null;
        }

        const left = versions.find((version) => version?.id === compareSelection[0]) as AiPromptVersionType | undefined;
        const right = versions.find((version) => version?.id === compareSelection[1]) as
            | AiPromptVersionType
            | undefined;

        if (!left || !right) {
            return null;
        }

        return {diff: simpleDiff(left.content, right.content), left, right};
    }, [compareSelection, versions]);

    if (promptIsLoading) {
        return <PageLoader loading={true} />;
    }

    if (!prompt) {
        return null;
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <div className="mb-6 flex items-center gap-4 py-4">
                <button className="text-muted-foreground hover:text-foreground" onClick={onBack}>
                    <ArrowLeftIcon className="size-5" />
                </button>

                <div className="flex-1">
                    <h2 className="text-xl font-semibold">{prompt.name}</h2>

                    {prompt.description && <p className="text-sm text-muted-foreground">{prompt.description}</p>}
                </div>

                <Button
                    icon={<PlusIcon className="size-4" />}
                    label="New Version"
                    onClick={() => setShowVersionDialog(true)}
                />
            </div>

            <div className="mb-8 grid grid-cols-3 gap-4">
                {ENVIRONMENTS.map((environment) => {
                    const activeVersion = activeVersionsByEnvironment[environment];

                    return (
                        <div className="rounded-lg border p-4" key={environment}>
                            <h3 className="mb-2 text-sm font-medium capitalize">{environment}</h3>

                            {activeVersion ? (
                                <div className="flex items-center gap-2">
                                    <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-800">
                                        v{activeVersion.versionNumber}
                                    </span>

                                    <span className="text-xs text-muted-foreground">Active</span>
                                </div>
                            ) : (
                                <span className="text-xs text-muted-foreground">No active version</span>
                            )}
                        </div>
                    );
                })}
            </div>

            <div>
                <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-lg font-medium">Version History</h3>

                    <div className="flex items-center gap-2">
                        {compareSelection.length > 0 && (
                            <span className="text-xs text-muted-foreground">{compareSelection.length}/2 selected</span>
                        )}

                        <Button
                            disabled={compareSelection.length !== 2}
                            icon={<GitCompareIcon className="size-4" />}
                            label="Compare"
                            onClick={() => setShowCompareDialog(true)}
                            variant="outline"
                        />

                        {compareSelection.length > 0 && (
                            <Button label="Clear" onClick={() => setCompareSelection([])} variant="outline" />
                        )}
                    </div>
                </div>

                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead>
                            <tr className="border-b text-muted-foreground">
                                <th className="pb-2 font-medium">Version</th>

                                <th className="pb-2 font-medium">Environment</th>

                                <th className="pb-2 font-medium">Status</th>

                                <th className="pb-2 font-medium">Type</th>

                                <th className="pb-2 font-medium">Commit Message</th>

                                <th className="pb-2 font-medium">Created By</th>

                                <th className="pb-2 font-medium">Created</th>

                                <th className="pb-2 font-medium">Content</th>

                                <th className="pb-2 font-medium">Metrics</th>

                                <th className="pb-2 font-medium">Actions</th>
                            </tr>
                        </thead>

                        <tbody>
                            {versions.map((version) =>
                                version ? (
                                    <tr className="border-b" key={version.id}>
                                        <td className="py-3 font-medium">v{version.versionNumber}</td>

                                        <td className="py-3 capitalize">{version.environment || '-'}</td>

                                        <td className="py-3">
                                            <span
                                                className={twMerge(
                                                    'rounded-full px-2 py-0.5 text-xs font-medium',
                                                    version.active
                                                        ? 'bg-green-100 text-green-800'
                                                        : 'bg-gray-100 text-gray-800'
                                                )}
                                            >
                                                {version.active ? 'Active' : 'Inactive'}
                                            </span>
                                        </td>

                                        <td className="py-3">{version.type}</td>

                                        <td className="py-3 text-muted-foreground">{version.commitMessage || '-'}</td>

                                        <td className="py-3">{version.createdBy}</td>

                                        <td className="py-3 text-muted-foreground">
                                            {version.createdDate
                                                ? new Date(version.createdDate).toLocaleDateString()
                                                : '-'}
                                        </td>

                                        <td className="max-w-48 truncate py-3 text-muted-foreground">
                                            {version.content.substring(0, 50)}

                                            {version.content.length > 50 ? '...' : ''}
                                        </td>

                                        <td className="py-3 text-xs text-muted-foreground">
                                            {version.metrics && version.metrics.invocationCount > 0 ? (
                                                <div className="flex flex-col gap-0.5">
                                                    <span>{version.metrics.invocationCount} calls</span>

                                                    {version.metrics.avgLatencyMs != null && (
                                                        <span>{Math.round(version.metrics.avgLatencyMs)}ms avg</span>
                                                    )}

                                                    {version.metrics.avgCostUsd != null && (
                                                        <span>${version.metrics.avgCostUsd.toFixed(4)} avg</span>
                                                    )}

                                                    {version.metrics.errorRate != null &&
                                                        version.metrics.errorRate > 0 && (
                                                            <span className="text-red-600">
                                                                {(version.metrics.errorRate * 100).toFixed(1)}% errors
                                                            </span>
                                                        )}
                                                </div>
                                            ) : (
                                                <span>-</span>
                                            )}
                                        </td>

                                        <td className="py-3">
                                            <div className="flex items-center gap-3">
                                                {!version.active && version.environment && (
                                                    <button
                                                        className="flex items-center gap-1 text-xs text-blue-600 hover:text-blue-800"
                                                        disabled={setActiveMutation.isPending}
                                                        onClick={() =>
                                                            handleDeploy(
                                                                version as AiPromptVersionType,
                                                                version.environment!
                                                            )
                                                        }
                                                    >
                                                        <RocketIcon className="size-3" />
                                                        Deploy
                                                    </button>
                                                )}

                                                <button
                                                    className={twMerge(
                                                        'flex items-center gap-1 text-xs hover:text-blue-800',
                                                        compareSelection.includes(version.id)
                                                            ? 'font-semibold text-blue-800'
                                                            : 'text-blue-600'
                                                    )}
                                                    onClick={() => handleToggleCompare(version.id)}
                                                    title={
                                                        compareSelection.includes(version.id)
                                                            ? 'Deselect for comparison'
                                                            : 'Select for comparison'
                                                    }
                                                >
                                                    <GitCompareIcon className="size-3" />

                                                    {compareSelection.includes(version.id) ? 'Selected' : 'Compare'}
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ) : null
                            )}
                        </tbody>
                    </table>

                    {versions.length === 0 && (
                        <p className="py-8 text-center text-sm text-muted-foreground">
                            No versions yet. Create one to get started.
                        </p>
                    )}
                </div>
            </div>

            {showVersionDialog && (
                <AiPromptVersionDialog onClose={() => setShowVersionDialog(false)} promptId={promptId} />
            )}

            {showCompareDialog && compareVersions && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
                    <div className="flex max-h-[85vh] w-full max-w-4xl flex-col rounded-lg bg-background p-6 shadow-lg">
                        <div className="mb-4 flex items-center justify-between">
                            <h3 className="text-lg font-medium">
                                {`Compare v${compareVersions.left.versionNumber} \u2192 v${compareVersions.right.versionNumber}`}
                            </h3>

                            <button onClick={() => setShowCompareDialog(false)}>
                                <XIcon className="size-4" />
                            </button>
                        </div>

                        <div className="flex-1 overflow-auto rounded border bg-muted/20 font-mono text-xs">
                            {compareVersions.diff.map((diffLine, diffIndex) => (
                                <div
                                    className={twMerge(
                                        'whitespace-pre-wrap px-3 py-0.5',
                                        diffLine.type === 'added' && 'bg-green-100 text-green-900',
                                        diffLine.type === 'removed' && 'bg-red-100 text-red-900'
                                    )}
                                    key={diffIndex}
                                >
                                    <span className="mr-2 inline-block w-4 select-none text-muted-foreground">
                                        {diffLine.type === 'added' ? '+' : diffLine.type === 'removed' ? '-' : ' '}
                                    </span>

                                    {diffLine.line || '\u00A0'}
                                </div>
                            ))}
                        </div>

                        <div className="mt-4 flex justify-end">
                            <Button label="Close" onClick={() => setShowCompareDialog(false)} variant="outline" />
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AiPromptDetail;
