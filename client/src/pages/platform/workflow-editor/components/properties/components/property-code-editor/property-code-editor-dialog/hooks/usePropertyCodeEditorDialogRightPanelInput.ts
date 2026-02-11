import {usePropertyCodeEditorDialogStore} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/stores/usePropertyCodeEditorDialogStore';
import {useCallback, useEffect, useMemo, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

type InputValueType = Record<string, unknown>;

interface UsePropertyCodeEditorDialogRightPanelInputPropsI {
    input: InputValueType;
}

export const usePropertyCodeEditorDialogRightPanelInput = ({
    input,
}: UsePropertyCodeEditorDialogRightPanelInputPropsI) => {
    const [jsonValue, setJsonValue] = useState(() => JSON.stringify(input, null, 2));
    const [parseError, setParseError] = useState<string | null>(null);

    const {setInputParameters} = usePropertyCodeEditorDialogStore(
        useShallow((state) => ({
            setInputParameters: state.setInputParameters,
        }))
    );

    const initialJsonValue = useMemo(() => JSON.stringify(input, null, 2), [input]);

    useEffect(() => {
        setJsonValue(initialJsonValue);
        setInputParameters(input);
        setParseError(null);
    }, [initialJsonValue, input, setInputParameters]);

    const handleEditorChange = useCallback(
        (value: string | undefined) => {
            const newValue = value ?? '';

            setJsonValue(newValue);

            try {
                const parsedValue = JSON.parse(newValue);

                if (typeof parsedValue === 'object' && parsedValue !== null && !Array.isArray(parsedValue)) {
                    setInputParameters(parsedValue as InputValueType);
                    setParseError(null);
                } else {
                    setParseError('JSON must be an object');
                }
            } catch {
                setParseError('Invalid JSON');
            }
        },
        [setInputParameters]
    );

    const handleReset = useCallback(() => {
        setJsonValue(initialJsonValue);
        setInputParameters(input);
        setParseError(null);
    }, [initialJsonValue, input, setInputParameters]);

    const hasChanges = jsonValue !== initialJsonValue;

    return {
        handleEditorChange,
        handleReset,
        hasChanges,
        jsonValue,
        parseError,
    };
};
