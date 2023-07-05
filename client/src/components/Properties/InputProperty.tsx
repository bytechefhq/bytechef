import Input from 'components/Input/Input';
import {useDataPillPanelStore} from 'pages/automation/project/stores/useDataPillPanelStore';
import {useNodeDetailsDialogStore} from 'pages/automation/project/stores/useNodeDetailsDialogStore';
import useWorkflowDefinitionStore from 'pages/automation/project/stores/useWorkflowDefinitionStore';
import {ReactNode, useRef} from 'react';
import {twMerge} from 'tailwind-merge';

interface InputPropertyProps {
    controlType?: string;
    description?: string;
    defaultValue?: string;
    error?: boolean;
    fieldsetClassName?: string;
    hidden?: boolean;
    name?: string;
    label?: string;
    leadingIcon?: ReactNode;
    required?: boolean;
    title?: string;
}

const InputProperty = ({
    controlType,
    defaultValue,
    description,
    error,
    hidden,
    label,
    leadingIcon,
    name,
    required,
    title,
}: InputPropertyProps) => {
    const {dataPillPanelOpen, setDataPillPanelOpen} = useDataPillPanelStore();
    const {focusedInput, nodeDetailsDialogOpen, setFocusedInput} =
        useNodeDetailsDialogStore();
    const {dataPills} = useWorkflowDefinitionStore();

    const inputRef = useRef(null);

    const dataPill = dataPills.find((dataPill) => dataPill.name === name);

    const getInputType = () => {
        switch (controlType) {
            case 'DATE':
                return 'date';
            case 'DATE_TIME':
                return 'datetime-local';
            case 'EMAIL':
                return 'email';
            case 'NUMBER':
                return 'number';
            case 'PASSWORD':
                return 'password';
            case 'PHONE':
                return 'tel';
            case 'TIME':
                return 'time';
            case 'URL':
                return 'url';
            default:
                return 'text';
        }
    };

    return (
        <Input
            className={twMerge(
                dataPillPanelOpen &&
                    focusedInput?.name === name &&
                    'shadow-lg shadow-blue-200 ring ring-blue-500'
            )}
            dataPills={dataPill?.value}
            description={description}
            defaultValue={defaultValue as string}
            error={error}
            fieldsetClassName="flex-1 mb-0"
            key={name}
            label={label || name}
            leadingIcon={leadingIcon}
            name={name!}
            required={required}
            title={title}
            type={hidden ? 'hidden' : getInputType()}
            onFocus={() => {
                if (nodeDetailsDialogOpen) {
                    setDataPillPanelOpen(true);

                    if (inputRef.current) {
                        setFocusedInput(inputRef.current);
                    }
                }
            }}
            ref={inputRef}
        />
    );
};

export default InputProperty;
