import {Option} from '@/shared/middleware/platform/configuration';

interface WorkflowNodeOptionsSelectPropsI {
    isLoading: boolean;
    onChange: (value: string) => void;
    options?: Option[];
    value?: string;
}

const WorkflowNodeOptionsSelect = ({isLoading, onChange, options, value}: WorkflowNodeOptionsSelectPropsI) => (
    <select
        className="flex h-9 w-full rounded-md border border-stroke-neutral-secondary bg-surface-neutral-primary px-3 py-1 text-sm text-content-neutral-primary"
        onChange={(event) => onChange(event.target.value)}
        value={value ?? ''}
    >
        <option disabled value="">
            {isLoading ? 'Loading...' : 'Select...'}
        </option>

        {(options ?? []).map((option) => (
            <option key={String(option.value)} value={String(option.value)}>
                {option.label ?? String(option.value)}
            </option>
        ))}
    </select>
);

export default WorkflowNodeOptionsSelect;
