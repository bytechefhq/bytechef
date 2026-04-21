export const getPageUrl = (type: 'form' | 'chats', environmentId?: number, staticWebhookUrl?: string) => {
    if (!staticWebhookUrl) {
        return '';
    }

    const workflowExecutionId = staticWebhookUrl.substring(
        staticWebhookUrl.lastIndexOf('/webhooks/') + '/webhooks/'.length
    );

    return `${type === 'chats' ? '/automation' : ''}/${type}${type === 'form' ? `/${environmentId}` : ''}/${workflowExecutionId}`;
};
