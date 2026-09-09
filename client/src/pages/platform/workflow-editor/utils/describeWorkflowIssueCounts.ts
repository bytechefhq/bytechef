export default function describeWorkflowIssueCounts(errorCount: number, warningCount: number): string {
    const parts: Array<string> = [];

    if (errorCount > 0) {
        parts.push(errorCount === 1 ? '1 error' : `${errorCount} errors`);
    }

    if (warningCount > 0) {
        parts.push(warningCount === 1 ? '1 warning' : `${warningCount} warnings`);
    }

    return parts.join(', ');
}
