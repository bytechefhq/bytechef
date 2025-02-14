import yaml
import os
import pdb
from crewai import Agent, Crew, Process, Task
from crewai.project import CrewBase, agent, crew, task

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

@CrewBase
class SdkAgent():
    """SdkAgent crew for generating Java SDK with Freshdesk Component DSL compliance"""

    agents_config_file = os.path.join(BASE_DIR, "config", "agents.yaml")
    tasks_config_file = os.path.join(BASE_DIR, "config", "tasks.yaml")

    @agent
    def api_researcher(self) -> Agent:
        pdb.set_trace()
        return Agent(
            config=self.agents_config['api_researcher'],
            verbose=True
        )

    @agent
    def sdk_generator(self) -> Agent:
        pdb.set_trace()
        return Agent(
            config=self.agents_config['sdk_generator'],
            verbose=True
        )

    @agent
    def dsl_validator(self) -> Agent:
        pdb.set_trace()
        return Agent(
            config=self.agents_config['dsl_validator'],
            verbose=True
        )

    # ✅ Define Tasks
    @task
    def extract_api_task(self) -> Task:
     """Extract API details from Swagger JSON."""
     task_config = self.tasks_config.get("extract_api_task", {})
     pdb.set_trace()
     return Task(
            config=self.tasks_config['extract_api_task'],
            agent=self.api_researcher()
        )

    @task
    def generate_connection_task(self) -> Task:
        return Task(
            config=self.tasks_config.get("generate_connection_task", {}),
        )

    @task
    def generate_actions_task(self) -> Task:
        return Task(
            config=self.tasks_config.get("generate_actions_task", {}),
        )

    @task
    def validate_dsl_task(self) -> Task:
        return Task(
            config=self.tasks_config.get("validate_dsl_task", {}),
        )

    # ✅ Define Crew & Execution Flow
    @crew
    def crew(self) -> Crew:
        """Creates the SDK Agent crew"""
        return Crew(
            agents=self.agents,
            tasks=self.tasks,
            process=Process.hierarchical,  # Structured execution
            verbose=True,
        )
