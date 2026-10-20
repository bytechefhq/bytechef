interface JobTimingI {
    endDate?: Date;
    startDate?: Date;
}

export function getJobDuration(job?: JobTimingI): string | undefined {
    const startDate = job?.startDate?.getTime();
    const endDate = job?.endDate?.getTime();

    if (startDate && endDate) {
        return `${Math.round(endDate - startDate)}ms`;
    }
}

export function getJobExecutionDate(job?: JobTimingI): string | undefined {
    const startDate = job?.startDate;

    if (startDate) {
        return `${startDate.toLocaleDateString()} ${startDate.toLocaleTimeString()}`;
    }
}
