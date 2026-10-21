# Chạy TaskManagement trên Kubernetes (kind)

**Tiếng Việt** | [English](README.md)

## 0. Khái niệm cần nhớ

| Thuật ngữ | Ý nghĩa | Trong project này |
|---|---|---|
| Cluster | Một cụm máy chạy k8s | `kind` tạo cluster `taskmgmt` bên trong 1 container Docker |
| Node | Một máy trong cluster | 1 node: `taskmgmt-control-plane` |
| Namespace | Ngăn riêng để gom tài nguyên | `taskmgmt` (luôn thêm `-n taskmgmt`) |
| Pod | Đơn vị nhỏ nhất, chứa 1 container | mỗi service Spring Boot = 1 pod |
| Deployment | Mô tả "muốn chạy N pod của image X", tự tạo lại khi pod chết | `user-service`, `api-gateway`, ... |
| StatefulSet | Như Deployment nhưng cho thứ có dữ liệu | `postgres` |
| Service | Tên DNS + cân bằng tải cho các pod | `http://user-service` trong cluster = thay Eureka |
| ConfigMap / Secret | Cấu hình thường / cấu hình nhạy cảm, tiêm vào pod bằng env | `app-config`, `app-secrets` |
| PVC | Ổ đĩa của pod | dữ liệu Postgres |

Luồng gọi: `trình duyệt -> port-forward :8765 -> Service api-gateway -> pod api-gateway -> Service user-service -> pod user-service`.

Cấu trúc thư mục `k8s/`:
- `00-config.yaml`: namespace
- `secret.yaml` (tự tạo từ `secret.example.yaml`, đã gitignore): mật khẩu, JWT
- `infra.yaml`: postgres, redis, rabbitmq
- `helm/taskmgmt/`: Helm chart cho các app: ConfigMap, 7 service Spring Boot (RollingUpdate, probe Actuator, graceful shutdown), HPA 2–4 pod theo CPU, PDB `minAvailable: 1`, Ingress `http://taskmgmt.local` -> api-gateway. Mọi thông số nằm trong `values.yaml`
- `helm-adopt.sh`: chạy một lần để Helm nhận các resource đã tạo bằng `kubectl apply` (không downtime)
- `observability.yaml` + `deploy-observability.sh`: Prometheus, Alertmanager, Grafana (mục 12)
- `build-and-deploy.sh`: build image + nạp vào kind + apply

## 1. Cài đặt (một lần)

```bash
sudo snap install kubectl --classic
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.24.0/kind-linux-amd64 && chmod +x ./kind && sudo mv ./kind /usr/local/bin/kind
```

Docker Desktop -> Settings -> Resources: cấp **Memory 6-8GB** (mặc định ~2GB là không đủ, pod sẽ `Pending`).
Kiểm tra:
```bash
kubectl describe node | grep -A1 "memory:" | head -3
```

## 2. Tạo cluster (một lần)

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
kubectl get nodes          # phải thấy 1 node Ready
kind get clusters          # liệt kê cluster
```

### 2b. Addon cho Ingress và HPA (một lần)

```bash
# Ingress controller
kubectl apply -f https://kind.sigs.k8s.io/examples/ingress/deploy-ingress-nginx.yaml
# metrics-server (kind cần --kubelet-insecure-tls)
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
kubectl patch deploy metrics-server -n kube-system --type=json -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
echo "127.0.0.1 taskmgmt.local" | sudo tee -a /etc/hosts
```

## 3. Cấu hình secret

```bash
cd ~/Desktop/code/TaskManagementAPI/Backend
cp k8s/secret.example.yaml k8s/secret.yaml
nano k8s/secret.yaml
```
- `JWT_SECRET`: **tối thiểu 32 ký tự** (sinh nhanh: `openssl rand -base64 48`)
- `DB_PASSWORD`: đặt trước khi chạy Postgres lần đầu. Postgres chỉ đọc mật khẩu lúc khởi tạo volume; đổi sau đó phải xóa volume (xem mục 9).

## 4. Build và deploy

```bash
./k8s/build-and-deploy.sh kind
```
Script này: build 7 image Docker -> `kind load` vào cluster -> apply config, secret, infra, apps.

Chỉ build lại 1 service sau khi sửa code (ví dụ user-service):
```bash
docker build -t taskmgmt/user-service:latest -f user-service/Dockerfile .   # service dùng common-lib: context là Backend/
# api-gateway, dashboard-service: docker build -t taskmgmt/api-gateway:latest ./api-gateway
kind load docker-image taskmgmt/user-service:latest --name taskmgmt
kubectl rollout restart deploy/user-service -n taskmgmt
```
(Phải `rollout restart` vì tag `latest` không đổi nên k8s không tự biết có image mới.)

## 5. Theo dõi trạng thái

```bash
kubectl get pods -n taskmgmt            # xem 1 lần
kubectl get pods -n taskmgmt -w         # theo dõi liên tục (Ctrl+C thoát)
kubectl get all -n taskmgmt             # pod + service + deployment
```
Cách đọc cột:
- `READY 1/1` = sẵn sàng nhận request; `0/1` = đang chạy nhưng chưa sẵn sàng
- `STATUS Running` tốt; `Pending` chưa được xếp lịch (thường thiếu RAM/CPU); `CrashLoopBackOff` app chết liên tục; `ImagePullBackOff`/`ErrImagePull` không có image (quên `kind load`)
- `RESTARTS` tăng liên tục = có lỗi

Spring Boot khởi động chậm: chờ 1-3 phút, 7 JVM cùng lên sẽ nặng máy.

## 6. Truy cập ứng dụng

Qua Ingress (sau bước 2b): `curl http://taskmgmt.local/actuator/health`, frontend gọi `http://taskmgmt.local`.

