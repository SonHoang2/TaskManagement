#!/usr/bin/env bash
# Usage: ./k8s/build-and-deploy.sh [kind|minikube]   (run from Backend/)
set -euo pipefail
cd "$(dirname "$0")/.."
RUNTIME=${1:-kind}
SERVICES="api-gateway user-service project-service task-service sprint-service notification-service dashboard-service"

for s in $SERVICES; do
  case $s in
    # these depend on common-lib, so their Dockerfiles expect Backend/ as build context
    user-service|project-service|task-service|sprint-service|notification-service)
      docker build -t "taskmgmt/$s:latest" -f "./$s/Dockerfile" . ;;
    *) docker build -t "taskmgmt/$s:latest" "./$s" ;;
  esac
  case $RUNTIME in
    kind) kind load docker-image "taskmgmt/$s:latest" --name "${KIND_CLUSTER_NAME:-taskmgmt}" ;;
    minikube) minikube image load "taskmgmt/$s:latest" ;;
  esac
done

kubectl apply -f k8s/00-config.yaml
kubectl create configmap postgres-init -n taskmgmt --from-file=init-db.sql --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f k8s/secret.yaml -f k8s/infra.yaml -f k8s/apps.yaml
kubectl apply -f k8s/pdb.yaml
# HPA/Ingress need metrics-server / ingress-nginx (README); skip quietly if the CRDs/controller are missing
kubectl apply -f k8s/hpa.yaml -f k8s/ingress.yaml || echo 'WARN: hpa/ingress not applied, see k8s/README.md'
echo "Gateway: kubectl port-forward -n taskmgmt svc/api-gateway 8765:80"
