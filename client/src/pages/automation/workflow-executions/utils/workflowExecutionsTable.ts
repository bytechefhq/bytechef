import {Job} from '@/shared/middleware/automation/workflow/execution';

export const MAX_SUBFLOW_DEPTH = 10;

export const formatDateTime = (date: Date) => `${date.toLocaleDateString()} ${date.toLocaleTimeString()}`;

export const getSubflowChildJobs = ({job, seenJobIds}: {job: Job; seenJobIds: Set<string>}): Job[] => {
    if (!job.taskExecutions) {
        return [];
    }

    const executionSubflows = job.taskExecutions?.map((taskExecution) => taskExecution.childJob);

    return executionSubflows.filter(
        (childJob): childJob is Job => childJob != null && (childJob.id == null || !seenJobIds.has(childJob.id))
    );
};

interface HasExpandedSubflowProps {
    childJobs: Job[];
    depth: number;
    expandedJobIds: Set<string>;
    seenJobIds: Set<string>;
}

export const hasExpandedSubflow = ({childJobs, depth, expandedJobIds, seenJobIds}: HasExpandedSubflowProps) =>
    depth < MAX_SUBFLOW_DEPTH &&
    childJobs.some((childJob) => {
        const jobId = childJob.id;

        if (jobId == null || !expandedJobIds.has(jobId)) {
            return false;
        }

        return getSubflowChildJobs({job: childJob, seenJobIds: new Set(seenJobIds).add(jobId)}).length > 0;
    });
