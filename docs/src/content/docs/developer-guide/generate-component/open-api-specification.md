---
title: "OpenAPI Specification"
---

1. Create OpenAPI Specification:
    - Inside your new package, create an `openapi.yaml` file.
    - Write the [OpenAPI specification](https://swagger.io/specification/) for your component to define its API structure and endpoints.

2. Navigate to CLI Directory:
    - Change your working directory to the `BYTECHEF_HOME/cli/cli-app` folder.

3. Generate Component:
    - Execute the following command to generate the `NewComponent` in the `newcomponent` directory:
      ```bash
      ./bytechef.sh component init --open-api-path ../../server/libs/modules/components/newcomponent/openapi.yaml --output-path ../../server/libs/modules/components --name newcomponent
      ```
    - This command initializes the component based on the OpenAPI specification, placing the generated files in the specified output path.  