Hoặc port-forward trực tiếp:

```bash
kubectl port-forward -n taskmgmt svc/api-gateway 8765:80
```
Để terminal đó chạy. Frontend gọi `http://localhost:8765`.

Xem RabbitMQ UI (tùy chọn):
```bash
kubectl port-forward -n taskmgmt svc/rabbitmq 15672:15672
```
Kết nối DB bằng công cụ ngoài:
```bash
kubectl port-forward -n taskmgmt svc/postgres 5433:5432
```

## 7. Debug khi lỗi (quan trọng nhất)

Làm theo thứ tự:
```bash
kubectl get pods -n taskmgmt                              # 1. pod nào lỗi
kubectl describe pod -n taskmgmt <tên-pod>                # 2. xem mục Events cuối cùng
kubectl logs -n taskmgmt deploy/user-service --tail=100   # 3. log app
kubectl logs -n taskmgmt deploy/user-service --previous   # 4. log của lần chạy trước (khi đang crash)
kubectl logs -n taskmgmt deploy/user-service -f           # theo dõi log trực tiếp
```
Lọc nhanh nguyên nhân Spring:
```bash
kubectl logs -n taskmgmt deploy/user-service --previous | grep -E "Caused by|APPLICATION FAILED|Action"
```
Vào trong pod:
```bash
kubectl exec -it -n taskmgmt postgres-0 -- psql -U postgres
kubectl exec -it -n taskmgmt deploy/user-service -- sh
```
Kiểm tra env mà pod đang nhận:
```bash
kubectl exec -n taskmgmt deploy/user-service -- env | grep -E "DB_|JWT|RABBIT"
```

Lỗi thường gặp:

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Pending`, Events: `Insufficient memory` | Docker Desktop cấp ít RAM | tăng RAM (mục 1) hoặc tắt bớt service (mục 8) |
| `ImagePullBackOff` | chưa `kind load` image | chạy lại mục 4 |
| `WeakKeyException ... bits` | `JWT_SECRET` ngắn hơn 32 ký tự | sửa secret, apply, restart |
| `password authentication failed` | mật khẩu DB khác lúc khởi tạo volume | xóa volume Postgres (mục 9) |
| `kubectl` timeout / máy đơ | quá tải | giảm service chạy, tăng RAM |

## 8. Các lệnh vận hành hằng ngày

Áp dụng thay đổi YAML:
```bash
kubectl apply -f k8s/00-config.yaml -f k8s/infra.yaml
helm upgrade --install taskmgmt k8s/helm/taskmgmt -n taskmgmt   # apps, config, HPA, PDB, Ingress
```
Sửa secret/configmap xong **phải restart pod** thì mới nhận giá trị mới:
```bash
kubectl apply -f k8s/secret.yaml
kubectl rollout restart deploy -n taskmgmt api-gateway user-service project-service task-service
```
Bật/tắt service (tiết kiệm tài nguyên). Mặc định `sprint`, `notification`, `dashboard` đang tắt:
```bash
kubectl scale deploy sprint-service notification-service dashboard-service -n taskmgmt --replicas=1   # bật
kubectl scale deploy sprint-service notification-service dashboard-service -n taskmgmt --replicas=0   # tắt
```
Tắt hết (giữ dữ liệu):
```bash
kubectl scale deploy --all -n taskmgmt --replicas=0
```
Bật lại 4 service chính + hạ tầng:
```bash
kubectl scale deploy rabbitmq redis api-gateway user-service project-service task-service -n taskmgmt --replicas=1
```
Xem tài nguyên (kind không có `kubectl top`):
```bash
docker stats --no-stream taskmgmt-control-plane
kubectl describe node | sed -n '/Allocated resources/,/Events/p'
```

### Kiểm chứng rolling update / HPA

```bash
kubectl get hpa -n taskmgmt -w                           # TARGETS phải hiện % thay vì <unknown>
kubectl rollout restart deploy/user-service -n taskmgmt
kubectl rollout status deploy/user-service -n taskmgmt   # pod mới Ready rồi pod cũ mới tắt, không downtime
# tạo tải để xem scale up:
kubectl run load -n taskmgmt --rm -it --image=busybox -- sh -c 'while true; do wget -q -O- http://api-gateway/actuator/health; done'
```
Lưu ý: sprint/notification/dashboard vẫn `replicas: 0` (tiết kiệm RAM) nên chưa có HPA.

## 9. Dọn dẹp / reset

Reset database (MẤT dữ liệu trong k8s):
```bash
kubectl delete statefulset postgres -n taskmgmt
kubectl delete pvc -n taskmgmt data-postgres-0
kubectl apply -f k8s/infra.yaml
```
Xóa toàn bộ app, giữ cluster:
```bash
kubectl delete namespace taskmgmt
```
Tạm dừng cluster cho nhẹ máy (dữ liệu giữ nguyên) và bật lại:
```bash
docker stop taskmgmt-control-plane
docker start taskmgmt-control-plane
```
Xóa hẳn cluster:
```bash
kind delete cluster --name taskmgmt
```

## 10. Thêm service mới

1. Viết Dockerfile; thêm một dòng vào `services:` trong `helm/taskmgmt/values.yaml` (tên, `port`, và `db`/`jwt`/`rabbit` nếu cần).
2. Service k8s mở cổng 80 và trỏ vào cổng container, nên service khác gọi `http://<tên-service>`.
3. Muốn đi qua gateway: thêm route trong `api-gateway/src/main/resources/application.properties` và biến `<TÊN>_SERVICE_URL` trong `config:` của `helm/taskmgmt/values.yaml`.
4. Muốn gọi từ service khác bằng Feign: `@FeignClient(name = "x", url = "${services.x.url:http://x}")`.

