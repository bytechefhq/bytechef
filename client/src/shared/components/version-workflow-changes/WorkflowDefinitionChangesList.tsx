import Badge, {BadgeStyleType} from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {WorkflowChangeType, WorkflowDefinitionChange, WorkflowNodeChangeKind} from '@/shared/middleware/graphql';
import {useState} from 'react';

const KIND_BADGES: Record<WorkflowNodeChangeKind, {label: string; styleType: BadgeStyleType}> = {
    [WorkflowNodeChangeKind.Added]: {label: 'Added', styleType: 'success-outline'},
    [WorkflowNodeChangeKind.Modified]: {label: 'Modified', styleType: 'warning-outline'},
    [WorkflowNodeChangeKind.Moved]: {label: 'Moved', styleType: 'primary-outline'},
    [WorkflowNodeChangeKind.Removed]: {label: 'Removed', styleType: 'destructive-outline'},
};

const VALUE_PREVIEW_LENGTH = 160;

interface ValueChangeTextProps {
    text: string;
}

const ValueChangeText = ({text}: ValueChangeTextProps) => {
    const [expanded, setExpanded] = useState(false);

    if (text.length <= VALUE_PREVIEW_LENGTH) {
        return <>{text}</>;
    }

    return (
        <>
            {expanded ? text : `${text.slice(0, VALUE_PREVIEW_LENGTH)}…`}

            <Button
                className="ml-1 h-auto p-0 font-sans text-xs"
                label={expanded ? 'Show less' : 'Show more'}
                onClick={() => setExpanded(!expanded)}
                variant="link"
            />
        </>
    );
};

interface WorkflowDefinitionChangesListProps {
    changeType?: WorkflowChangeType;
    changes: WorkflowDefinitionChange[];
}

const WorkflowDefinitionChangesList = ({changeType, changes}: WorkflowDefinitionChangesListProps) => {
    if (changeType === WorkflowChangeType.Added) {
        return <p className="text-sm text-content-neutral-secondary">Workflow added in this version.</p>;
    }

    if (changeType === WorkflowChangeType.Removed) {
        return <p className="text-sm text-content-neutral-secondary">Workflow removed in this version.</p>;
    }

    if (changes.length === 0) {
        return <p className="text-sm text-content-neutral-secondary">No structural changes.</p>;
    }

    return (
        <div className="flex flex-col gap-4">
            {changes.map((change, index) => {
                const kindBadge = KIND_BADGES[change.kind];

                return (
                    <section
                        className="flex flex-col gap-2"
                        key={`${change.kind}-${change.target}-${change.parentPath ?? ''}-${change.name ?? ''}-${index}`}
                    >
                        <div className="flex items-center gap-2 text-sm">
                            <Badge label={kindBadge.label} styleType={kindBadge.styleType} />

                            <span className="font-medium">{change.label || change.name || 'Workflow'}</span>

                            {change.type && (
                                <span className="text-xs text-content-neutral-secondary">{change.type}</span>
                            )}

                            {change.parentPath && (
                                <span className="text-xs text-content-neutral-secondary">{`in ${change.parentPath}`}</span>
                            )}
                        </div>

                        {change.valueChanges.length > 0 && (
                            <dl className="grid grid-cols-[minmax(0,1fr)_minmax(0,2fr)] gap-x-4 gap-y-1 pl-2 text-xs">
                                {change.valueChanges.map((valueChange) => (
                                    <div className="contents" key={valueChange.path}>
                                        <dt className="truncate font-mono text-content-neutral-secondary">
                                            {valueChange.path}
                                        </dt>

                                        <dd className="font-mono break-all">
                                            <ValueChangeText
                                                text={`${valueChange.oldValue ?? '—'} → ${valueChange.newValue ?? '—'}`}
                                            />
                                        </dd>
                                    </div>
                                ))}
                            </dl>
                        )}
                    </section>
                );
            })}
        </div>
    );
};

export default WorkflowDefinitionChangesList;
