/* tslint:disable */
/* eslint-disable */
/**
 * Automation Configuration API
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
 * 
 * @export
 */
export const WorkflowFormatModel = {
    Json: 'JSON',
    Yaml: 'YAML'
} as const;
export type WorkflowFormatModel = typeof WorkflowFormatModel[keyof typeof WorkflowFormatModel];


export function WorkflowFormatModelFromJSON(json: any): WorkflowFormatModel {
    return WorkflowFormatModelFromJSONTyped(json, false);
}

export function WorkflowFormatModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowFormatModel {
    return json as WorkflowFormatModel;
}

export function WorkflowFormatModelToJSON(value?: WorkflowFormatModel | null): any {
    return value as any;
}

