import {describe, expect, test, vi} from 'vitest';

import {enqueueWorkflowMutation} from './workflowMutationQueue';

describe('workflowMutationQueue', () => {
    test('executes a single mutation', async () => {
        const executionOrder: number[] = [];

        enqueueWorkflowMutation(() => {
            executionOrder.push(1);

            return Promise.resolve();
        });

        await vi.waitFor(() => {
            expect(executionOrder).toEqual([1]);
        });
    });

    test('serializes concurrent mutations', async () => {
        const executionOrder: string[] = [];

        enqueueWorkflowMutation(
            () =>
                new Promise<void>((resolve) => {
                    setTimeout(() => {
                        executionOrder.push('first-start');
                        executionOrder.push('first-end');
                        resolve();
                    }, 50);
                })
        );

        enqueueWorkflowMutation(
            () =>
                new Promise<void>((resolve) => {
                    executionOrder.push('second-start');
                    executionOrder.push('second-end');
                    resolve();
                })
        );

        await vi.waitFor(
            () => {
                expect(executionOrder).toEqual(['first-start', 'first-end', 'second-start', 'second-end']);
            },
            {timeout: 500}
        );
    });

    test('continues executing after a failed mutation', async () => {
        const executionOrder: string[] = [];

        enqueueWorkflowMutation(() => {
            executionOrder.push('failing');

            return Promise.reject(new Error('mutation failed'));
        });

        enqueueWorkflowMutation(() => {
            executionOrder.push('succeeding');

            return Promise.resolve();
        });

        await vi.waitFor(
            () => {
                expect(executionOrder).toEqual(['failing', 'succeeding']);
            },
            {timeout: 200}
        );
    });

    test('serializes three mutations in order', async () => {
        const executionOrder: number[] = [];

        for (let index = 1; index <= 3; index++) {
            const capturedIndex = index;

            enqueueWorkflowMutation(
                () =>
                    new Promise<void>((resolve) => {
                        setTimeout(() => {
                            executionOrder.push(capturedIndex);
                            resolve();
                        }, 10);
                    })
            );
        }

        await vi.waitFor(
            () => {
                expect(executionOrder).toEqual([1, 2, 3]);
            },
            {timeout: 500}
        );
    });
});
