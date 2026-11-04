export interface WebhookPresetI {
    headers: Array<{name: string; value: string}>;
    id: 'CUSTOM' | 'DATADOG' | 'NEW_RELIC';
    label: string;
    template: string;
    url: string;
}

export interface WebhookPresetSettingsI {
    webhook?: string;
    webhookBodyTemplate?: string;
    webhookHeaders?: Array<{name: string; value: string}>;
}

const CUSTOM_TEMPLATE = `{
  "eventType": "{{eventType}}",
  "jobId": "{{jobId}}",
  "jobName": "{{jobName}}",
  "jobStatus": "{{job.status}}",
  "workflowName": "{{workflow.name}}",
  "projectName": "{{project.name}}",
  "environment": "{{environment}}",
  "errorMessage": "{{error.message}}",
  "executionUrl": "{{executionUrl}}",
  "deliveryId": "{{deliveryId}}",
  "timestamp": "{{timestamp}}"
}`;

const DATADOG_HOST_PATTERN = /(^|\.)datadoghq\.(com|eu)$/;

const NEW_RELIC_HOST_PATTERN = /(^|\.)(newrelic\.com|nr-data\.net)$/;

const DATADOG_TEMPLATE = `{
  "data": {
    "type": "event",
    "attributes": {
      "title": "{{workflow.name}}: {{eventType}}",
      "category": "alert",
      "integration_id": "custom-events",
      "message": "Job {{jobName}} ({{jobId}}) in {{environment}}. {{error.message}} {{executionUrl}}",
      "aggregation_key": "bytechef-job-{{jobId}}",
      "tags": ["source:bytechef", "environment:{{environment}}", "event:{{eventType}}"],
      "attributes": {
        "status": "error",
        "custom": {{summary}}
      }
    }
  }
}`;

const NEW_RELIC_TEMPLATE = `[
  {
    "eventType": "ByteChefNotification",
    "notificationEvent": "{{eventType}}",
    "jobId": "{{jobId}}",
    "jobName": "{{jobName}}",
    "jobStatus": "{{job.status}}",
    "workflowName": "{{workflow.name}}",
    "projectName": "{{project.name}}",
    "environment": "{{environment}}",
    "source": "{{source}}",
    "errorTaskName": "{{error.taskName}}",
    "errorMessage": "{{error.message}}",
    "executionUrl": "{{executionUrl}}"
  }
]`;

export const WEBHOOK_PRESETS: WebhookPresetI[] = [
    {headers: [], id: 'CUSTOM', label: 'Custom', template: CUSTOM_TEMPLATE, url: ''},
    {
        headers: [
            {name: 'DD-API-KEY', value: ''},
            {name: 'DD-APPLICATION-KEY', value: ''},
        ],
        id: 'DATADOG',
        label: 'Datadog',
        template: DATADOG_TEMPLATE,
        url: 'https://event-management-intake.datadoghq.com/api/v2/events',
    },
    {
        headers: [{name: 'Api-Key', value: ''}],
        id: 'NEW_RELIC',
        label: 'New Relic',
        template: NEW_RELIC_TEMPLATE,
        url: 'https://insights-collector.newrelic.com/v1/accounts/YOUR_ACCOUNT_ID/events',
    },
];

export function findWebhookPresetForUrl(url: string | undefined): WebhookPresetI {
    const customPreset = WEBHOOK_PRESETS[0];

    let hostname: string;

    try {
        hostname = new URL((url || '').trim()).hostname.toLowerCase();
    } catch {
        return customPreset;
    }

    const presetId = DATADOG_HOST_PATTERN.test(hostname)
        ? 'DATADOG'
        : NEW_RELIC_HOST_PATTERN.test(hostname)
          ? 'NEW_RELIC'
          : 'CUSTOM';

    return WEBHOOK_PRESETS.find((preset) => preset.id === presetId) || customPreset;
}

export function isWebhookPresetUnchanged(
    settings: WebhookPresetSettingsI | undefined,
    preset: WebhookPresetI
): boolean {
    const headers = settings?.webhookHeaders || [];

    return (
        (settings?.webhook || '') === preset.url &&
        (settings?.webhookBodyTemplate || '') === preset.template &&
        headers.length === preset.headers.length &&
        headers.every((header, index) => header.name === preset.headers[index].name && !header.value)
    );
}

export function isWebhookSettingsEmpty(settings: WebhookPresetSettingsI | undefined): boolean {
    return !settings?.webhook && !settings?.webhookBodyTemplate && !settings?.webhookHeaders?.length;
}
