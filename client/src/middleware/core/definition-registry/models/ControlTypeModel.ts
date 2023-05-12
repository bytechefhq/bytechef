/* tslint:disable */
/* eslint-disable */
/**
 * Core Definition API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


/**
 * A type of the control to show in UI.
 * @export
 */
export const ControlTypeModel = {
    Checkbox: 'CHECKBOX',
    CodeEditor: 'CODE_EDITOR',
    Date: 'DATE',
    DateTime: 'DATE_TIME',
    Email: 'EMAIL',
    Expression: 'EXPRESSION',
    InputEmail: 'INPUT_EMAIL',
    InputInteger: 'INPUT_INTEGER',
    InputNumber: 'INPUT_NUMBER',
    InputPassword: 'INPUT_PASSWORD',
    InputPhone: 'INPUT_PHONE',
    InputText: 'INPUT_TEXT',
    InputUrl: 'INPUT_URL',
    JsonBuilder: 'OBJECT_BUILDER',
    MultiSelect: 'MULTI_SELECT',
    Phone: 'PHONE',
    SchemaDesigner: 'SCHEMA_DESIGNER',
    Select: 'SELECT',
    Subdomain: 'SUBDOMAIN',
    TextArea: 'TEXT_AREA',
    Time: 'TIME',
    Url: 'URL'
} as const;
export type ControlTypeModel = typeof ControlTypeModel[keyof typeof ControlTypeModel];


export function ControlTypeModelFromJSON(json: any): ControlTypeModel {
    return ControlTypeModelFromJSONTyped(json, false);
}

export function ControlTypeModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): ControlTypeModel {
    return json as ControlTypeModel;
}

export function ControlTypeModelToJSON(value?: ControlTypeModel | null): any {
    return value as any;
}

