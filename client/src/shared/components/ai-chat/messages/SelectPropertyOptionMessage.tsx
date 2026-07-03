import ComboBox from '@/components/ComboBox/ComboBox';
import {DataMessagePartProps, useThreadRuntime} from '@assistant-ui/react';
import {CheckIcon} from 'lucide-react';
import {useEffect, useState} from 'react';

export interface SelectPropertyOptionItemI {
    label: string;
    value: string;
}

export interface SelectPropertyOptionDataI {
    componentName: string;
    kind: 'select-property-option';
    options: SelectPropertyOptionItemI[];
    propertyName: string;
    truncated?: boolean;
}

/**
 * Renders the selectPropertyOption / selectTriggerPropertyOption tool result as a searchable picker of all
 * options the tool fetched from the connection (taken straight from the tool result, not re-emitted by the
 * LLM). On pick, the option's real value is submitted as a system message "User picked: <label> (value:
 * <value>)" so the agent writes the value (e.g. a channel id) into the workflow.
 */
const SelectPropertyOptionMessage = ({data}: DataMessagePartProps<SelectPropertyOptionDataI>) => {
    const [picked, setPicked] = useState<SelectPropertyOptionItemI | undefined>();
    const [superseded, setSuperseded] = useState(false);

    const threadRuntime = useThreadRuntime();

    useEffect(() => {
        const initialCount = threadRuntime.getState().messages.length;

        return threadRuntime.subscribe(() => {
            if (threadRuntime.getState().messages.length > initialCount) {
                setSuperseded(true);
            }
        });
    }, [threadRuntime]);

    if (picked) {
        return (
            <div className="mt-2 flex items-center gap-2 text-sm">
                <CheckIcon className="size-4 text-emerald-600" />

                <span>
                    Picked: <span className="font-medium">{picked.label}</span>
                </span>
            </div>
        );
    }

    const options: SelectPropertyOptionItemI[] = data.options ?? [];

    if (options.length === 0) {
        return (
            <div className="mt-2 rounded-md border border-border bg-muted/30 p-3 text-sm text-muted-foreground">
                No options available for {data.propertyName}.
            </div>
        );
    }

    const items = options.map((option) => ({label: option.label, value: option.label}));

    return (
        <div className={`mt-2 flex w-full min-w-0 flex-col gap-1${superseded ? 'opacity-60' : ''}`}>
            <ComboBox
                emptyMessage="No match"
                items={items}
                onChange={(item) => {
                    if (!item) {
                        return;
                    }

                    const option = options.find((candidate) => candidate.label === item.value);

                    if (!option) {
                        return;
                    }

                    setPicked(option);

                    threadRuntime.append({
                        content: [{text: `User picked: ${option.label} (value: ${option.value})`, type: 'text'}],
                        role: 'system',
                    });
                }}
                value={undefined}
            />

            {data.truncated && (
                <span className="text-xs text-muted-foreground">
                    Showing the first {options.length}. Narrow with a search term if you don&apos;t see yours.
                </span>
            )}
        </div>
    );
};

export default SelectPropertyOptionMessage;
