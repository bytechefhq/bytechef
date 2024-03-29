/* tslint:disable */
/* eslint-disable */
/**
 * The Platform Configuration API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { mapValues } from '../runtime';
import type { WorkflowTestConfigurationConnectionModel } from './WorkflowTestConfigurationConnectionModel';
import {
    WorkflowTestConfigurationConnectionModelFromJSON,
    WorkflowTestConfigurationConnectionModelFromJSONTyped,
    WorkflowTestConfigurationConnectionModelToJSON,
} from './WorkflowTestConfigurationConnectionModel';

/**
 * Contains configuration and connections required for the test execution of a particular workflow.
 * @export
 * @interface WorkflowTestConfigurationModel
 */
export interface WorkflowTestConfigurationModel {
    /**
     * The created by.
     * @type {string}
     * @memberof WorkflowTestConfigurationModel
     */
    readonly createdBy?: string;
    /**
     * The created date.
     * @type {Date}
     * @memberof WorkflowTestConfigurationModel
     */
    readonly createdDate?: Date;
    /**
     * The input parameters used as workflow input values.
     * @type {{ [key: string]: string; }}
     * @memberof WorkflowTestConfigurationModel
     */
    inputs?: { [key: string]: string; };
    /**
     * The connections used by workflow test.
     * @type {Array<WorkflowTestConfigurationConnectionModel>}
     * @memberof WorkflowTestConfigurationModel
     */
    connections?: Array<WorkflowTestConfigurationConnectionModel>;
    /**
     * The last modified by.
     * @type {string}
     * @memberof WorkflowTestConfigurationModel
     */
    readonly lastModifiedBy?: string;
    /**
     * The last modified date.
     * @type {Date}
     * @memberof WorkflowTestConfigurationModel
     */
    readonly lastModifiedDate?: Date;
    /**
     * The id of a workflow.
     * @type {string}
     * @memberof WorkflowTestConfigurationModel
     */
    readonly workflowId?: string;
    /**
     * 
     * @type {number}
     * @memberof WorkflowTestConfigurationModel
     */
    version?: number;
}

/**
 * Check if a given object implements the WorkflowTestConfigurationModel interface.
 */
export function instanceOfWorkflowTestConfigurationModel(value: object): boolean {
    return true;
}

export function WorkflowTestConfigurationModelFromJSON(json: any): WorkflowTestConfigurationModel {
    return WorkflowTestConfigurationModelFromJSONTyped(json, false);
}

export function WorkflowTestConfigurationModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowTestConfigurationModel {
    if (json == null) {
        return json;
    }
    return {
        
        'createdBy': json['createdBy'] == null ? undefined : json['createdBy'],
        'createdDate': json['createdDate'] == null ? undefined : (new Date(json['createdDate'])),
        'inputs': json['inputs'] == null ? undefined : json['inputs'],
        'connections': json['connections'] == null ? undefined : ((json['connections'] as Array<any>).map(WorkflowTestConfigurationConnectionModelFromJSON)),
        'lastModifiedBy': json['lastModifiedBy'] == null ? undefined : json['lastModifiedBy'],
        'lastModifiedDate': json['lastModifiedDate'] == null ? undefined : (new Date(json['lastModifiedDate'])),
        'workflowId': json['workflowId'] == null ? undefined : json['workflowId'],
        'version': json['__version'] == null ? undefined : json['__version'],
    };
}

export function WorkflowTestConfigurationModelToJSON(value?: WorkflowTestConfigurationModel | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'inputs': value['inputs'],
        'connections': value['connections'] == null ? undefined : ((value['connections'] as Array<any>).map(WorkflowTestConfigurationConnectionModelToJSON)),
        '__version': value['version'],
    };
}