## 11. Chạy local không dùng k8s

Dùng biến môi trường trỏ về localhost, ví dụ `USER_SERVICE_URL=http://localhost:5001` cho gateway và `SERVICES_TASK_SERVICE_URL=http://localhost:5003` cho Feign. Hoặc dùng `docker compose up` (đã có sẵn các biến này).

## 12. Giám sát: Prometheus, Alertmanager, Grafana

Luồng dữ liệu:
```
Spring Boot (Micrometer) --/actuator/prometheus:9090--> Prometheus --rules--> Alertmanager --> (Slack/webhook)
                                                            \--> Grafana (dashboard)
```
- **Micrometer** đo sẵn request, JVM, DB pool; `micrometer-registry-prometheus` xuất ra định dạng text mà Prometheus đọc.
- **Cổng 9090 riêng** cho Actuator (`MANAGEMENT_SERVER_PORT`): không đi qua Service/Ingress nên người dùng ngoài không đọc được metrics; probe cũng dùng cổng này.
- **Prometheus tự tìm pod** có annotation `prometheus.io/scrape: "true"` (đặt trong template của chart) và scrape từng pod, nên mỗi replica của HPA có series riêng. Label `service` lấy từ label `app` của pod.
- **RED** = Rate (req/s), Errors (tỉ lệ 5xx), Duration (p95/p99): ba chỉ số cốt lõi của một service. Dashboard `TaskManagement - RED & JVM` có thêm phần USE cho JVM, CPU, DB pool.
- **Alert** (`observability/prometheus/rules/taskmgmt.rules.yml`): `TargetDown`, `ApiGatewayNoInstances`, `FrequentRestarts`, `HighErrorBudgetBurnFast/Slow` (SLO 99.5%, cảnh báo theo tốc độ đốt error budget), `HighLatencyP95`, `JvmHeapHigh`, `HighCpuUsage`, `DbConnectionPoolSaturated`. Alertmanager gom nhóm, chống lặp, và để alert `critical` chặn các `warning` cùng service.

Triển khai (đã tự chạy trong `build-and-deploy.sh` và workflow CD):
```bash
./k8s/deploy-observability.sh
```
Mở giao diện (chạy mỗi lệnh ở một terminal):
```bash
kubectl port-forward -n taskmgmt svc/grafana 3000:80          # http://localhost:3000  (admin/admin, đổi ở lần đầu)
kubectl port-forward -n taskmgmt svc/prometheus 9090:9090     # Status > Targets, Alerts
kubectl port-forward -n taskmgmt svc/alertmanager 9093:9093
```
Đặt mật khẩu Grafana (tuỳ chọn): `kubectl create secret generic grafana-admin -n taskmgmt --from-literal=password=... && kubectl rollout restart deploy/grafana -n taskmgmt`.

Thử alert:
```bash
kubectl scale deploy/user-service -n taskmgmt --replicas=0   # ~2 phút sau: TargetDown (nhớ HPA sẽ kéo lại, tạm xoá hpa user-service nếu cần)
k6 run load-tests/task-service/create-task-load-test.js       # xem Rate/Duration trên dashboard
```
Đổi rule hoặc dashboard: sửa file trong `observability/`, chạy lại `deploy-observability.sh` (pod tự restart nhờ checksum). Với docker compose thì `docker compose up -d prometheus alertmanager grafana` (Prometheus: `localhost:9090`, Alertmanager: `localhost:9093`).

Lưu ý: dữ liệu Prometheus lưu 7 ngày/1GB trên PVC `prometheus-data`; Loki chưa có trên k8s nên Grafana k8s chưa có log (compose thì có).
