#!/bin/bash

echo "🔍 Populating only BLANK files (will not overwrite existing content)..."

# Helper function to safely create file with frontmatter only if it's blank
safe_create() {
  local file="$1"
  local title="$2"
  local description="$3"
  
  if [ ! -s "$file" ]; then  # -s checks if file is empty
    cat > "$file" << CONTENT
---
title: $title
description: $description
---

# $title

Coming soon...
CONTENT
    echo "✓ Created: $file"
  else
    echo "⊘ Skipped (has content): $file"
  fi
}

# Automation - Build
safe_create "automation/build/overview.md" "Build Overview" "Design and create workflows"
safe_create "automation/build/reference.md" "Build Reference" "Technical reference for building workflows"

# Automation - Deploy
safe_create "automation/deploy/overview.md" "Deploy Overview" "Deploy workflows to production"
safe_create "automation/deploy/how-to-guides.md" "Deploy How-To Guides" "Deployment guides"
safe_create "automation/deploy/reference.md" "Deploy Reference" "Deployment reference"

# Automation - Monitor
safe_create "automation/monitor/overview.md" "Monitor Overview" "Monitor and debug workflow executions"
safe_create "automation/monitor/how-to-guides.md" "Monitor How-To Guides" "Monitoring guides"
safe_create "automation/monitor/reference.md" "Monitor Reference" "Monitoring reference"

# Automation - Other
safe_create "automation/connect-data.md" "Connect Data" "Connect to third-party applications"
safe_create "automation/getting-started.md" "Getting Started" "Start with automation"
safe_create "automation/git-type.md" "Git Integration" "Configure ByteChef to read/write workflows from/to Git"

# Getting Started
safe_create "getting-started/welcome.md" "Welcome" "Welcome to ByteChef documentation"
safe_create "getting-started/introduction.md" "Introduction" "What is ByteChef and core concepts"
safe_create "getting-started/glossary.md" "Glossary" "Key concepts and terminology"
safe_create "getting-started/quick-start.md" "Quick Start" "Get up and running in minutes"

# Developer Guide
safe_create "developer-guide/index.md" "Developer Guide" "Build components and extend ByteChef"
safe_create "developer-guide/overview.md" "Developer Guide Overview" "Build components and extend ByteChef"
safe_create "developer-guide/development-environment.md" "Development Environment" "Set up your development environment"

# Developer Guide - Build Components
safe_create "developer-guide/build-components/overview.md" "Build Components Overview" "Learn to build custom components"
safe_create "developer-guide/build-components/create-component-definition.md" "Create Component Definition" "Create a component definition"
safe_create "developer-guide/build-components/create-action.md" "Create Action" "Create an action"
safe_create "developer-guide/build-components/create-trigger.md" "Create Trigger" "Create a trigger"
safe_create "developer-guide/build-components/publish.md" "Publish Component" "Publish your component"

# Developer Guide - Testing
safe_create "developer-guide/testing/overview.md" "Testing Overview" "Testing components"
safe_create "developer-guide/testing/component-specification.md" "Component Specification" "Component specification"
safe_create "developer-guide/testing/test-execution.md" "Test Execution" "Execute component tests"
safe_create "developer-guide/testing/contribute.md" "Contribute" "Contribute to ByteChef"

# Developer Guide - API
safe_create "developer-guide/api/reference.md" "API Reference" "ByteChef API reference"
safe_create "developer-guide/api/expressions.md" "Expressions" "Expression language reference"

# Embedded
safe_create "embedded/introduction.md" "Embedding ByteChef" "Embed ByteChef in your applications"
safe_create "embedded/glossary.md" "Embedded Glossary" "Embedded mode glossary"
safe_create "embedded/quick-example.md" "Quick Example" "Quick example of embedded ByteChef"

# Platform
safe_create "platform/notificationns/overview.md" "Notifications Overview" "Platform notifications overview"
safe_create "platform/notificationns/configuration.md" "Notifications Configuration" "Configure notifications"

# Reference
safe_create "reference/components.md" "Components" "Component reference"
safe_create "reference/api-specification.md" "API Specification" "API specification"
safe_create "reference/flow-controls/index.mdx" "Flow Controls" "Flow control reference"

# Self-Hosting
safe_create "self-hosting/index.md" "Self-Hosting" "Deploy ByteChef on your own infrastructure"
safe_create "self-hosting/architecture.md" "Architecture" "ByteChef architecture overview"

# Self-Hosting - Deployment
safe_create "self-hosting/deployment/local-docker.md" "Local Docker" "Run ByteChef locally with Docker"
safe_create "self-hosting/deployment/docker.md" "Docker" "Deploy ByteChef with Docker"
safe_create "self-hosting/deployment/kubernetes.md" "Kubernetes" "Deploy ByteChef on Kubernetes"
safe_create "self-hosting/deployment/aws-ecs.md" "AWS ECS" "Deploy ByteChef on AWS ECS"
safe_create "self-hosting/deployment/azure-container-instance.md" "Azure Container Instance" "Deploy ByteChef on Azure Container Instance"
safe_create "self-hosting/deployment/google-cloud-run.md" "Google Cloud Run" "Deploy ByteChef on Google Cloud Run"
safe_create "self-hosting/deployment/digitalocean.md" "DigitalOcean" "Deploy ByteChef on DigitalOcean"

# Self-Hosting - Configuration
safe_create "self-hosting/configuration/configure-instance.md" "Configure Instance" "Configure your ByteChef instance"
safe_create "self-hosting/configuration/environment-variables.md" "Environment Variables" "ByteChef environment configuration"
safe_create "self-hosting/configuration/manage-instance.md" "Manage Instance" "Manage your ByteChef instance"

echo ""
echo "✅ Done! Only blank files were populated."
echo "📝 Files with existing content were left untouched."
