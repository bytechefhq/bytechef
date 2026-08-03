import {Accordion} from '@/components/ui/accordion';
import {TaskExecution} from '@/shared/middleware/automation/workflow/execution';
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import WorkflowExecutionsAccordionItem from '../WorkflowExecutionsAccordionItem';

let nextId = 1;

function createNodeTaskExecution(nodeName: string, taskNumber: number): TaskExecution {
    return {
        id: String(nextId++),
        jobId: '1',
        priority: 1,
        startDate: new Date('2024-01-01T10:00:00'),
        status: 'COMPLETED',
        taskNumber,
        title: `${nodeName} task`,
        type: 'test/v1/testAction',
        workflowTask: {
            name: `${nodeName}_task_${taskNumber}`,
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

describe('WorkflowExecutionsAccordionItem graph grouping', () => {
    it('should group a graph task execution children into per-node visit sections', () => {
        const children = [
            createNodeTaskExecution('node_a', 1),
            createNodeTaskExecution('node_a', 2),
            createNodeTaskExecution('node_b', 1),
            createNodeTaskExecution('node_a', 1),
        ];
        const graphTaskExecution = createGraphTaskExecution(children);

        render(
            <Accordion defaultValue={['graph-1']} type="multiple">
                <WorkflowExecutionsAccordionItem
                    defaultValue={['graph-1']}
                    execution={graphTaskExecution}
                    onExecutionClick={() => {}}
                    selectedExecutionId=""
                >
                    <span>Graph</span>
                </WorkflowExecutionsAccordionItem>
            </Accordion>
        );

        expect(screen.getByText('node_a (visit 1)')).toBeInTheDocument();
        expect(screen.getByText('node_b (visit 1)')).toBeInTheDocument();
        expect(screen.getByText('node_a (visit 2)')).toBeInTheDocument();

        expect(screen.queryByText(/transitions/)).not.toBeInTheDocument();
        expect(screen.getByText('3 node visits')).toBeInTheDocument();
    });

    it('should render the flat children list for a non-graph task execution', () => {
        const children = [createNodeTaskExecution('node_a', 1), createNodeTaskExecution('node_b', 1)];
        const nonGraphTaskExecution = {
            children,
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

        render(
            <Accordion defaultValue={['loopish-1']} type="multiple">
                <WorkflowExecutionsAccordionItem
                    defaultValue={['loopish-1']}
                    execution={nonGraphTaskExecution}
                    onExecutionClick={() => {}}
                    selectedExecutionId=""
                >
                    <span>Branch</span>
                </WorkflowExecutionsAccordionItem>
            </Accordion>
        );

        expect(screen.queryByText(/visit 1/)).not.toBeInTheDocument();
        expect(screen.queryByText(/transitions/)).not.toBeInTheDocument();
    });
});
