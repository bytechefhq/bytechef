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


import * as runtime from '../runtime';
import type {
  SaveWorkflowTestConfigurationConnectionRequestModel,
  SaveWorkflowTestConfigurationInputsRequestModel,
  WorkflowTestConfigurationConnectionModel,
  WorkflowTestConfigurationModel,
} from '../models/index';
import {
    SaveWorkflowTestConfigurationConnectionRequestModelFromJSON,
    SaveWorkflowTestConfigurationConnectionRequestModelToJSON,
    SaveWorkflowTestConfigurationInputsRequestModelFromJSON,
    SaveWorkflowTestConfigurationInputsRequestModelToJSON,
    WorkflowTestConfigurationConnectionModelFromJSON,
    WorkflowTestConfigurationConnectionModelToJSON,
    WorkflowTestConfigurationModelFromJSON,
    WorkflowTestConfigurationModelToJSON,
} from '../models/index';

export interface GetWorkflowTestConfigurationRequest {
    workflowId: string;
}

export interface GetWorkflowTestConfigurationConnectionsRequest {
    workflowId: string;
    workflowNodeName: string;
}

export interface SaveWorkflowTestConfigurationRequest {
    workflowId: string;
    workflowTestConfigurationModel: Omit<WorkflowTestConfigurationModel, 'createdBy'|'createdDate'|'lastModifiedBy'|'lastModifiedDate'|'workflowId'>;
}

export interface SaveWorkflowTestConfigurationConnectionRequest {
    workflowId: string;
    workflowNodeName: string;
    workflowConnectionKey: string;
    saveWorkflowTestConfigurationConnectionRequestModel: SaveWorkflowTestConfigurationConnectionRequestModel;
}

export interface SaveWorkflowTestConfigurationInputsRequest {
    workflowId: string;
    saveWorkflowTestConfigurationInputsRequestModel: SaveWorkflowTestConfigurationInputsRequestModel;
}

/**
 * 
 */
export class WorkflowTestConfigurationApi extends runtime.BaseAPI {

