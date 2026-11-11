import {NextStepI} from '@/components/GettingStarted/GettingStartedNextSteps';
import {
    EMBEDDED_AUTOMATION_HUB_DOCUMENTATION_URL,
    EMBEDDED_CONNECT_DIALOG_DOCUMENTATION_URL,
    EMBEDDED_MCP_SERVERS_DOCUMENTATION_URL,
    EMBEDDED_SIGNING_KEYS_DOCUMENTATION_URL,
    EMBEDDED_WORKFLOWS_DOCUMENTATION_URL,
} from '@/ee/pages/embedded/embeddedDocumentationUrls';

export const EMBEDDED_NEXT_STEPS: NextStepI[] = [
    {
        description: 'Let your users connect their apps from your product with the frontend SDK.',
        external: true,
        href: EMBEDDED_CONNECT_DIALOG_DOCUMENTATION_URL,
        title: 'Connect popup',
    },
    {
        description: 'Sign the user tokens your backend hands to the frontend SDK.',
        external: true,
        href: EMBEDDED_SIGNING_KEYS_DOCUMENTATION_URL,
        title: 'Signing keys',
    },
    {
        description: 'Run multi-step logic for your users with integration workflows.',
        external: true,
        href: EMBEDDED_WORKFLOWS_DOCUMENTATION_URL,
        title: 'Integration workflows',
    },
    {
        description: "Expose your users' tools to AI agents over MCP.",
        external: true,
        href: EMBEDDED_MCP_SERVERS_DOCUMENTATION_URL,
        title: 'MCP servers',
    },
    {
        description: 'Let your users build their own automations inside your product.',
        external: true,
        href: EMBEDDED_AUTOMATION_HUB_DOCUMENTATION_URL,
        title: 'Automation Hub',
    },
    {
        description: 'See every call your backend made, including the one you just made.',
        external: false,
        href: '/embedded/external-invocations',
        title: 'External Invocations',
    },
];
