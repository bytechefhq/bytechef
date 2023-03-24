/* tslint:disable */
/* eslint-disable */
/**
 * OpenAPI definition
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: v1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
/**
 * 
 * @export
 * @interface CreateIntegrationWorkflowRequestModel
 */
export interface CreateIntegrationWorkflowRequestModel {
    /**
     * The descriptive name for a workflow.
     * @type {string}
     * @memberof CreateIntegrationWorkflowRequestModel
     */
    label: string;
    /**
     * The workflow description.
     * @type {string}
     * @memberof CreateIntegrationWorkflowRequestModel
     */
    description?: string;
    /**
     * The workflow definition.
     * @type {string}
     * @memberof CreateIntegrationWorkflowRequestModel
     */
    definition?: string;
}

/**
 * Check if a given object implements the CreateIntegrationWorkflowRequestModel interface.
 */
export function instanceOfCreateIntegrationWorkflowRequestModel(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "label" in value;

    return isInstance;
}

export function CreateIntegrationWorkflowRequestModelFromJSON(json: any): CreateIntegrationWorkflowRequestModel {
    return CreateIntegrationWorkflowRequestModelFromJSONTyped(json, false);
}

export function CreateIntegrationWorkflowRequestModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): CreateIntegrationWorkflowRequestModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'label': json['label'],
        'description': !exists(json, 'description') ? undefined : json['description'],
        'definition': !exists(json, 'definition') ? undefined : json['definition'],
    };
}

export function CreateIntegrationWorkflowRequestModelToJSON(value?: CreateIntegrationWorkflowRequestModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'label': value.label,
        'description': value.description,
        'definition': value.definition,
    };
}

