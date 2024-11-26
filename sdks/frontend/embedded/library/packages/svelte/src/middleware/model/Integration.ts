import { Workflow } from "./Workflow";
export interface Integration {
  id: number;
  title: string;
  description: string;
  icon: string;
  workflows: Workflow[];
}