import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {ChevronDownIcon, ChevronRightIcon, FileIcon, SendIcon} from 'lucide-react';
import {ReactNode, useState} from 'react';
import {twMerge} from 'tailwind-merge';

export interface VersionHistoryItemI {
    description?: string | null;
    published: boolean;
    publishedDate?: Date;
    version: number;
}

interface VersionHistoryTimelineProps {
    ariaLabel: string;
    items: VersionHistoryItemI[];
    onSelectVersion?: (version: number) => void;
    renderActions?: (item: VersionHistoryItemI) => ReactNode;
    renderChanges: (item: VersionHistoryItemI) => ReactNode;
    selectedVersion?: number;
}

const formatVersionDate = (date: Date) =>
    date.toLocaleString(undefined, {day: 'numeric', hour: '2-digit', minute: '2-digit', month: 'short'});

const VersionHistoryTimeline = ({
    ariaLabel,
    items,
    onSelectVersion,
    renderActions,
    renderChanges,
    selectedVersion,
}: VersionHistoryTimelineProps) => {
    const [expandedVersions, setExpandedVersions] = useState<Set<number>>(new Set());

    const selectable = onSelectVersion !== undefined;

    const handleToggleExpanded = (version: number) => {
        setExpandedVersions((previousVersions) => {
            const nextVersions = new Set(previousVersions);

            if (nextVersions.has(version)) {
                nextVersions.delete(version);
            } else {
                nextVersions.add(version);
            }

            return nextVersions;
        });
    };

    return (
        <ol
            aria-label={ariaLabel}
            className="flex min-h-0 flex-1 flex-col gap-y-3 overflow-y-auto p-3"
            role={selectable ? 'listbox' : undefined}
        >
            {items.map((item, index) => {
                const {published, version} = item;

                const expanded = expandedVersions.has(version);
                const lastItem = index === items.length - 1;
                const selected = selectedVersion === version;
                const versionSelectable = selectable && published;

                const summary = (
                    <>
                        <div className="flex items-center gap-x-2">
                            {published ? (
                                <Badge
                                    className="border-transparent"
                                    icon={<SendIcon />}
                                    label="Published"
                                    styleType="success-outline"
                                    weight="semibold"
                                />
                            ) : (
                                <Badge icon={<FileIcon />} label="Draft" styleType="warning-filled" weight="semibold" />
                            )}

                            {item.publishedDate && (
                                <span className="text-sm text-muted-foreground">
                                    {formatVersionDate(item.publishedDate)}
                                </span>
                            )}
                        </div>

                        <p className="text-sm whitespace-pre-line">
                            {item.description || (published ? 'No description.' : 'Current working version.')}
                        </p>
                    </>
                );

                return (
                    <li className="flex gap-x-3" key={version} role={selectable ? 'none' : undefined}>
                        <div className="flex w-10 shrink-0 flex-col items-center">
                            <span
                                className={twMerge(
                                    'mt-2 rounded-md px-2 py-0.5 text-sm font-semibold',
                                    selected && 'bg-surface-brand-primary text-white'
                                )}
                            >
                                {`V${version}`}
                            </span>

                            {!lastItem && <div className="mt-2 w-px flex-1 bg-border" />}
                        </div>

                        <div
                            className={twMerge(
                                'flex min-w-0 flex-1 flex-col gap-y-2 rounded-md border border-transparent bg-surface-neutral-primary p-3',
                                versionSelectable && 'cursor-pointer hover:border-stroke-brand-secondary-hover',
                                selected && 'border-stroke-brand-primary hover:border-stroke-brand-primary'
                            )}
                        >
                            <div className="flex items-center gap-x-2">
                                {selectable ? (
                                    <div
                                        aria-disabled={!published}
                                        aria-label={`Version ${version}`}
                                        aria-selected={selected}
                                        className="flex min-w-0 flex-1 flex-col gap-y-2 rounded-sm outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50"
                                        onClick={() => published && onSelectVersion(version)}
                                        onKeyDown={(event) => {
                                            if (published && (event.key === 'Enter' || event.key === ' ')) {
                                                event.preventDefault();

                                                onSelectVersion(version);
                                            }
                                        }}
                                        role="option"
                                        tabIndex={published ? 0 : -1}
                                    >
                                        {summary}
                                    </div>
                                ) : (
                                    <div className="flex min-w-0 flex-1 flex-col gap-y-2">{summary}</div>
                                )}

                                <Button
                                    aria-expanded={expanded}
                                    aria-label={`Toggle changes of version ${version}`}
                                    className="size-6"
                                    icon={expanded ? <ChevronDownIcon /> : <ChevronRightIcon />}
                                    onClick={(event) => {
                                        event.stopPropagation();

                                        handleToggleExpanded(version);
                                    }}
                                    onKeyDown={(event) => event.stopPropagation()}
                                    size="icon"
                                    variant="ghost"
                                />

                                {renderActions?.(item)}
                            </div>

                            {expanded && (
                                <div
                                    onClick={(event) => event.stopPropagation()}
                                    onKeyDown={(event) => event.stopPropagation()}
                                    role="presentation"
                                >
                                    {renderChanges(item)}
                                </div>
                            )}
                        </div>
                    </li>
                );
            })}
        </ol>
    );
};

export default VersionHistoryTimeline;
