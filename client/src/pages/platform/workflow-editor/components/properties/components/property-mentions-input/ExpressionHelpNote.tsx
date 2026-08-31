import {ExternalLinkIcon} from 'lucide-react';

export const EXPRESSIONS_DOCUMENTATION_URL = 'https://docs.bytechef.io/reference/expressions';

const ExpressionHelpNote = () => (
    <p className="mt-1 flex flex-wrap items-start gap-x-1 text-xs text-content-neutral-secondary">
        <span>This field supports functions to build a value.</span>

        <a
            className="underline underline-offset-2 hover:text-content-neutral-primary"
            href={EXPRESSIONS_DOCUMENTATION_URL}
            rel="noreferrer"
            target="_blank"
        >
            Learn more <ExternalLinkIcon className="inline size-3" />
        </a>
    </p>
);

export default ExpressionHelpNote;
