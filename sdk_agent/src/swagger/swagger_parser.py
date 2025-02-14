import yaml
import json
import os
import logging
from crewai import Agent, Crew, Process, Task
from crewai.project import CrewBase, agent, crew, task

# Setup logging
logging.basicConfig(level=logging.DEBUG, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

# Get the absolute path of the current file
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
SWAGGER_FILE = os.path.join(BASE_DIR, "config", "syncro.json")  # Path to Swagger JSON

def load_yaml_config(file_path):
    """Load YAML config files dynamically and handle exceptions."""
    if not os.path.exists(file_path):
        logger.error(f"❌ ERROR: File not found - {file_path}")
        return {}

    try:
        with open(file_path, "r") as file:
            data = yaml.safe_load(file)
            if data is None:
                logger.error(f"❌ ERROR: YAML file is empty - {file_path}")
                return {}
            return data
    except yaml.YAMLError as e:
        logger.error(f"❌ ERROR: YAML Parsing Error in {file_path} - {e}")
        return {}

def load_swagger_json(file_path=SWAGGER_FILE):
    """Load Swagger JSON and extract API details."""
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"Swagger JSON file not found at: {file_path}")

    with open(file_path, "r", encoding="utf-8") as file:
        swagger_data = json.load(file)

    api_details = []

    for path, methods in swagger_data.get("paths", {}).items():
        for method, details in methods.items():
            api_details.append({
                "method": method.upper(),
                "path": path,
                "operation_id": details.get("operationId", f"{method.upper()} {path}"),
                "summary": details.get("summary", "No description provided"),
                "parameters": details.get("parameters", []),
                "responses": details.get("responses", {})
            })

    return api_details

# ✅ Load Configurations
agents_config_file = os.path.join(BASE_DIR, "config", "agents.yaml")
tasks_config_file = os.path.join(BASE_DIR, "config", "tasks.yaml")

agents_config = load_yaml_config(agents_config_file) or {}
tasks_config = load_yaml_config(tasks_config_file) or {}

# ✅ Load API Details from Swagger JSON
api_details = load_swagger_json()
logger.info(f"✅ Loaded {len(api_details)} API endpoints from Swagger JSON.")

@CrewBase
class SdkAgent():
    """SdkAgent crew for generating Java SDK with Freshdesk Component DSL compliance"""

    # ✅ Define Agents
    @agent
    def api_researcher(self) -> Agent:
        return Agent(
            config=agents_config.get("api_researcher", {}),
            verbose=True
        )

    @agent
    def sdk_generator(self) -> Agent:
        return Agent(
            config=agents_config.get("sdk_generator", {}),
            verbose=True
        )

    @agent
    def dsl_validator(self) -> Agent:
        return Agent(
            config=agents_config.get("dsl_validator", {}),
            verbose=True
        )

    # ✅ Define Tasks
    @task
    def extract_api_task(self) -> Task:
        """Extract API details from Swagger JSON and pass to SDK generator."""
        return Task(
            config=tasks_config.get("extract_api_task", {}),
            description="Extract API details from Swagger JSON.",
            expected_output="A structured list of API endpoints and parameters.",
            input_data=api_details  # Pass Swagger API details to task
        )

#     @task
#     def generate_connection_task(self) -> Task:
#         return Task(
#             config=tasks_config.get("generate_connection_task", {}),
#             description="Generate API connection class using Freshdesk Component DSL.",
#             expected_output="A Java class that handles authentication and connection.",
#             context=self.extract_api_task  # Runs AFTER API details are extracted
#         )
#
#     @task
#     def generate_actions_task(self) -> Task:
#         return Task(
#             config=tasks_config.get("generate_actions_task", {}),
#             description="Generate API action classes using Freshdesk Component DSL.",
#             expected_output="Java classes mapping API endpoints to Component DSL actions.",
#             context=self.extract_api_task  # Runs AFTER API details are extracted
#         )
#
#     @task
#     def validate_dsl_task(self) -> Task:
#         return Task(
#             config=tasks_config.get("validate_dsl_task", {}),
#             description="Validate SDK structure for Freshdesk Component DSL compliance.",
#             expected_output="A report confirming compliance or listing errors.",
#             context=[self.generate_connection_task, self.generate_actions_task]  # Runs AFTER SDK is generated
#         )

    # ✅ Define Crew & Execution Flow
    @crew
    def crew(self) -> Crew:
        """Creates the SDK Agent crew"""
        return Crew(
            agents=self.agents,
            tasks=self.tasks,
            process=Process.hierarchical,  # Ensures structured execution
            verbose=True,
        )
