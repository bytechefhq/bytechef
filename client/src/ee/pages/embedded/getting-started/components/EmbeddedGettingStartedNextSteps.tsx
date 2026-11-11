import {
    EMBEDDED_AUTOMATION_HUB_DOCUMENTATION_URL,
    EMBEDDED_CONNECT_DIALOG_DOCUMENTATION_URL,
    EMBEDDED_MCP_SERVERS_DOCUMENTATION_URL,
    EMBEDDED_SIGNING_KEYS_DOCUMENTATION_URL,
    EMBEDDED_WORKFLOWS_DOCUMENTATION_URL,
} from '@/ee/pages/embedded/embeddedDocumentationUrls';
import {Link} from 'react-router-dom';

interface NextStepI {
    description: string;
    external: boolean;
    href: string;
    title: string;
}

const NEXT_STEPS: NextStepI[] = [
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

const linkClassName = 'text-sm font-medium text-content-brand-primary hover:underline';

const EmbeddedGettingStartedNextSteps = () => (
    <section aria-labelledby="embedded-getting-started-next-steps" className="space-y-3">
        <h2 className="text-base font-semibold" id="embedded-getting-started-next-steps">
            Next steps
        </h2>

        <ul className="grid gap-3 sm:grid-cols-2">
            {NEXT_STEPS.map((nextStep) => (
                <li className="rounded-md border border-stroke-neutral-secondary p-3" key={nextStep.title}>
                    {nextStep.external ? (
                        <a className={linkClassName} href={nextStep.href} rel="noopener noreferrer" target="_blank">
                            {nextStep.title}
                        </a>
                    ) : (
                        <Link className={linkClassName} to={nextStep.href}>
                            {nextStep.title}
                        </Link>
                    )}

                    <p className="text-xs text-content-neutral-secondary">{nextStep.description}</p>
                </li>
            ))}
        </ul>
    </section>
);

export default EmbeddedGettingStartedNextSteps;
