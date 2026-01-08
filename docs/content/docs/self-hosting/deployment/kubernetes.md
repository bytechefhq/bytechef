---
<<<<<<< HEAD
description: "ByteChef Kubernetes Deployment. Learn how to deploy and manage ByteChef clusters on Kubernetes platforms."
title: Kubernetes
---

Using [Kubernetes](https://kubernetes.io) (K8s) is a great way to run ByteChef in production self-hosted environments. It delivers automatic scaling, self-healing capabilities, and streamlines database and configuration handling.

While ByteChef also supports [Helm](https://helm.sh/) for simplified deployments, we recommend using Kubernetes for production environments. Refer to the [Helm chart values](https://github.com/bytechefhq/bytechef/blob/master/kubernetes/helm/bytechef-monolith/values.yaml) in the ByteChef GitHub repository.


### Prerequisites
Ensure Kubernetes and Helm are installed on your system:

- **kubectl** - Kubernetes command-line tool.
- **Helm 3.x** - Package manager for Kubernetes.
- **Kubernetes Cluster** - A running cluster (Docker Desktop, Minikube, or cloud providers like EKS, GKE, AKS).

### Getting Started with ByteChef

- To begin, run these commands in your terminal:

```bash
git clone https://github.com/bytechefhq/bytechef.git
cd bytechef/kubernetes/helm/bytechef-monolith
```
- Create a namespace for ByteChef using Kubernetes Cluster:
```bash
kubectl create namespace bytechef
```

- Configure PostgreSQL and ByteChef using `cURL` via the `quixkstart.yaml` file from GitHub:
```bash
curl -O https://raw.githubusercontent.com/bytechefhq/bytechef/master/kubernetes/quickstart.yaml

kubectl apply -f quickstart.yaml
```
- Deploy ByteChef using Helm:
```
helm install bytechef . --namespace bytechef --wait
```

Here's a list of the thinngs we've achieved with the commands above:

- Created a ByteChef namespace for resource isolation.
- Deployed a PostgreSQL instance for data persistence.
- Attached persistent storage to PostgreSQL so data remains intact after pod restarts.
- Setup default database configuration automatically.

After deployment completes, access ByteChef by port-forwarding:

```bash
kubectl port-forward -n bytechef svc/bytechef 8080:8080
```
Access ByteChef via http://localhost:8080

### PostgreSQL Configuration
ByteChef stores all credentials, workflows, and run history in PostgreSQL even via Docker. The quick start includes PostgreSQL automatically.

> **Warning**
> Ensure the persistent volume is properly configured. Loss of this volume will result in data loss. The persistent volume preserves your workflows, credentials, and execution records between pod restarts.

To connect to an external PostgreSQL server, update your values.yaml:
```yaml
env:
  secret:
    BYTECHEF_DATASOURCE_URL: "jdbc:postgresql://your-host:5432/bytechef"
    BYTECHEF_DATASOURCE_USERNAME: "postgres"
    BYTECHEF_DATASOURCE_PASSWORD: "your_password"
```

### Upgrading ByteChef

To deploy a newer version, update the image tag and relaunch using this command:

```bash
cd /path/to/bytechef/kubernetes/helm/bytechef-monolith
```

After doing that, you need to Fetch the current version:
```
docker pull docker.bytechef.io/bytechef/bytechef:latest
```

(Optional) If you want to fetch a specific release, run this command:
```
docker pull docker.bytechef.io/bytechef/bytechef:1.20.0
```

Update the image tag in `values.yaml`, then upgrade with Helm:
```
helm upgrade bytechef . --namespace bytechef --wait
```

### Upgrading with Helm
When managing ByteChef via Helm, go to your helm chart directory:
```bash
cd /path/to/bytechef/kubernetes/helm/bytechef-monolith
```

Update image tag in `values.yaml`, then upgrade:
```bash
helm upgrade bytechef . --namespace bytechef --wait
```

Or set the tag inline:
```bash
helm upgrade bytechef . \
  --namespace bytechef \
  --set image.tag=1.20.0 \
  --wait
```

### Next steps

- [Review ByteChef configuration options](/self-hosting/deployment/kubernetes)
- [Check out our Docker setup](/self-hosting/deployment/local-docker)
- [Build your initial automation](/automation/quick-start/configure-workflow-trigger)
=======
description: "ByteChef Kubernetes Deployment. Deploy and manage ByteChef clusters on Kubernetes platforms."
title: Kubernetes
---

Coming soon...
>>>>>>> 002576c3f5 (docs: Update Docker deployment documentation and clean up files)
