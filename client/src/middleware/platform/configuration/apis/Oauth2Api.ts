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


import * as runtime from '../runtime';
import type {
  GetOAuth2AuthorizationParametersRequestModel,
  OAuth2AuthorizationParametersModel,
  OAuth2PropertiesModel,
} from '../models/index';
import {
    GetOAuth2AuthorizationParametersRequestModelFromJSON,
    GetOAuth2AuthorizationParametersRequestModelToJSON,
    OAuth2AuthorizationParametersModelFromJSON,
    OAuth2AuthorizationParametersModelToJSON,
    OAuth2PropertiesModelFromJSON,
    OAuth2PropertiesModelToJSON,
} from '../models/index';

export interface GetOAuth2AuthorizationParametersRequest {
    getOAuth2AuthorizationParametersRequestModel: GetOAuth2AuthorizationParametersRequestModel;
}

/**
 * 
 */
export class Oauth2Api extends runtime.BaseAPI {

    /**
     * Retrieves oauth2 authorization parameters.
     * Retrieves oauth2 authorization parameters
     */
    async getOAuth2AuthorizationParametersRaw(requestParameters: GetOAuth2AuthorizationParametersRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<OAuth2AuthorizationParametersModel>> {
        if (requestParameters['getOAuth2AuthorizationParametersRequestModel'] == null) {
            throw new runtime.RequiredError(
                'getOAuth2AuthorizationParametersRequestModel',
                'Required parameter "getOAuth2AuthorizationParametersRequestModel" was null or undefined when calling getOAuth2AuthorizationParameters().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/oauth2/authorization-parameters`,
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: GetOAuth2AuthorizationParametersRequestModelToJSON(requestParameters['getOAuth2AuthorizationParametersRequestModel']),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => OAuth2AuthorizationParametersModelFromJSON(jsonValue));
    }

    /**
     * Retrieves oauth2 authorization parameters.
     * Retrieves oauth2 authorization parameters
     */
    async getOAuth2AuthorizationParameters(requestParameters: GetOAuth2AuthorizationParametersRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<OAuth2AuthorizationParametersModel> {
        const response = await this.getOAuth2AuthorizationParametersRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get OAuth2 properties.
     * Get OAuth2 properties
     */
    async getOAuth2PropertiesRaw(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<OAuth2PropertiesModel>> {
        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/oauth2/properties`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => OAuth2PropertiesModelFromJSON(jsonValue));
    }

    /**
     * Get OAuth2 properties.
     * Get OAuth2 properties
     */
    async getOAuth2Properties(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<OAuth2PropertiesModel> {
        const response = await this.getOAuth2PropertiesRaw(initOverrides);
        return await response.value();
    }

}
