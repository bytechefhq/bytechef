const INPUT_REQUIRED_PREFIX = 'INPUT_REQUIRED:';
const INVALID_WORKFLOW_PREFIX = 'INVALID_WORKFLOW:';
const MISSING_CONNECTION_PREFIX = 'MISSING_CONNECTION:';

export const describeWorkflowErrors = (errorCount: number | undefined): string => {
    if (errorCount === 1) {
        return 'the error';
    }

    return errorCount && errorCount > 1 ? `the ${errorCount} errors` : 'the errors';
};

export const isInvalidWorkflowAttentionReason = (attentionReason: string | undefined): boolean =>
    !!attentionReason?.startsWith(INVALID_WORKFLOW_PREFIX);

export const describeAttentionReason = (attentionReason: string | undefined): string | undefined => {
    if (!attentionReason) {
        return undefined;
    }

    if (attentionReason.startsWith(MISSING_CONNECTION_PREFIX)) {
        const componentName = attentionReason.slice(MISSING_CONNECTION_PREFIX.length);

        return `Connect ${componentName || 'the missing connection'} to keep this running`;
    }

    if (attentionReason.startsWith(INPUT_REQUIRED_PREFIX)) {
        const inputName = attentionReason.slice(INPUT_REQUIRED_PREFIX.length);

        return `Fill in ${inputName || 'the required input'} to keep this running`;
    }

    if (attentionReason.startsWith(INVALID_WORKFLOW_PREFIX)) {
        const errorCount = Number.parseInt(attentionReason.slice(INVALID_WORKFLOW_PREFIX.length), 10);

        return `Fix ${describeWorkflowErrors(Number.isNaN(errorCount) ? undefined : errorCount)} in this workflow to turn it on`;
    }

    return 'Turn it on again to apply the latest update';
};
