/* tslint:disable */
/* eslint-disable */
/**
 * OpenAPI definition
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: v1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


import * as runtime from '../runtime';
import type {
  CategoryModel,
  IntegrationModel,
  PostIntegrationWorkflowRequestModel,
  TagModel,
} from '../models';
import {
    CategoryModelFromJSON,
    CategoryModelToJSON,
    IntegrationModelFromJSON,
    IntegrationModelToJSON,
    PostIntegrationWorkflowRequestModelFromJSON,
    PostIntegrationWorkflowRequestModelToJSON,
    TagModelFromJSON,
    TagModelToJSON,
} from '../models';

export interface DeleteIntegrationRequest {
    id: number;
}

export interface GetIntegrationRequest {
    id: number;
}

export interface PostIntegrationRequest {
    integrationModel: IntegrationModel;
}

export interface PostIntegrationWorkflowRequest {
    id: number;
    postIntegrationWorkflowRequestModel: PostIntegrationWorkflowRequestModel;
}

export interface PutIntegrationRequest {
    id: number;
    integrationModel: IntegrationModel;
}

export interface PutIntegrationTagsRequest {
    id: number;
    tagModel: Array<TagModel>;
}

/**
 * 
 */
export class IntegrationsApi extends runtime.BaseAPI {

    /**
     * Delete an integration.
     * Delete an integration.
     */
    async deleteIntegrationRaw(requestParameters: DeleteIntegrationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters.id === null || requestParameters.id === undefined) {
            throw new runtime.RequiredError('id','Required parameter requestParameters.id was null or undefined when calling deleteIntegration.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/integrations/{id}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters.id))),
            method: 'DELETE',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Delete an integration.
     * Delete an integration.
     */
    async deleteIntegration(requestParameters: DeleteIntegrationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.deleteIntegrationRaw(requestParameters, initOverrides);
    }

    /**
     * Get an integration by id.
     * Get an integration by id.
     */
    async getIntegrationRaw(requestParameters: GetIntegrationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<IntegrationModel>> {
        if (requestParameters.id === null || requestParameters.id === undefined) {
            throw new runtime.RequiredError('id','Required parameter requestParameters.id was null or undefined when calling getIntegration.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/integrations/{id}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters.id))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => IntegrationModelFromJSON(jsonValue));
    }

    /**
     * Get an integration by id.
     * Get an integration by id.
     */
    async getIntegration(requestParameters: GetIntegrationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<IntegrationModel> {
        const response = await this.getIntegrationRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get categories.
     * Get categories.
     */
    async getIntegrationCategoriesRaw(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<CategoryModel>>> {
        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/integrations/categories`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(CategoryModelFromJSON));
    }

    /**
     * Get categories.
     * Get categories.
     */
    async getIntegrationCategories(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<CategoryModel>> {
        const response = await this.getIntegrationCategoriesRaw(initOverrides);
        return await response.value();
    }

    /**
     * Get integration tags.
     * Get integration tags.
     */
    async getIntegrationTagsRaw(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<TagModel>>> {
        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/integrations/tags`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(TagModelFromJSON));
    }

    /**
     * Get integration tags.
     * Get integration tags.
     */
    async getIntegrationTags(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<TagModel>> {
        const response = await this.getIntegrationTagsRaw(initOverrides);
        return await response.value();
    }

    /**
     * Get integrations.
     * Get integrations.
     */
    async getIntegrationsRaw(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<IntegrationModel>>> {
        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/integrations`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(IntegrationModelFromJSON));
    }

    /**
     * Get integrations.
     * Get integrations.
     */
    async getIntegrations(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<IntegrationModel>> {
        const response = await this.getIntegrationsRaw(initOverrides);
        return await response.value();
    }

    /**
     * Create a new integration.
     * Create a new integration.
     */
    async postIntegrationRaw(requestParameters: PostIntegrationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<IntegrationModel>> {
        if (requestParameters.integrationModel === null || requestParameters.integrationModel === undefined) {
            throw new runtime.RequiredError('integrationModel','Required parameter requestParameters.integrationModel was null or undefined when calling postIntegration.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/integrations`,
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: IntegrationModelToJSON(requestParameters.integrationModel),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => IntegrationModelFromJSON(jsonValue));
    }

    /**
     * Create a new integration.
     * Create a new integration.
     */
    async postIntegration(requestParameters: PostIntegrationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<IntegrationModel> {
        const response = await this.postIntegrationRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Creates new workflow and adds it to an existing integration.
     * Creates new workflow and adds it to an existing integration.
     */
    async postIntegrationWorkflowRaw(requestParameters: PostIntegrationWorkflowRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<IntegrationModel>> {
        if (requestParameters.id === null || requestParameters.id === undefined) {
            throw new runtime.RequiredError('id','Required parameter requestParameters.id was null or undefined when calling postIntegrationWorkflow.');
        }

        if (requestParameters.postIntegrationWorkflowRequestModel === null || requestParameters.postIntegrationWorkflowRequestModel === undefined) {
            throw new runtime.RequiredError('postIntegrationWorkflowRequestModel','Required parameter requestParameters.postIntegrationWorkflowRequestModel was null or undefined when calling postIntegrationWorkflow.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/integrations/{id}/workflows`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters.id))),
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: PostIntegrationWorkflowRequestModelToJSON(requestParameters.postIntegrationWorkflowRequestModel),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => IntegrationModelFromJSON(jsonValue));
    }

    /**
     * Creates new workflow and adds it to an existing integration.
     * Creates new workflow and adds it to an existing integration.
     */
    async postIntegrationWorkflow(requestParameters: PostIntegrationWorkflowRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<IntegrationModel> {
        const response = await this.postIntegrationWorkflowRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Update an existing integration.
     * Update an existing integration.
     */
    async putIntegrationRaw(requestParameters: PutIntegrationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<IntegrationModel>> {
        if (requestParameters.id === null || requestParameters.id === undefined) {
            throw new runtime.RequiredError('id','Required parameter requestParameters.id was null or undefined when calling putIntegration.');
        }

        if (requestParameters.integrationModel === null || requestParameters.integrationModel === undefined) {
            throw new runtime.RequiredError('integrationModel','Required parameter requestParameters.integrationModel was null or undefined when calling putIntegration.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/integrations/{id}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters.id))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: IntegrationModelToJSON(requestParameters.integrationModel),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => IntegrationModelFromJSON(jsonValue));
    }

    /**
     * Update an existing integration.
     * Update an existing integration.
     */
    async putIntegration(requestParameters: PutIntegrationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<IntegrationModel> {
        const response = await this.putIntegrationRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Updates tags of an existing integration.
     * Updates tags of an existing integration.
     */
    async putIntegrationTagsRaw(requestParameters: PutIntegrationTagsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters.id === null || requestParameters.id === undefined) {
            throw new runtime.RequiredError('id','Required parameter requestParameters.id was null or undefined when calling putIntegrationTags.');
        }

        if (requestParameters.tagModel === null || requestParameters.tagModel === undefined) {
            throw new runtime.RequiredError('tagModel','Required parameter requestParameters.tagModel was null or undefined when calling putIntegrationTags.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/integrations/{id}/tags`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters.id))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: requestParameters.tagModel.map(TagModelToJSON),
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Updates tags of an existing integration.
     * Updates tags of an existing integration.
     */
    async putIntegrationTags(requestParameters: PutIntegrationTagsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.putIntegrationTagsRaw(requestParameters, initOverrides);
    }

}
