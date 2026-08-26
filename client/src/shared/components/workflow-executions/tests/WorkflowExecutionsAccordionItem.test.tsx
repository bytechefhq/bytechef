import {Accordion} from '@/components/ui/accordion';
import {TaskExecution} from '@/shared/middleware/automation/workflow/execution';
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import WorkflowExecutionsAccordionItem from '../WorkflowExecutionsAccordionItem';

let nextId = 1;

function createNodeTaskExecution(nodeName: string): TaskExecution {
    return {
        id: String(nextId++),
        jobId: '1',
        priority: 1,
        startDate: new Date('2024-01-01T10:00:00'),
        status: 'COMPLETED',
        // Every graph node task is dispatched as the single task of its visit, so the server always
        // stamps it 1 (`GraphTaskUtils#dispatchNodeTask`).
        taskNumber: 1,
        title: `${nodeName} task`,
        type: 'test/v1/testAction',
        workflowTask: {
            name: nodeName,
            parameters: {__node: nodeName},
            type: 'test/v1/testAction',
        },
    } as TaskExecution;
}

function createGraphTaskExecution(children: TaskExecution[]): TaskExecution {
    return {
        children,
        id: 'graph-1',
        jobId: '1',
        priority: 1,
        startDate: new Date('2024-01-01T10:00:00'),
        status: 'COMPLETED',
        title: 'Graph',
        type: 'graph/v1',
        workflowTask: {
            name: 'graph_1',
            type: 'graph/v1',
        },
    } as TaskExecution;
}

function renderGraphExecution(execution: TaskExecution) {
    return render(
        <Accordion defaultValue={[execution.id as string]} type="multiple">
            <WorkflowExecutionsAccordionItem
                defaultValue={[execution.id as string]}
                execution={execution}
                onExecutionClick={() => {}}
                selectedExecutionId=""
            >
                <span>Graph</span>
            </WorkflowExecutionsAccordionItem>
        </Accordion>
    );
}

describe('WorkflowExecutionsAccordionItem graph visits', () => {
    it('should render one row per child task execution, numbering repeat visits of a node', () => {
        renderGraphExecution(
            createGraphTaskExecution([
                createNodeTaskExecution('node_a'),
                createNodeTaskExecution('node_b'),
                createNodeTaskExecution('node_a'),
            ])
        );

        expect(screen.getByText('node_a (visit 1)')).toBeInTheDocument();
        expect(screen.getByText('node_b (visit 1)')).toBeInTheDocument();
        expect(screen.getByText('node_a (visit 2)')).toBeInTheDocument();
    });

    it('should summarize three child task executions as three node visits', () => {
        renderGraphExecution(
            createGraphTaskExecution([
                createNodeTaskExecution('node_a'),
                createNodeTaskExecution('node_b'),
                createNodeTaskExecution('node_c'),
            ])
        );

        expect(screen.getByText('3 node visits')).toBeInTheDocument();
    });

    it('should summarize a single child task execution as one node visit', () => {
        renderGraphExecution(createGraphTaskExecution([createNodeTaskExecution('node_a')]));

        expect(screen.getByText('1 node visit')).toBeInTheDocument();
    });

    it('should no longer nest a per-task list under each visit', () => {
        renderGraphExecution(
            createGraphTaskExecution([createNodeTaskExecution('node_a'), createNodeTaskExecution('node_b')])
        );

        expect(screen.queryByText('task')).not.toBeInTheDocument();
        expect(screen.queryByText('tasks')).not.toBeInTheDocument();
    });

    it('should render the flat children list for a non-graph task execution', () => {
        const nonGraphTaskExecution = {
            children: [createNodeTaskExecution('node_a'), createNodeTaskExecution('node_b')],
            id: 'loopish-1',
            jobId: '1',
            priority: 1,
            startDate: new Date('2024-01-01T10:00:00'),
            status: 'COMPLETED',
            title: 'Branch',
            type: 'branch/v1',
            workflowTask: {
                name: 'branch_1',
                type: 'branch/v1',
            },
        } as TaskExecution;

        renderGraphExecution(nonGraphTaskExecution);

        expect(screen.queryByText(/visit 1/)).not.toBeInTheDocument();
        expect(screen.queryByText(/node visits?/)).not.toBeInTheDocument();
    });
});
