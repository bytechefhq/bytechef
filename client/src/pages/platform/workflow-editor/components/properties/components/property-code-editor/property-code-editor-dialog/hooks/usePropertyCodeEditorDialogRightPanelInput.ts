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
    const {setInputParameters} = usePropertyCodeEditorDialogStore(
        useShallow((state) => ({
            setInputParameters: state.setInputParameters,
        }))
    );

    const initialJsonValue = useMemo(() => JSON.stringify(input, null, 2), [input]);

    const [jsonValue, setJsonValue] = useState(initialJsonValue);
    const [parseError, setParseError] = useState<string | null>(null);

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
                const parsed = JSON.parse(newValue);

                if (typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed)) {
                    setInputParameters(parsed as InputValueType);
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
