/* tslint:disable */
/* eslint-disable */
/**
 * The Platform User Internal API
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
  AdminApiKey,
  CreateAdminApiKey200Response,
} from '../models/index';
import {
    AdminApiKeyFromJSON,
    AdminApiKeyToJSON,
    CreateAdminApiKey200ResponseFromJSON,
    CreateAdminApiKey200ResponseToJSON,
} from '../models/index';

export interface CreateAdminApiKeyRequest {
    adminApiKey: Omit<AdminApiKey, 'createdBy'|'createdDate'|'id'|'lastModifiedBy'|'lastModifiedDate'|'lastUsedDate'|'secretKey'>;
}

export interface DeleteAdminApiKeyRequest {
    id: number;
}

export interface GetAdminApiKeyRequest {
    id: number;
}

export interface UpdateAdminApiKeyRequest {
    id: number;
    adminApiKey: Omit<AdminApiKey, 'createdBy'|'createdDate'|'id'|'lastModifiedBy'|'lastModifiedDate'|'lastUsedDate'|'secretKey'>;
}

/**
 * 
 */
export class AdminApiKeyApi extends runtime.BaseAPI {

    /**
     * Create a new admin API key.
     * Create a new admin API key
     */
    async createAdminApiKeyRaw(requestParameters: CreateAdminApiKeyRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<CreateAdminApiKey200Response>> {
        if (requestParameters['adminApiKey'] == null) {
            throw new runtime.RequiredError(
                'adminApiKey',
                'Required parameter "adminApiKey" was null or undefined when calling createAdminApiKey().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/admin-api-keys`,
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: AdminApiKeyToJSON(requestParameters['adminApiKey']),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => CreateAdminApiKey200ResponseFromJSON(jsonValue));
    }

    /**
     * Create a new admin API key.
     * Create a new admin API key
     */
    async createAdminApiKey(requestParameters: CreateAdminApiKeyRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<CreateAdminApiKey200Response> {
        const response = await this.createAdminApiKeyRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Delete an admin API key.
     * Delete an admin API key
     */
    async deleteAdminApiKeyRaw(requestParameters: DeleteAdminApiKeyRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters['id'] == null) {
            throw new runtime.RequiredError(
                'id',
                'Required parameter "id" was null or undefined when calling deleteAdminApiKey().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/admin-api-keys/{id}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters['id']))),
            method: 'DELETE',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Delete an admin API key.
     * Delete an admin API key
     */
    async deleteAdminApiKey(requestParameters: DeleteAdminApiKeyRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.deleteAdminApiKeyRaw(requestParameters, initOverrides);
    }

    /**
     * Get an admin API key by id.
     * Get an admin API key by id
     */
    async getAdminApiKeyRaw(requestParameters: GetAdminApiKeyRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<AdminApiKey>> {
        if (requestParameters['id'] == null) {
            throw new runtime.RequiredError(
                'id',
                'Required parameter "id" was null or undefined when calling getAdminApiKey().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/admin-api-keys/{id}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters['id']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => AdminApiKeyFromJSON(jsonValue));
    }

    /**
     * Get an admin API key by id.
     * Get an admin API key by id
     */
    async getAdminApiKey(requestParameters: GetAdminApiKeyRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<AdminApiKey> {
        const response = await this.getAdminApiKeyRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get admin API keys.
     * Get admin api keys
     */
    async getAdminApiKeysRaw(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<AdminApiKey>>> {
        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/admin-api-keys`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(AdminApiKeyFromJSON));
    }

    /**
     * Get admin API keys.
     * Get admin api keys
     */
    async getAdminApiKeys(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<AdminApiKey>> {
        const response = await this.getAdminApiKeysRaw(initOverrides);
        return await response.value();
    }

    /**
     * Update an existing admin API key.
     * Update an existing admin API key
     */
    async updateAdminApiKeyRaw(requestParameters: UpdateAdminApiKeyRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<AdminApiKey>> {
        if (requestParameters['id'] == null) {
            throw new runtime.RequiredError(
                'id',
                'Required parameter "id" was null or undefined when calling updateAdminApiKey().'
            );
        }

        if (requestParameters['adminApiKey'] == null) {
            throw new runtime.RequiredError(
                'adminApiKey',
                'Required parameter "adminApiKey" was null or undefined when calling updateAdminApiKey().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/admin-api-keys/{id}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters['id']))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: AdminApiKeyToJSON(requestParameters['adminApiKey']),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => AdminApiKeyFromJSON(jsonValue));
    }

    /**
     * Update an existing admin API key.
     * Update an existing admin API key
     */
    async updateAdminApiKey(requestParameters: UpdateAdminApiKeyRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<AdminApiKey> {
        const response = await this.updateAdminApiKeyRaw(requestParameters, initOverrides);
        return await response.value();
    }

}
