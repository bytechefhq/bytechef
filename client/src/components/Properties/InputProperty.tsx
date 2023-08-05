import Input from 'components/Input/Input';
import MentionsInput from 'components/MentionsInput/MentionsInput';
import {useDataPillPanelStore} from 'pages/automation/project/stores/useDataPillPanelStore';
import {useNodeDetailsDialogStore} from 'pages/automation/project/stores/useNodeDetailsDialogStore';
import useWorkflowDefinitionStore from 'pages/automation/project/stores/useWorkflowDefinitionStore';
import {ReactNode, useRef} from 'react';

interface InputPropertyProps {
    controlType?: string;
    description?: string;
    defaultValue?: string;
    error?: boolean;
    fieldsetClassName?: string;
    hidden?: boolean;
    mention?: boolean;
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
    mention,
    name,
    required,
    title,
}: InputPropertyProps) => {
    const {setDataPillPanelOpen} = useDataPillPanelStore();
    const {nodeDetailsDialogOpen, setFocusedInput} =
        useNodeDetailsDialogStore();

    const {dataPills} = useWorkflowDefinitionStore();

    const inputRef = useRef(null);

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
        <>
            {mention ? (
                <MentionsInput
                    data={dataPills}
                    id={name!}
                    placeholder="Mention datapills using '${'"
                />
            ) : (
                <Input
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
            )}
        </>
    );
};

export default InputProperty;
