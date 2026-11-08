import ComboBox from '@/components/ComboBox/ComboBox';
import {Label} from '@/components/ui/label';
import {useState} from 'react';

import {ConnectionPrefillOptionI} from './types';

interface ConnectionPrefillPickerProps {
    onSelect: (option: ConnectionPrefillOptionI) => void;
    options: ConnectionPrefillOptionI[];
}

const ConnectionPrefillPicker = ({onSelect, options}: ConnectionPrefillPickerProps) => {
    const [selectedValue, setSelectedValue] = useState<string>();

    if (!options.length) {
        return null;
    }

    const selectedOption = options.find((option) => option.value === selectedValue);

    return (
        <div className="space-y-2">
            <Label>Choose a known server</Label>

            <ComboBox
                aria-label="Choose a known server"
                emptyMessage="No server found."
                items={options.map((option) => ({label: option.label, value: option.value}))}
                name="connection-prefill-picker"
                onChange={(item) => {
                    const option = options.find((option) => option.value === item?.value);

                    if (option) {
                        setSelectedValue(option.value);

                        onSelect(option);
                    }
                }}
                value={selectedValue}
            />

            {selectedOption && (
                <div className="space-y-1 text-sm text-muted-foreground">
                    {selectedOption.description && <p>{selectedOption.description}</p>}

                    {selectedOption.notes.map((note) => (
                        <p key={note}>{note}</p>
                    ))}

                    {(selectedOption.websiteUrl || selectedOption.componentDocumentationUrl) && (
                        <div className="flex flex-wrap gap-x-3">
                            {selectedOption.websiteUrl && (
                                <a
                                    className="text-content-brand-primary"
                                    href={selectedOption.websiteUrl}
                                    rel="noopener noreferrer"
                                    target="_blank"
                                >
                                    Server documentation
                                </a>
                            )}

                            {selectedOption.componentDocumentationUrl && (
                                <a
                                    className="text-content-brand-primary"
                                    href={selectedOption.componentDocumentationUrl}
                                    rel="noopener noreferrer"
                                    target="_blank"
                                >
                                    ByteChef also has a {selectedOption.label} component
                                </a>
                            )}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default ConnectionPrefillPicker;
