import {Link} from 'react-router-dom';

export interface NextStepI {
    description: string;
    external: boolean;
    href: string;
    title: string;
}

interface GettingStartedNextStepsProps {
    steps: NextStepI[];
}

const linkClassName = 'text-sm font-medium text-content-brand-primary hover:underline';

const GettingStartedNextSteps = ({steps}: GettingStartedNextStepsProps) => (
    <section aria-labelledby="getting-started-next-steps" className="space-y-3">
        <h2 className="text-base leading-none font-semibold" id="getting-started-next-steps">
            Next steps
        </h2>

        <ul className="grid gap-3 sm:grid-cols-2">
            {steps.map((nextStep) => (
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

export default GettingStartedNextSteps;
