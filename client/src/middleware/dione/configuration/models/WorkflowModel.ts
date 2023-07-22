/* tslint:disable */
/* eslint-disable */
/**
 * Embedded Configuration API
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
import type { InputModel } from './InputModel';
import {
    InputModelFromJSON,
    InputModelFromJSONTyped,
    InputModelToJSON,
} from './InputModel';
import type { OutputModel } from './OutputModel';
import {
    OutputModelFromJSON,
    OutputModelFromJSONTyped,
    OutputModelToJSON,
} from './OutputModel';
import type { WorkflowConnectionModel } from './WorkflowConnectionModel';
import {
    WorkflowConnectionModelFromJSON,
    WorkflowConnectionModelFromJSONTyped,
    WorkflowConnectionModelToJSON,
} from './WorkflowConnectionModel';
import type { WorkflowFormatModel } from './WorkflowFormatModel';
import {
    WorkflowFormatModelFromJSON,
    WorkflowFormatModelFromJSONTyped,
    WorkflowFormatModelToJSON,
} from './WorkflowFormatModel';
import type { WorkflowTaskModel } from './WorkflowTaskModel';
import {
    WorkflowTaskModelFromJSON,
    WorkflowTaskModelFromJSONTyped,
    WorkflowTaskModelToJSON,
} from './WorkflowTaskModel';

/**
 * The blueprint that describe the execution of a job.
 * @export
 * @interface WorkflowModel
 */
export interface WorkflowModel {
    /**
     * 
     * @type {Array<WorkflowConnectionModel>}
     * @memberof WorkflowModel
     */
    connections?: Array<WorkflowConnectionModel>;
    /**
     * The created by.
     * @type {string}
     * @memberof WorkflowModel
     */
    readonly createdBy?: string;
    /**
     * The created date.
     * @type {Date}
     * @memberof WorkflowModel
     */
    readonly createdDate?: Date;
    /**
     * The definition of a workflow.
     * @type {string}
     * @memberof WorkflowModel
     */
    definition?: string;
    /**
     * The description of a workflow.
     * @type {string}
     * @memberof WorkflowModel
     */
    description?: string;
    /**
     * 
     * @type {WorkflowFormatModel}
     * @memberof WorkflowModel
     */
    format?: WorkflowFormatModel;
    /**
     * The id of the workflow.
     * @type {string}
     * @memberof WorkflowModel
     */
    readonly id?: string;
    /**
     * The workflow's expected list of inputs.
     * @type {Array<InputModel>}
     * @memberof WorkflowModel
     */
    readonly inputs?: Array<InputModel>;
    /**
     * The descriptive name for the workflow
     * @type {string}
     * @memberof WorkflowModel
     */
    readonly label?: string;
    /**
     * The last modified by.
     * @type {string}
     * @memberof WorkflowModel
     */
    readonly lastModifiedBy?: string;
    /**
     * The last modified date.
     * @type {Date}
     * @memberof WorkflowModel
     */
    readonly lastModifiedDate?: Date;
    /**
     * The workflow's list of expected outputs.
     * @type {Array<OutputModel>}
     * @memberof WorkflowModel
     */
    readonly outputs?: Array<OutputModel>;
    /**
     * The type of the source which stores the workflow definition.
     * @type {string}
     * @memberof WorkflowModel
     */
    sourceType?: WorkflowModelSourceTypeEnum;
    /**
     * The maximum number of times a task may retry.
     * @type {number}
     * @memberof WorkflowModel
     */
    readonly maxRetries?: number;
    /**
     * The steps that make up the workflow.
     * @type {Array<WorkflowTaskModel>}
     * @memberof WorkflowModel
     */
    readonly tasks?: Array<WorkflowTaskModel>;
    /**
     * 
     * @type {number}
     * @memberof WorkflowModel
     */
    version?: number;
}


/**
 * @export
 */
export const WorkflowModelSourceTypeEnum = {
    Classpath: 'CLASSPATH',
    Filesystem: 'FILESYSTEM',
    Git: 'GIT',
    Jdbc: 'JDBC'
} as const;
export type WorkflowModelSourceTypeEnum = typeof WorkflowModelSourceTypeEnum[keyof typeof WorkflowModelSourceTypeEnum];


/**
 * Check if a given object implements the WorkflowModel interface.
 */
export function instanceOfWorkflowModel(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function WorkflowModelFromJSON(json: any): WorkflowModel {
    return WorkflowModelFromJSONTyped(json, false);
}

export function WorkflowModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'connections': !exists(json, 'connections') ? undefined : ((json['connections'] as Array<any>).map(WorkflowConnectionModelFromJSON)),
        'createdBy': !exists(json, 'createdBy') ? undefined : json['createdBy'],
        'createdDate': !exists(json, 'createdDate') ? undefined : (new Date(json['createdDate'])),
        'definition': !exists(json, 'definition') ? undefined : json['definition'],
        'description': !exists(json, 'description') ? undefined : json['description'],
        'format': !exists(json, 'format') ? undefined : WorkflowFormatModelFromJSON(json['format']),
        'id': !exists(json, 'id') ? undefined : json['id'],
        'inputs': !exists(json, 'inputs') ? undefined : ((json['inputs'] as Array<any>).map(InputModelFromJSON)),
        'label': !exists(json, 'label') ? undefined : json['label'],
        'lastModifiedBy': !exists(json, 'lastModifiedBy') ? undefined : json['lastModifiedBy'],
        'lastModifiedDate': !exists(json, 'lastModifiedDate') ? undefined : (new Date(json['lastModifiedDate'])),
        'outputs': !exists(json, 'outputs') ? undefined : ((json['outputs'] as Array<any>).map(OutputModelFromJSON)),
        'sourceType': !exists(json, 'sourceType') ? undefined : json['sourceType'],
        'maxRetries': !exists(json, 'maxRetries') ? undefined : json['maxRetries'],
        'tasks': !exists(json, 'tasks') ? undefined : ((json['tasks'] as Array<any>).map(WorkflowTaskModelFromJSON)),
        'version': !exists(json, '__version') ? undefined : json['__version'],
    };
}

export function WorkflowModelToJSON(value?: WorkflowModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'connections': value.connections === undefined ? undefined : ((value.connections as Array<any>).map(WorkflowConnectionModelToJSON)),
        'definition': value.definition,
        'description': value.description,
        'format': WorkflowFormatModelToJSON(value.format),
        'sourceType': value.sourceType,
        '__version': value.version,
    };
}

