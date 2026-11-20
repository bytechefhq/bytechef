import {useMemo, useState} from 'react';

export interface MoveImpactWorkflowI {
    affectedConnectedUserCount: number;
    awaitsConfiguration: boolean;
    label: string;
    newRequiredConnections: string[];
    newRequiredInputs: string[];
    workflowUuid?: string;
}

export type MoveWorkflowInputValuesType = Record<string, Record<string, string>>;

export interface MoveWorkflowInputsI {
    inputs: Record<string, string>;
    workflowUuid: string;
}

const useMoveWorkflowInputs = (impact?: MoveImpactWorkflowI[]) => {
    const [inputValues, setInputValues] = useState<MoveWorkflowInputValuesType>({});

    const workflowInputs = useMemo<MoveWorkflowInputsI[]>(
        () =>
            (impact || [])
                .filter(
                    (impactWorkflow) => !!impactWorkflow.workflowUuid && impactWorkflow.newRequiredInputs.length > 0
                )
                .map((impactWorkflow) => {
                    const workflowUuid = impactWorkflow.workflowUuid!;

                    return {
                        inputs: Object.fromEntries(
                            impactWorkflow.newRequiredInputs.map((inputName) => [
                                inputName,
                                (inputValues[workflowUuid]?.[inputName] || '').trim(),
                            ])
                        ),
                        workflowUuid,
                    };
                }),
        [impact, inputValues]
    );

    const missingRequiredInput = workflowInputs.some((workflowInput) =>
        Object.values(workflowInput.inputs).some((value) => !value)
    );

    const setInputValue = (workflowUuid: string, inputName: string, value: string) =>
        setInputValues((previousInputValues) => ({
            ...previousInputValues,
            [workflowUuid]: {...previousInputValues[workflowUuid], [inputName]: value},
        }));

    return {inputValues, missingRequiredInput, setInputValue, workflowInputs};
};

export default useMoveWorkflowInputs;
