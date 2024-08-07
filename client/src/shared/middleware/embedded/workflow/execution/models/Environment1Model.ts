/* tslint:disable */
/* eslint-disable */
/**
 * Embedded Execution Internal API
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
 * The environment of an integration.
 * @export
 */
export const Environment1Model = {
    Test: 'TEST',
    Production: 'PRODUCTION'
} as const;
export type Environment1Model = typeof Environment1Model[keyof typeof Environment1Model];


export function instanceOfEnvironment1Model(value: any): boolean {
    return Object.values(Environment1Model).includes(value);
}

export function Environment1ModelFromJSON(json: any): Environment1Model {
    return Environment1ModelFromJSONTyped(json, false);
}

export function Environment1ModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): Environment1Model {
    return json as Environment1Model;
}

export function Environment1ModelToJSON(value?: Environment1Model | null): any {
    return value as any;
}

