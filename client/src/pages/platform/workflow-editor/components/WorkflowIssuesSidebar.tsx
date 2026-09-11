import {NodeDataType} from '@/shared/types';
import {AlertTriangleIcon} from 'lucide-react';
import {useCallback, useMemo} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowIssues from '../hooks/useWorkflowIssues';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {WorkflowIssueI, getWorkflowIssueKey} from '../stores/useWorkflowIssuesStore';
import describeWorkflowIssueCounts from '../utils/describeWorkflowIssueCounts';
import findClusterElementRootTaskName from '../utils/findClusterElementRootTaskName';
import openNodeDetails from '../utils/openNodeDetails';

interface WorkflowIssuesSidebarProps {
    visible: boolean;
}

const WorkflowIssuesSidebar = ({visible}: WorkflowIssuesSidebarProps) => {
    const {nodes, workflow} = useWorkflowDataStore(
        useShallow((state) => ({nodes: state.nodes, workflow: state.workflow}))
    );

    const issues = useWorkflowIssues();

    const issuesByNode = useMemo(() => {
        const grouped = new Map<string, Array<WorkflowIssueI>>();

        for (const issue of issues) {
            grouped.set(issue.nodeName, [...(grouped.get(issue.nodeName) ?? []), issue]);
        }

        return [...grouped.entries()];
    }, [issues]);

    const heading = useMemo(() => {
        if (issues.length === 0) {
            return 'Workflow Issues';
        }

        const errorCount = issues.filter((issue) => issue.severity === 'ERROR').length;
        const warningCount = issues.filter((issue) => issue.severity !== 'ERROR').length;

        return `Workflow Issues (${describeWorkflowIssueCounts(errorCount, warningCount)})`;
    }, [issues]);

    const handleIssueClick = useCallback(
        (nodeName: string) => {
            const targetNodeName = findClusterElementRootTaskName(workflow.tasks, nodeName) ?? nodeName;

            const node = nodes.find((currentNode) => (currentNode.data as NodeDataType).name === targetNodeName);

            if (node) {
                openNodeDetails(node.data as NodeDataType, 'properties');
            }
        },
        [nodes, workflow.tasks]
    );

    return (
        <aside
            aria-label="Workflow issues"
            className={twMerge(
                'absolute inset-y-2 right-14 flex w-96 flex-col overflow-hidden rounded-md border border-stroke-neutral-secondary bg-surface-neutral-secondary transition-[transform,opacity] duration-300 ease-in-out',
                visible ? 'translate-x-0 opacity-100' : 'translate-x-4 opacity-0'
            )}
        >
            <h2 className="px-3 py-3 text-sm font-semibold text-content-neutral-primary">{heading}</h2>

            <div className="flex flex-1 flex-col gap-3 overflow-y-auto p-3">
                {issuesByNode.length === 0 && <p className="text-sm text-content-neutral-secondary">No issues found</p>}

                {issuesByNode.map(([nodeName, nodeIssues]) => (
                    <section className="flex flex-col gap-1" key={nodeName}>
                        <h3 className="text-sm font-semibold text-content-neutral-secondary">{nodeName}</h3>

                        {nodeIssues.map((issue) => (
                            <button
                                className="flex items-start gap-2 rounded-md border-2 border-transparent bg-white px-2 py-1.5 text-left text-sm hover:border-blue-200"
                                key={getWorkflowIssueKey(issue)}
                                onClick={() => handleIssueClick(nodeName)}
                                type="button"
                            >
                                <AlertTriangleIcon
                                    className={twMerge(
                                        'mt-0.5 size-3.5 shrink-0',
                                        issue.severity === 'ERROR'
                                            ? 'text-content-destructive'
                                            : 'text-content-onwarning'
                                    )}
                                />

                                <span className="flex flex-col">
                                    <span className="text-content-neutral-primary">{issue.message}</span>

                                    {issue.propertyPath && !issue.message.includes(issue.propertyPath) && (
                                        <span className="text-content-neutral-secondary">{issue.propertyPath}</span>
                                    )}
                                </span>
                            </button>
                        ))}
                    </section>
                ))}
            </div>
        </aside>
    );
};

export default WorkflowIssuesSidebar;
