import {useLocation, useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

interface ExecutionsTabsProps {
    basePath: string;
}

const ExecutionsTabs = ({basePath}: ExecutionsTabsProps) => {
    const navigate = useNavigate();

    const location = useLocation();

    const executionsTabs = [
        {label: 'Workflow Executions', path: basePath},
        {label: 'Tool Invocations', path: `${basePath}/tool-invocations`},
    ];

    return (
        <div className="flex items-center gap-1">
            {executionsTabs.map((executionsTab) => {
                const active = location.pathname === executionsTab.path;

                return (
                    <button
                        className={twMerge(
                            'rounded-md px-3 py-1.5 text-sm font-medium',
                            active
                                ? 'bg-surface-neutral-secondary text-content-neutral-primary'
                                : 'text-content-neutral-secondary hover:text-content-neutral-primary'
                        )}
                        key={executionsTab.path}
                        onClick={() => navigate(executionsTab.path)}
                        type="button"
                    >
                        {executionsTab.label}
                    </button>
                );
            })}
        </div>
    );
};

export default ExecutionsTabs;
