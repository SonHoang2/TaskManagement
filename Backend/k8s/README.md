# Running TaskManagement on Kubernetes (kind)

[Tiếng Việt](README.vi.md) | **English**

## 0. Concepts to know

| Term | Meaning | In this project |
|---|---|---|
| Cluster | A group of machines running k8s | `kind` creates the `taskmgmt` cluster inside one Docker container |
| Node | One machine in the cluster | 1 node: `taskmgmt-control-plane` |
| Namespace | A separate compartment grouping resources | `taskmgmt` (always add `-n taskmgmt`) |
| Pod | The smallest unit, holds 1 container | each Spring Boot service = 1 pod |
| Deployment | Declares "run N pods of image X", recreates them when they die | `user-service`, `api-gateway`, ... |
| StatefulSet | Like a Deployment but for things that hold data | `postgres` |
| Service | DNS name + load balancing for pods | `http://user-service` inside the cluster = replaces Eureka |
| ConfigMap / Secret | Plain / sensitive configuration, injected into pods as env | `app-config`, `app-secrets` |
| PVC | A pod's disk | Postgres data |

Request flow: `browser -> port-forward :8765 -> api-gateway Service -> api-gateway pod -> user-service Service -> user-service pod`.

Layout of the `k8s/` folder:
- `00-config.yaml`: namespace
- `secret.yaml` (create it yourself from `secret.example.yaml`, git-ignored): passwords, JWT
- `infra.yaml`: postgres, redis, rabbitmq
- `helm/taskmgmt/`: Helm chart for the apps: ConfigMap, the 7 Spring Boot services (RollingUpdate, Actuator probes, graceful shutdown), CPU-based HPA 2–4 pods, PDB `minAvailable: 1`, Ingress `http://taskmgmt.local` -> api-gateway. All knobs live in `values.yaml`
- `helm-adopt.sh`: one-time script that hands resources created earlier by `kubectl apply` over to Helm (no downtime)
- `observability.yaml` + `deploy-observability.sh`: Prometheus, Alertmanager, Grafana (section 12)
- `build-and-deploy.sh`: build images + load into kind + apply

## 1. Installation (once)

```bash
sudo snap install kubectl --classic
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.24.0/kind-linux-amd64 && chmod +x ./kind && sudo mv ./kind /usr/local/bin/kind
```

Docker Desktop -> Settings -> Resources: give it **6-8GB of Memory** (the default ~2GB is not enough; pods will stay `Pending`).
Check:
```bash
kubectl describe node | grep -A1 "memory:" | head -3
```

## 2. Create the cluster (once)

```bash
cat <<EOF | kind create cluster --name taskmgmt --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  kubeadmConfigPatches:
  - |
    kind: InitConfiguration
    nodeRegistration:
      kubeletExtraArgs:
        node-labels: "ingress-ready=true"
  extraPortMappings:
  - {containerPort: 80, hostPort: 80, protocol: TCP}
EOF
kubectl get nodes          # you should see 1 Ready node
kind get clusters          # list clusters
```

### 2b. Addons for Ingress and HPA (once)

```bash
# Ingress controller
kubectl apply -f https://kind.sigs.k8s.io/examples/ingress/deploy-ingress-nginx.yaml
# metrics-server (kind needs --kubelet-insecure-tls)
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
kubectl patch deploy metrics-server -n kube-system --type=json -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
echo "127.0.0.1 taskmgmt.local" | sudo tee -a /etc/hosts
```

## 3. Configure secrets

```bash
cd ~/Desktop/code/TaskManagementAPI/Backend
cp k8s/secret.example.yaml k8s/secret.yaml
nano k8s/secret.yaml
```
- `JWT_SECRET`: **at least 32 characters** (quick way to generate: `openssl rand -base64 48`)
- `DB_PASSWORD`: set it before running Postgres for the first time. Postgres only reads the password when the volume is initialised; changing it later requires deleting the volume (see section 9).

## 4. Build and deploy

```bash
./k8s/build-and-deploy.sh kind
```
This script: builds the 7 Docker images -> `kind load` into the cluster -> applies config, secret, infra, apps.

Rebuild a single service after changing code (e.g. user-service):
```bash
docker build -t taskmgmt/user-service:latest -f user-service/Dockerfile .   # services using common-lib: the context is Backend/
# api-gateway, dashboard-service: docker build -t taskmgmt/api-gateway:latest ./api-gateway
kind load docker-image taskmgmt/user-service:latest --name taskmgmt
kubectl rollout restart deploy/user-service -n taskmgmt
```
(You must `rollout restart` because the `latest` tag does not change, so k8s cannot tell there is a new image.)

## 5. Check status

