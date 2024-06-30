/* tslint:disable */
/* eslint-disable */
/**
 * The Platform Configuration Internal API
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
/**
 * 
 * @export
 * @interface OAuth2AuthorizationParametersModel
 */
export interface OAuth2AuthorizationParametersModel {
    /**
     * 
     * @type {string}
     * @memberof OAuth2AuthorizationParametersModel
     */
    authorizationUrl?: string;
    /**
     * 
     * @type {{ [key: string]: string; }}
     * @memberof OAuth2AuthorizationParametersModel
     */
    extraQueryParameters?: { [key: string]: string; };
    /**
     * 
     * @type {string}
     * @memberof OAuth2AuthorizationParametersModel
     */
    clientId?: string;
    /**
     * 
     * @type {Array<string>}
     * @memberof OAuth2AuthorizationParametersModel
     */
    scopes?: Array<string>;
}

/**
 * Check if a given object implements the OAuth2AuthorizationParametersModel interface.
 */
export function instanceOfOAuth2AuthorizationParametersModel(value: object): boolean {
    return true;
}

export function OAuth2AuthorizationParametersModelFromJSON(json: any): OAuth2AuthorizationParametersModel {
    return OAuth2AuthorizationParametersModelFromJSONTyped(json, false);
}

export function OAuth2AuthorizationParametersModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): OAuth2AuthorizationParametersModel {
    if (json == null) {
        return json;
    }
    return {
        
        'authorizationUrl': json['authorizationUrl'] == null ? undefined : json['authorizationUrl'],
        'extraQueryParameters': json['extraQueryParameters'] == null ? undefined : json['extraQueryParameters'],
        'clientId': json['clientId'] == null ? undefined : json['clientId'],
        'scopes': json['scopes'] == null ? undefined : json['scopes'],
    };
}

export function OAuth2AuthorizationParametersModelToJSON(value?: OAuth2AuthorizationParametersModel | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'authorizationUrl': value['authorizationUrl'],
        'extraQueryParameters': value['extraQueryParameters'],
        'clientId': value['clientId'],
        'scopes': value['scopes'],
    };
}

