export const getPageUrl = (type: 'form' | 'chat', environmentId?: number, staticWebhookUrl?: string) => {
    if (!staticWebhookUrl) {
        return '';
    }

    const workflowExecutionId = staticWebhookUrl.substring(
        staticWebhookUrl.lastIndexOf('/webhooks/') + '/webhooks/'.length
    );

    return `${type === 'chat' ? '/automation' : ''}/${type}${type === 'form' ? `/${environmentId}` : ''}/${workflowExecutionId}`;
};
