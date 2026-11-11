const SUPPORT_EMAIL = 'support@bytechef.io';

interface GettingStartedHelpCardProps {
    title: string;
}

const GettingStartedHelpCard = ({title}: GettingStartedHelpCardProps) => (
    <section className="flex items-center justify-between gap-4 rounded-md border border-stroke-neutral-secondary p-4">
        <p className="text-sm font-medium">{title}</p>

        <a className="text-sm font-medium text-content-brand-primary hover:underline" href={`mailto:${SUPPORT_EMAIL}`}>
            Contact support
        </a>
    </section>
);

export default GettingStartedHelpCard;
