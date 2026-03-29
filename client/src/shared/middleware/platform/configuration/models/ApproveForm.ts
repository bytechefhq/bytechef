import type {TriggerFormInput} from './TriggerFormInput';
import {TriggerFormInputFromJSON, TriggerFormInputToJSON} from './TriggerFormInput';

/**
 * @export
 * @interface ApproveForm
 */
export interface ApproveForm {
    /**
     * @type {string}
     * @memberof ApproveForm
     */
    formDescription?: string;
    /**
     * @type {string}
     * @memberof ApproveForm
     */
    formTitle?: string;
    /**
     * @type {Array<TriggerFormInput>}
     * @memberof ApproveForm
     */
    inputs?: Array<TriggerFormInput>;
}

export function ApproveFormFromJSON(json: any): ApproveForm {
    if (json == null) {
        return json;
    }

    return {
        formDescription: json['formDescription'] == null ? undefined : json['formDescription'],
        formTitle: json['formTitle'] == null ? undefined : json['formTitle'],
        inputs: json['inputs'] == null ? undefined : (json['inputs'] as Array<any>).map(TriggerFormInputFromJSON),
    };
}

export function ApproveFormToJSON(value?: ApproveForm | null): any {
    if (value == null) {
        return value;
    }

    return {
        formDescription: value['formDescription'],
        formTitle: value['formTitle'],
        inputs:
            value['inputs'] == null ? undefined : (value['inputs'] as Array<any>).map(TriggerFormInputToJSON),
    };
}
