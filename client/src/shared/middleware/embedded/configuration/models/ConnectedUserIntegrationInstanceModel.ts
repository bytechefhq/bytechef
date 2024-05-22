/* tslint:disable */
/* eslint-disable */
/**
 * The Embedded Configuration API
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
import type { CredentialStatusModel } from './CredentialStatusModel';
import {
    CredentialStatusModelFromJSON,
    CredentialStatusModelFromJSONTyped,
    CredentialStatusModelToJSON,
} from './CredentialStatusModel';

/**
 * 
 * @export
 * @interface ConnectedUserIntegrationInstanceModel
 */
export interface ConnectedUserIntegrationInstanceModel {
    /**
     * The name of a component.
     * @type {string}
     * @memberof ConnectedUserIntegrationInstanceModel
     */
    componentName?: string;
    /**
     * If an integration's instance is enable dor not.
     * @type {boolean}
     * @memberof ConnectedUserIntegrationInstanceModel
     */
    enabled?: boolean;
    /**
     * The id of an integration instance.
     * @type {number}
     * @memberof ConnectedUserIntegrationInstanceModel
     */
    readonly id?: number;
    /**
     * The id of an integration.
     * @type {number}
     * @memberof ConnectedUserIntegrationInstanceModel
     */
    readonly integrationId?: number;
    /**
     * The version of an integration.
     * @type {number}
     * @memberof ConnectedUserIntegrationInstanceModel
     */
    readonly integrationVersion?: number;
    /**
     * The id of a connection.
     * @type {number}
     * @memberof ConnectedUserIntegrationInstanceModel
     */
    readonly connectionId?: number;
    /**
     * 
     * @type {CredentialStatusModel}
     * @memberof ConnectedUserIntegrationInstanceModel
     */
    credentialStatus?: CredentialStatusModel;
}

/**
 * Check if a given object implements the ConnectedUserIntegrationInstanceModel interface.
 */
export function instanceOfConnectedUserIntegrationInstanceModel(value: object): boolean {
    return true;
}

export function ConnectedUserIntegrationInstanceModelFromJSON(json: any): ConnectedUserIntegrationInstanceModel {
    return ConnectedUserIntegrationInstanceModelFromJSONTyped(json, false);
}

export function ConnectedUserIntegrationInstanceModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): ConnectedUserIntegrationInstanceModel {
    if (json == null) {
        return json;
    }
    return {
        
        'componentName': json['componentName'] == null ? undefined : json['componentName'],
        'enabled': json['enabled'] == null ? undefined : json['enabled'],
        'id': json['id'] == null ? undefined : json['id'],
        'integrationId': json['integrationId'] == null ? undefined : json['integrationId'],
        'integrationVersion': json['integrationVersion'] == null ? undefined : json['integrationVersion'],
        'connectionId': json['connectionId'] == null ? undefined : json['connectionId'],
        'credentialStatus': json['credentialStatus'] == null ? undefined : CredentialStatusModelFromJSON(json['credentialStatus']),
    };
}

export function ConnectedUserIntegrationInstanceModelToJSON(value?: Omit<ConnectedUserIntegrationInstanceModel, 'id'|'integrationId'|'integrationVersion'|'connectionId'> | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'componentName': value['componentName'],
        'enabled': value['enabled'],
        'credentialStatus': CredentialStatusModelToJSON(value['credentialStatus']),
    };
}

