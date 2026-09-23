const INPUT_REQUIRED_PREFIX = 'INPUT_REQUIRED:';
const MISSING_CONNECTION_PREFIX = 'MISSING_CONNECTION:';

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

    return 'Turn it on again to apply the latest update';
};