```bash
kubectl get pods -n taskmgmt            # one-off view
kubectl get pods -n taskmgmt -w         # watch continuously (Ctrl+C to quit)
kubectl get all -n taskmgmt             # pods + services + deployments
```
How to read the columns:
- `READY 1/1` = ready to take requests; `0/1` = running but not ready yet
- `STATUS Running` is good; `Pending` not scheduled (usually not enough RAM/CPU); `CrashLoopBackOff` the app keeps dying; `ImagePullBackOff`/`ErrImagePull` image not found (you forgot `kind load`)
- `RESTARTS` keeps growing = something is wrong

Spring Boot starts slowly: wait 1-3 minutes; 7 JVMs starting at once is heavy on the machine.

## 6. Access the application

Via Ingress (after step 2b): `curl http://taskmgmt.local/actuator/health`; the frontend calls `http://taskmgmt.local`.

Or port-forward directly:

```bash
kubectl port-forward -n taskmgmt svc/api-gateway 8765:80
```
Leave that terminal running. The frontend calls `http://localhost:8765`.

Open the RabbitMQ UI (optional):
```bash
kubectl port-forward -n taskmgmt svc/rabbitmq 15672:15672
```
Connect to the DB with an external tool:
```bash
kubectl port-forward -n taskmgmt svc/postgres 5433:5432
```

## 7. Debugging (most important)

Follow this order:
```bash
kubectl get pods -n taskmgmt                              # 1. which pod is failing
kubectl describe pod -n taskmgmt <pod-name>               # 2. read the Events section at the bottom
kubectl logs -n taskmgmt deploy/user-service --tail=100   # 3. app logs
kubectl logs -n taskmgmt deploy/user-service --previous   # 4. logs of the previous run (when it is crashing)
kubectl logs -n taskmgmt deploy/user-service -f           # follow logs live
```
Quickly filter for the Spring root cause:
```bash
kubectl logs -n taskmgmt deploy/user-service --previous | grep -E "Caused by|APPLICATION FAILED|Action"
```
Get inside a pod:
```bash
kubectl exec -it -n taskmgmt postgres-0 -- psql -U postgres
kubectl exec -it -n taskmgmt deploy/user-service -- sh
```
Check the env the pod actually received:
```bash
kubectl exec -n taskmgmt deploy/user-service -- env | grep -E "DB_|JWT|RABBIT"
```

Common problems:

| Symptom | Cause | Fix |
|---|---|---|
| `Pending`, Events: `Insufficient memory` | Docker Desktop has too little RAM | increase RAM (section 1) or turn off some services (section 8) |
| `ImagePullBackOff` | image was not `kind load`ed | rerun section 4 |
| `WeakKeyException ... bits` | `JWT_SECRET` is shorter than 32 characters | fix the secret, apply, restart |
| `password authentication failed` | DB password differs from the one used when the volume was initialised | delete the Postgres volume (section 9) |
| `kubectl` timeouts / machine freezes | overloaded | run fewer services, add RAM |

## 8. Day-to-day operations

Apply YAML changes:
```bash
kubectl apply -f k8s/00-config.yaml -f k8s/infra.yaml
helm upgrade --install taskmgmt k8s/helm/taskmgmt -n taskmgmt   # apps, config, HPA, PDB, Ingress
```
After editing a secret/configmap you **must restart the pods** for them to pick up the new values:
```bash
kubectl apply -f k8s/secret.yaml
kubectl rollout restart deploy -n taskmgmt api-gateway user-service project-service task-service
```
Turn services on/off (save resources). By default `sprint`, `notification` and `dashboard` are off:
```bash
kubectl scale deploy sprint-service notification-service dashboard-service -n taskmgmt --replicas=1   # on
kubectl scale deploy sprint-service notification-service dashboard-service -n taskmgmt --replicas=0   # off
```
Turn everything off (data is kept):
```bash
kubectl scale deploy --all -n taskmgmt --replicas=0
```
Bring the 4 core services + infrastructure back:
```bash
kubectl scale deploy rabbitmq redis api-gateway user-service project-service task-service -n taskmgmt --replicas=1
```
View resource usage (kind has no `kubectl top`):
```bash
docker stats --no-stream taskmgmt-control-plane
kubectl describe node | sed -n '/Allocated resources/,/Events/p'
```

### Verify rolling update / HPA

```bash
kubectl get hpa -n taskmgmt -w                           # TARGETS must show a % instead of <unknown>
kubectl rollout restart deploy/user-service -n taskmgmt
kubectl rollout status deploy/user-service -n taskmgmt   # the new pod becomes Ready before the old one stops: no downtime
# generate load to watch it scale up:
kubectl run load -n taskmgmt --rm -it --image=busybox -- sh -c 'while true; do wget -q -O- http://api-gateway/actuator/health; done'
```
Note: sprint/notification/dashboard are still `replicas: 0` (to save RAM), so they have no HPA yet.

