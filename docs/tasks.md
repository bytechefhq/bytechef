### Core Component Tools

[] **`listComponents`** - List all components with filtering options
[] **`getComponentInfo`** - Get comprehensive information about a specific component
[] **`getActionInfo`** - Get comprehensive information about a specific action
[] **`getTriggerInfo`** - Get comprehensive information about a specific trigger
[] **`getClusterElementInfo`** - Get comprehensive information about a specific cluster element
[] **`searchComponents`** - Full-text search across all components documentation
[] **`searchActionProperties`** - Find specific properties within actions
[] **`searchTriggerProperties`** - Find specific properties within triggers
[] **`searchClusterElementProperties`** - Find specific properties within cluster elements

### Core Task Dispatcher Tools

[] **`listTaskDispatchers`** - List all task dispatchers with filtering options
[] **`getTaskDispatcherInfo`** - Get comprehensive information about a specific task dispatcher
[] **`searchTaskDispatchers`** - Full-text search across all task dispatcher documentation
[] **`searchTaskDispatcherProperties`** - Find specific properties within task dispatchers

### Advanced Tools
[] **`get_node_for_task`** - Pre-configured node settings for common tasks
[] **`list_tasks`** - Discover available task templates
[] **`validate_node_operation`** - Validate node configurations (operation-aware, profiles support)
[] **`validate_node_minimal`** - Quick validation for just required fields
[] **`validate_workflow`** - Complete workflow validation including AI tool connections
[] **`validate_workflow_connections`** - Check workflow structure and AI tool connections
[] **`validate_workflow_expressions`** - Validate n8n expressions including $fromAI()
[] **`get_property_dependencies`** - Analyze property visibility conditions
[] **`get_node_documentation`** - Get parsed documentation from n8n-docs
[] **`get_database_statistics`** - View database metrics and coverage

## Project Management

[x] **`listProjects`** - List all projects in ByteChef with basic information
[x] **`getProject`** - Get comprehensive information about a specific project
[x] **`createProject`** - Create a new project with workflows
[x] **`updateProject`** - Update project settings and metadata
[x] **`deleteProject`** - Delete a project and all its workflows
[x] **`searchProjects`** - Full-text search across all projects
[x] **`getProjectStatus`** - Get project deployment and execution status
[x] **`publishProject`** - Publish a project version for deployment

## Project Workflow Management

[x] **`createProjectWorkflow`** - Create a new workflow in project. Returns the created workflow information including id, project id, workflow id, and reference code

### n8n Management Tools (Optional - Requires API Configuration)
These powerful tools allow you to manage n8n workflows directly from Claude. They're only available when you provide `N8N_API_URL` and `N8N_API_KEY` in your configuration.

#### Workflow Management
[] **`n8n_create_workflow`** - Create new workflows with nodes and connections
[] **`n8n_get_workflow`** - Get complete workflow by ID
[] **`n8n_get_workflow_details`** - Get workflow with execution statistics
[] **`n8n_get_workflow_structure`** - Get simplified workflow structure
[] **`n8n_get_workflow_minimal`** - Get minimal workflow info (ID, name, active status)
[] **`n8n_update_full_workflow`** - Update entire workflow (complete replacement)
[] **`n8n_update_partial_workflow`** - Update workflow using diff operations (NEW in v2.7.0!)
[] **`n8n_delete_workflow`** - Delete workflows permanently
[] **`n8n_list_workflows`** - List workflows with filtering and pagination
[] **`n8n_validate_workflow`** - Validate workflows already in n8n by ID (NEW in v2.6.3)

#### Execution Management
[] **`n8n_trigger_webhook_workflow`** - Trigger workflows via webhook URL
[] **`n8n_get_execution`** - Get execution details by ID
[] **`n8n_list_executions`** - List executions with status filtering
[] **`n8n_delete_execution`** - Delete execution records

### System Tools

[//]: # ([] **`healthCheck`** - Check ByteChef API connectivity and features)

[//]: # ([] **`diagnostic`** - Troubleshoot management tools visibility and configuration issues)

[//]: # ([] **`listAvailableTools`** - List all available management tools)
