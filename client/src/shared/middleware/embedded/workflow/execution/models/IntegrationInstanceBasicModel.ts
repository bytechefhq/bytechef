/* tslint:disable */
/* eslint-disable */
/**
 * Embedded Execution API
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
import type { Environment1Model } from './Environment1Model';
import {
    Environment1ModelFromJSON,
    Environment1ModelFromJSONTyped,
    Environment1ModelToJSON,
} from './Environment1Model';

/**
 * Contains configurations and connections required for the execution of integration workflows for a connected user.
 * @export
 * @interface IntegrationInstanceBasicModel
 */
export interface IntegrationInstanceBasicModel {
    /**
     * The id of a connection.
     * @type {number}
     * @memberof IntegrationInstanceBasicModel
     */
    readonly connectionId: number;
    /**
     * The id of a connected user.
     * @type {number}
     * @memberof IntegrationInstanceBasicModel
     */
    readonly connectedUserId?: number;
    /**
     * The created by.
     * @type {string}
     * @memberof IntegrationInstanceBasicModel
     */
    readonly createdBy?: string;
    /**
     * The created date.
     * @type {Date}
     * @memberof IntegrationInstanceBasicModel
     */
    readonly createdDate?: Date;
    /**
     * If an integration instance is enabled or not.
     * @type {boolean}
     * @memberof IntegrationInstanceBasicModel
     */
    enabled?: boolean;
    /**
     * 
     * @type {Environment1Model}
     * @memberof IntegrationInstanceBasicModel
     */
    environment?: Environment1Model;
    /**
     * The id of an integration instance.
     * @type {number}
     * @memberof IntegrationInstanceBasicModel
     */
    readonly id?: number;
    /**
     * The last execution date.
     * @type {Date}
     * @memberof IntegrationInstanceBasicModel
     */
    readonly lastExecutionDate?: Date;
    /**
     * The last modified by.
     * @type {string}
     * @memberof IntegrationInstanceBasicModel
     */
    readonly lastModifiedBy?: string;
    /**
     * The last modified date.
     * @type {Date}
     * @memberof IntegrationInstanceBasicModel
     */
    readonly lastModifiedDate?: Date;
    /**
     * Th id of an integration instance configuration.
     * @type {number}
     * @memberof IntegrationInstanceBasicModel
     */
    integrationInstanceConfigurationId?: number;
}

/**
 * Check if a given object implements the IntegrationInstanceBasicModel interface.
 */
export function instanceOfIntegrationInstanceBasicModel(value: object): boolean {
    if (!('connectionId' in value)) return false;
    return true;
}

export function IntegrationInstanceBasicModelFromJSON(json: any): IntegrationInstanceBasicModel {
    return IntegrationInstanceBasicModelFromJSONTyped(json, false);
}

export function IntegrationInstanceBasicModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): IntegrationInstanceBasicModel {
    if (json == null) {
        return json;
    }
    return {
        
        'connectionId': json['connectionId'],
        'connectedUserId': json['connectedUserId'] == null ? undefined : json['connectedUserId'],
        'createdBy': json['createdBy'] == null ? undefined : json['createdBy'],
        'createdDate': json['createdDate'] == null ? undefined : (new Date(json['createdDate'])),
        'enabled': json['enabled'] == null ? undefined : json['enabled'],
        'environment': json['environment'] == null ? undefined : Environment1ModelFromJSON(json['environment']),
        'id': json['id'] == null ? undefined : json['id'],
        'lastExecutionDate': json['lastExecutionDate'] == null ? undefined : (new Date(json['lastExecutionDate'])),
        'lastModifiedBy': json['lastModifiedBy'] == null ? undefined : json['lastModifiedBy'],
        'lastModifiedDate': json['lastModifiedDate'] == null ? undefined : (new Date(json['lastModifiedDate'])),
        'integrationInstanceConfigurationId': json['integrationInstanceConfigurationId'] == null ? undefined : json['integrationInstanceConfigurationId'],
    };
}

export function IntegrationInstanceBasicModelToJSON(value?: Omit<IntegrationInstanceBasicModel, 'connectionId'|'connectedUserId'|'createdBy'|'createdDate'|'id'|'lastExecutionDate'|'lastModifiedBy'|'lastModifiedDate'> | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'enabled': value['enabled'],
        'environment': Environment1ModelToJSON(value['environment']),
        'integrationInstanceConfigurationId': value['integrationInstanceConfigurationId'],
    };
}