    /**
     * Get a workflow test configuration.
     * Get a workflow test configuration
     */
    async getWorkflowTestConfigurationRaw(requestParameters: GetWorkflowTestConfigurationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<WorkflowTestConfigurationModel>> {
        if (requestParameters['workflowId'] == null) {
            throw new runtime.RequiredError(
                'workflowId',
                'Required parameter "workflowId" was null or undefined when calling getWorkflowTestConfiguration().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/workflow-test-configurations/{workflowId}`.replace(`{${"workflowId"}}`, encodeURIComponent(String(requestParameters['workflowId']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => WorkflowTestConfigurationModelFromJSON(jsonValue));
    }

    /**
     * Get a workflow test configuration.
     * Get a workflow test configuration
     */
    async getWorkflowTestConfiguration(requestParameters: GetWorkflowTestConfigurationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<WorkflowTestConfigurationModel> {
        const response = await this.getWorkflowTestConfigurationRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get a workflow test configuration connections.
     * Get a workflow test configuration connections
     */
    async getWorkflowTestConfigurationConnectionsRaw(requestParameters: GetWorkflowTestConfigurationConnectionsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<WorkflowTestConfigurationConnectionModel>>> {
        if (requestParameters['workflowId'] == null) {
            throw new runtime.RequiredError(
                'workflowId',
                'Required parameter "workflowId" was null or undefined when calling getWorkflowTestConfigurationConnections().'
            );
        }

        if (requestParameters['workflowNodeName'] == null) {
            throw new runtime.RequiredError(
                'workflowNodeName',
                'Required parameter "workflowNodeName" was null or undefined when calling getWorkflowTestConfigurationConnections().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/workflow-test-configurations/{workflowId}/connections/{workflowNodeName}`.replace(`{${"workflowId"}}`, encodeURIComponent(String(requestParameters['workflowId']))).replace(`{${"workflowNodeName"}}`, encodeURIComponent(String(requestParameters['workflowNodeName']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(WorkflowTestConfigurationConnectionModelFromJSON));
    }

    /**
     * Get a workflow test configuration connections.
     * Get a workflow test configuration connections
     */
    async getWorkflowTestConfigurationConnections(requestParameters: GetWorkflowTestConfigurationConnectionsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<WorkflowTestConfigurationConnectionModel>> {
        const response = await this.getWorkflowTestConfigurationConnectionsRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Create new or update an existing workflow test configuration.
     * Create new or update an existing workflow test configuration
     */
    async saveWorkflowTestConfigurationRaw(requestParameters: SaveWorkflowTestConfigurationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<WorkflowTestConfigurationModel>> {
        if (requestParameters['workflowId'] == null) {
            throw new runtime.RequiredError(
                'workflowId',
                'Required parameter "workflowId" was null or undefined when calling saveWorkflowTestConfiguration().'
            );
        }

        if (requestParameters['workflowTestConfigurationModel'] == null) {
            throw new runtime.RequiredError(
                'workflowTestConfigurationModel',
                'Required parameter "workflowTestConfigurationModel" was null or undefined when calling saveWorkflowTestConfiguration().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/workflow-test-configurations/{workflowId}`.replace(`{${"workflowId"}}`, encodeURIComponent(String(requestParameters['workflowId']))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: WorkflowTestConfigurationModelToJSON(requestParameters['workflowTestConfigurationModel']),
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => WorkflowTestConfigurationModelFromJSON(jsonValue));
    }

    /**
     * Create new or update an existing workflow test configuration.
     * Create new or update an existing workflow test configuration
     */
    async saveWorkflowTestConfiguration(requestParameters: SaveWorkflowTestConfigurationRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<WorkflowTestConfigurationModel> {
        const response = await this.saveWorkflowTestConfigurationRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Save a workflow test configuration connection.
     * Save a workflow test configuration connection
     */
    async saveWorkflowTestConfigurationConnectionRaw(requestParameters: SaveWorkflowTestConfigurationConnectionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters['workflowId'] == null) {
            throw new runtime.RequiredError(
                'workflowId',
                'Required parameter "workflowId" was null or undefined when calling saveWorkflowTestConfigurationConnection().'
            );
        }

        if (requestParameters['workflowNodeName'] == null) {
            throw new runtime.RequiredError(
                'workflowNodeName',
                'Required parameter "workflowNodeName" was null or undefined when calling saveWorkflowTestConfigurationConnection().'
            );
        }

        if (requestParameters['workflowConnectionKey'] == null) {
            throw new runtime.RequiredError(
                'workflowConnectionKey',
                'Required parameter "workflowConnectionKey" was null or undefined when calling saveWorkflowTestConfigurationConnection().'
            );
        }

        if (requestParameters['saveWorkflowTestConfigurationConnectionRequestModel'] == null) {
            throw new runtime.RequiredError(
                'saveWorkflowTestConfigurationConnectionRequestModel',
                'Required parameter "saveWorkflowTestConfigurationConnectionRequestModel" was null or undefined when calling saveWorkflowTestConfigurationConnection().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/workflow-test-configurations/{workflowId}/connections/{workflowNodeName}/{workflowConnectionKey}`.replace(`{${"workflowId"}}`, encodeURIComponent(String(requestParameters['workflowId']))).replace(`{${"workflowNodeName"}}`, encodeURIComponent(String(requestParameters['workflowNodeName']))).replace(`{${"workflowConnectionKey"}}`, encodeURIComponent(String(requestParameters['workflowConnectionKey']))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: SaveWorkflowTestConfigurationConnectionRequestModelToJSON(requestParameters['saveWorkflowTestConfigurationConnectionRequestModel']),
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Save a workflow test configuration connection.
     * Save a workflow test configuration connection
     */
    async saveWorkflowTestConfigurationConnection(requestParameters: SaveWorkflowTestConfigurationConnectionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.saveWorkflowTestConfigurationConnectionRaw(requestParameters, initOverrides);
    }

    /**
     * Save a workflow test configuration inputs.
     * Save a workflow test configuration inputs
     */
    async saveWorkflowTestConfigurationInputsRaw(requestParameters: SaveWorkflowTestConfigurationInputsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters['workflowId'] == null) {
            throw new runtime.RequiredError(
                'workflowId',
                'Required parameter "workflowId" was null or undefined when calling saveWorkflowTestConfigurationInputs().'
            );
        }

        if (requestParameters['saveWorkflowTestConfigurationInputsRequestModel'] == null) {
            throw new runtime.RequiredError(
                'saveWorkflowTestConfigurationInputsRequestModel',
                'Required parameter "saveWorkflowTestConfigurationInputsRequestModel" was null or undefined when calling saveWorkflowTestConfigurationInputs().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/workflow-test-configurations/{workflowId}/inputs`.replace(`{${"workflowId"}}`, encodeURIComponent(String(requestParameters['workflowId']))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: SaveWorkflowTestConfigurationInputsRequestModelToJSON(requestParameters['saveWorkflowTestConfigurationInputsRequestModel']),
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Save a workflow test configuration inputs.
     * Save a workflow test configuration inputs
     */
    async saveWorkflowTestConfigurationInputs(requestParameters: SaveWorkflowTestConfigurationInputsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.saveWorkflowTestConfigurationInputsRaw(requestParameters, initOverrides);
    }

}
