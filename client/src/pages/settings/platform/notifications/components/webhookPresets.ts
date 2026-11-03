export interface WebhookPresetI {
    headers: Array<{name: string; value: string}>;
    id: 'CUSTOM' | 'DATADOG' | 'NEW_RELIC';
    label: string;
    template: string;
    url: string;
}

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
    {headers: [], id: 'CUSTOM', label: 'Custom', template: '', url: ''},
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
