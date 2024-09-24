/* tslint:disable */
/* eslint-disable */
/**
 * The Platform API Connector Internal API
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
 * The HTTP method.
 * @export
 */
export const HttpMethod = {
    Delete: 'DELETE',
    Get: 'GET',
    Post: 'POST',
    Put: 'PUT',
    Patch: 'PATCH'
} as const;
export type HttpMethod = typeof HttpMethod[keyof typeof HttpMethod];


export function instanceOfHttpMethod(value: any): boolean {
    for (const key in HttpMethod) {
        if (Object.prototype.hasOwnProperty.call(HttpMethod, key)) {
            if (HttpMethod[key as keyof typeof HttpMethod] === value) {
                return true;
            }
        }
    }
    return false;
}

export function HttpMethodFromJSON(json: any): HttpMethod {
    return HttpMethodFromJSONTyped(json, false);
}

export function HttpMethodFromJSONTyped(json: any, ignoreDiscriminator: boolean): HttpMethod {
    return json as HttpMethod;
}

export function HttpMethodToJSON(value?: HttpMethod | null): any {
    return value as any;
}

