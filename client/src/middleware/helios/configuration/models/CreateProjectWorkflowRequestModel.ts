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

import { exists, mapValues } from '../runtime';
/**
 * 
 * @export
 * @interface CreateProjectWorkflowRequestModel
 */
export interface CreateProjectWorkflowRequestModel {
    /**
     * The descriptive name for a workflow.
     * @type {string}
     * @memberof CreateProjectWorkflowRequestModel
     */
    label: string;
    /**
     * The workflow description.
     * @type {string}
     * @memberof CreateProjectWorkflowRequestModel
     */
    description?: string;
    /**
     * The workflow definition.
     * @type {string}
     * @memberof CreateProjectWorkflowRequestModel
     */
    definition?: string;
}

/**
 * Check if a given object implements the CreateProjectWorkflowRequestModel interface.
 */
export function instanceOfCreateProjectWorkflowRequestModel(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "label" in value;

    return isInstance;
}

export function CreateProjectWorkflowRequestModelFromJSON(json: any): CreateProjectWorkflowRequestModel {
    return CreateProjectWorkflowRequestModelFromJSONTyped(json, false);
}

export function CreateProjectWorkflowRequestModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): CreateProjectWorkflowRequestModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'label': json['label'],
        'description': !exists(json, 'description') ? undefined : json['description'],
        'definition': !exists(json, 'definition') ? undefined : json['definition'],
    };
}

export function CreateProjectWorkflowRequestModelToJSON(value?: CreateProjectWorkflowRequestModel | null): any {
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