## 9. Cleanup / reset

Reset the database (DATA IS LOST in k8s):
```bash
kubectl delete statefulset postgres -n taskmgmt
kubectl delete pvc -n taskmgmt data-postgres-0
kubectl apply -f k8s/infra.yaml
```
Remove all apps, keep the cluster:
```bash
kubectl delete namespace taskmgmt
```
Pause the cluster to free up the machine (data is kept) and start it again:
```bash
docker stop taskmgmt-control-plane
docker start taskmgmt-control-plane
```
Delete the cluster entirely:
```bash
kind delete cluster --name taskmgmt
```

## 10. Adding a new service

1. Write a Dockerfile; add one line under `services:` in `helm/taskmgmt/values.yaml` (name, `port`, and `db`/`jwt`/`rabbit` if needed).
2. The k8s Service exposes port 80 and forwards to the container port, so other services call `http://<service-name>`.
3. To route it through the gateway: add a route in `api-gateway/src/main/resources/application.properties` and a `<NAME>_SERVICE_URL` variable under `config:` in `values.yaml`.
4. To call it from another service with Feign: `@FeignClient(name = "x", url = "${services.x.url:http://x}")`.

## 11. Running locally without k8s

Use environment variables pointing at localhost, e.g. `USER_SERVICE_URL=http://localhost:5001` for the gateway and `SERVICES_TASK_SERVICE_URL=http://localhost:5003` for Feign. Or use `docker compose up` (these variables are already set there).

## 12. Monitoring: Prometheus, Alertmanager, Grafana

Data flow:
```
Spring Boot (Micrometer) --/actuator/prometheus:9090--> Prometheus --rules--> Alertmanager --> (Slack/webhook)
                                                            \--> Grafana (dashboard)
```
- **Micrometer** measures requests, JVM and DB pool out of the box; `micrometer-registry-prometheus` exposes them in the text format Prometheus reads.
- **A dedicated port 9090** for Actuator (`MANAGEMENT_SERVER_PORT`): it does not go through the Service/Ingress, so outside users cannot read the metrics; the probes use this port too.
- **Prometheus discovers pods itself** via the `prometheus.io/scrape: "true"` annotation (set in the chart template) and scrapes each pod, so every HPA replica gets its own series. The `service` label comes from the pod's `app` label.
- **RED** = Rate (req/s), Errors (5xx ratio), Duration (p95/p99): the three core metrics of a service. The `TaskManagement - RED & JVM` dashboard also has a USE section for JVM, CPU and DB pool.
- **Alerts** (`observability/prometheus/rules/taskmgmt.rules.yml`): `TargetDown`, `ApiGatewayNoInstances`, `FrequentRestarts`, `HighErrorBudgetBurnFast/Slow` (99.5% SLO, alerting on error-budget burn rate), `HighLatencyP95`, `JvmHeapHigh`, `HighCpuUsage`, `DbConnectionPoolSaturated`. Alertmanager groups and de-duplicates alerts, and lets a `critical` alert inhibit `warning` alerts of the same service.

Deploy (already run automatically by `build-and-deploy.sh` and the CD workflow):
```bash
./k8s/deploy-observability.sh
```
Open the UIs (run each command in its own terminal):
```bash
kubectl port-forward -n taskmgmt svc/grafana 3000:80          # http://localhost:3000  (admin/admin, change on first login)
kubectl port-forward -n taskmgmt svc/prometheus 9090:9090     # Status > Targets, Alerts
kubectl port-forward -n taskmgmt svc/alertmanager 9093:9093
```
Set the Grafana password (optional): `kubectl create secret generic grafana-admin -n taskmgmt --from-literal=password=... && kubectl rollout restart deploy/grafana -n taskmgmt`.

Try an alert:
```bash
kubectl scale deploy/user-service -n taskmgmt --replicas=0   # ~2 minutes later: TargetDown (remember the HPA will bring it back; delete the user-service hpa temporarily if needed)
k6 run load-tests/task-service/create-task-load-test.js       # watch Rate/Duration on the dashboard
```
To change rules or dashboards: edit the files in `observability/` and rerun `deploy-observability.sh` (pods restart automatically thanks to a checksum). With docker compose use `docker compose up -d prometheus alertmanager grafana` (Prometheus: `localhost:9090`, Alertmanager: `localhost:9093`).

Note: Prometheus data is kept for 7 days/1GB on the `prometheus-data` PVC; Loki is not on k8s yet, so Grafana on k8s has no logs (compose does).
