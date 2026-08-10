#!/usr/bin/env bash
# ==========================================================================
# Nạp dữ liệu CSV (đã sinh bằng generate_fake_data.py) vào 5 database
# theo đúng kiến trúc microservice: user_db, project_db, task_db,
# sprint_db, notification_db trên localhost.
#
# Cách dùng:
#   chmod +x load_fake_data.sh
#   ./load_fake_data.sh
#
# Yêu cầu trước khi chạy:
#   1. Đã tạo sẵn schema (bảng) trong từng DB tương ứng (từ file DBML export
#      ra SQL, hoặc migration của service).
#   2. psql đã cài, có thể kết nối tới localhost.
#   3. Thư mục hiện tại chứa các file .csv (users.csv, projects.csv, ...).
#
# QUAN TRỌNG - tên cột do Java/Hibernate sinh ra có thể không khớp CSV:
#   Mỗi lệnh copy_table() bên dưới có khai báo RÕ danh sách cột theo đúng
#   thứ tự trong file CSV (mình sinh ra). Postgres sẽ map cột thứ i của CSV
#   vào đúng tên cột thứ i bạn liệt kê ở đây - không quan tâm CSV header ghi
#   gì, cũng không quan tâm thứ tự cột vật lý trong bảng.
#   => Trước khi chạy, hãy đối chiếu với `\d <table>` trong từng DB rồi SỬA
#      LẠI đúng tên cột thật (vd owner_id -> ownerId, is_read -> isRead...)
#      trong từng dòng copy_table() tương ứng.
#
# LƯU Ý VỀ TRUNCATE:
#   Trước khi copy, script sẽ TRUNCATE ... RESTART IDENTITY CASCADE cho
#   từng bảng để xóa sạch dữ liệu cũ (và dữ liệu ở các bảng con tham chiếu
#   FK tới nó). Vì dùng CASCADE nên KHÔNG cần quan tâm thứ tự xóa - nhưng
#   cũng đồng nghĩa: nếu có bảng nào ngoài danh sách dưới đây tham chiếu FK
#   tới bảng bị truncate, dữ liệu bảng đó cũng sẽ bị xóa theo. Hãy chắc chắn
#   bạn hiểu rõ schema trước khi chạy trên môi trường có dữ liệu quan trọng.
# ==========================================================================
set -euo pipefail

# --------------------------------------------------------------------------
# Cấu hình kết nối - CHỈNH LẠI cho đúng môi trường của bạn
# --------------------------------------------------------------------------
DB_HOST="localhost"
DB_PORT="5432"
DB_USER="myuser"                 # đổi thành user Postgres của bạn
export PGPASSWORD="${PGPASSWORD:-mypassword}"   # hoặc export PGPASSWORD trước khi chạy script

# Thư mục chứa các file CSV (mặc định = thư mục hiện tại)
DATA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -W)"

# truncate_table <db> <table>
# Xóa sạch dữ liệu cũ trước khi nạp lại. CASCADE để Postgres tự xóa theo
# các bảng có FK tham chiếu tới bảng này. RESTART IDENTITY để reset lại
# các cột serial/identity (nếu có) về giá trị ban đầu.
truncate_table() {
  local db="$1" table="$2"
  echo "  -> TRUNCATE $db.$table"
  psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$db" -v ON_ERROR_STOP=1 \
    -c "TRUNCATE TABLE $table RESTART IDENTITY CASCADE"
}

# copy_table <db> <table> <csv> <"col1,col2,col3,...">
# Cột liệt kê trong đối số thứ 4 PHẢI đúng thứ tự với cột trong file CSV,
# và PHẢI là tên cột THẬT trong bảng Postgres (không phải tên trong CSV).
copy_table() {
  local db="$1" table="$2" csv="$3" columns="$4"
  echo "  -> $db.$table ($columns)  <=  $csv"
  psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$db" -v ON_ERROR_STOP=1 \
    -c "\\copy $table ($columns) FROM '$DATA_DIR/$csv' WITH (FORMAT csv, HEADER true)"
}

# load_table <db> <table> <csv> <"col1,col2,...">
# Gộp 2 bước: truncate rồi copy, cho gọn.
load_table() {
  local db="$1" table="$2" csv="$3" columns="$4"
  truncate_table "$db" "$table"
  copy_table "$db" "$table" "$csv" "$columns"
}

# # --------------------------------------------------------------------------
# # 1. user_db
# # --------------------------------------------------------------------------
# echo "[1/5] Nạp dữ liệu vào user_db..."
# load_table user_db users users.csv \
#   "id,full_name,email,password,avatar_url,role,created_at,updated_at"

# # --------------------------------------------------------------------------
# # 2. project_db  (thứ tự quan trọng vì có FK nội bộ trong cùng DB)
# # --------------------------------------------------------------------------
# echo "[2/5] Nạp dữ liệu vào project_db..."
# load_table project_db projects projects.csv \
#   "id,name,description,owner_id,created_at,updated_at"
# load_table project_db project_members project_members.csv \
#   "id,project_id,user_id,role,joined_at"
# load_table project_db project_invitations project_invitations.csv \
#   "id,project_id,invitee_id,invited_by_id,status,responded_at,created_at,updated_at"
# load_table project_db labels labels.csv \
#   "id,project_id,name,color,created_at"

# --------------------------------------------------------------------------
# 3. task_db
# --------------------------------------------------------------------------
echo "[3/5] Nạp dữ liệu vào task_db..."
load_table task_db tasks tasks.csv \
  "id,project_id,title,description,status,priority,assignee_id,reporter_id,parent_task_id,due_date,start_date,created_at,updated_at"
load_table task_db task_comments task_comments.csv \
  "id,task_id,user_id,content,created_at"
load_table task_db task_attachments task_attachments.csv \
  "id,task_id,file_url,file_name,uploaded_by,created_at"
load_table task_db task_labels task_labels.csv \
  "id,task_id,label_id"
load_table task_db task_histories task_histories.csv \
  "id,task_id,changed_by,field,old_value,new_value,created_at"

# # --------------------------------------------------------------------------
# # 4. sprint_db
# # --------------------------------------------------------------------------
# echo "[4/5] Nạp dữ liệu vào sprint_db..."
# load_table sprint_db sprints sprints.csv \
#   "id,project_id,name,status,start_date,end_date,created_at"
# load_table sprint_db task_sprints task_sprints.csv \
#   "task_id,sprint_id"

# # --------------------------------------------------------------------------
# # 5. notification_db
# # --------------------------------------------------------------------------
# echo "[5/5] Nạp dữ liệu vào notification_db..."
# load_table notification_db notifications notifications.csv \
#   "id,user_id,type,content,entity_id,entity_type,is_read,created_at"

# echo ""
# echo "Hoàn tất! Đã xóa dữ liệu cũ và nạp dữ liệu mới vào cả 5 database."
