import {ReactNode} from 'react';

interface AgentSectionProps {
    action?: ReactNode;
    children: ReactNode;
    title: string;
}

/**
 * A titled block of the Agent builder. Replaces the bordered {@code Card} the sections used to render in:
 * the detail page stacks ten of these, and ten nested boxes read as chrome rather than structure, so the
 * heading plus vertical rhythm carries the grouping on its own.
 *
 * <p>
 * {@code action} renders right-aligned on the heading row, which is where every section that can gain a row
 * (tools, channels, skills, sub-agents) puts its add control.
 * </p>
 */
const AgentSection = ({action, children, title}: AgentSectionProps) => {
    return (
        <section className="flex flex-col gap-3">
            <div className="flex min-h-8 items-center justify-between gap-2">
                <h2 className="font-semibold">{title}</h2>

                {action}
            </div>

            {children}
        </section>
    );
};

export default AgentSection;
