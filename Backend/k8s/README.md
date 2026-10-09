# Chạy TaskManagement trên Kubernetes (kind)

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
- `00-config.yaml`: namespace + ConfigMap
- `secret.yaml` (tự tạo từ `secret.example.yaml`, đã gitignore): mật khẩu, JWT
- `infra.yaml`: postgres, redis, rabbitmq
- `apps.yaml`: 7 service Spring Boot
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
kind create cluster --name taskmgmt
kubectl get nodes          # phải thấy 1 node Ready
kind get clusters          # liệt kê cluster
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
kubectl apply -f k8s/00-config.yaml -f k8s/infra.yaml -f k8s/apps.yaml
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

1. Viết Dockerfile; thêm service vào `apps.yaml` (copy một khối Deployment + Service có sẵn, đổi tên và cổng).
2. Service k8s mở cổng 80 và trỏ vào cổng container, nên service khác gọi `http://<tên-service>`.
3. Muốn đi qua gateway: thêm route trong `api-gateway/src/main/resources/application.properties` và biến `<TÊN>_SERVICE_URL` trong `00-config.yaml`.
4. Muốn gọi từ service khác bằng Feign: `@FeignClient(name = "x", url = "${services.x.url:http://x}")`.

## 11. Chạy local không dùng k8s

Dùng biến môi trường trỏ về localhost, ví dụ `USER_SERVICE_URL=http://localhost:5001` cho gateway và `SERVICES_TASK_SERVICE_URL=http://localhost:5003` cho Feign. Hoặc dùng `docker compose up` (đã có sẵn các biến này).
