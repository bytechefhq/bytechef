import {ReactNode} from 'react';

interface AgentSectionProps {
    action?: ReactNode;
    children: ReactNode;
    title: string;
}

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
