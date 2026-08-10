#!/usr/bin/env python3
"""
Sinh dữ liệu giả (fake data) cho toàn bộ schema (users, projects, tasks, ...)
- Phiên bản tối ưu tốc độ: multiprocessing + thuật toán unique_pairs O(n).

Cách chạy:
    pip install faker --break-system-packages
    python3 generate_fake_data_fast.py

Tuỳ chỉnh:
    N_ROWS=1000000 OUTPUT_DIR=./output WORKERS=12 python3 generate_fake_data_fast.py

Import vào Postgres (thứ tự để không vỡ FK):
    \\copy users FROM 'output/users.csv' WITH (FORMAT csv, HEADER true);
    \\copy projects FROM 'output/projects.csv' WITH (FORMAT csv, HEADER true);
    \\copy project_members FROM 'output/project_members.csv' WITH (FORMAT csv, HEADER true);
    \\copy project_invitations FROM 'output/project_invitations.csv' WITH (FORMAT csv, HEADER true);
    \\copy labels FROM 'output/labels.csv' WITH (FORMAT csv, HEADER true);
    \\copy tasks FROM 'output/tasks.csv' WITH (FORMAT csv, HEADER true);
    \\copy task_comments FROM 'output/task_comments.csv' WITH (FORMAT csv, HEADER true);
    \\copy task_attachments FROM 'output/task_attachments.csv' WITH (FORMAT csv, HEADER true);
    \\copy task_labels FROM 'output/task_labels.csv' WITH (FORMAT csv, HEADER true);
    \\copy task_histories FROM 'output/task_histories.csv' WITH (FORMAT csv, HEADER true);
    \\copy sprints FROM 'output/sprints.csv' WITH (FORMAT csv, HEADER true);
    \\copy task_sprints FROM 'output/task_sprints.csv' WITH (FORMAT csv, HEADER true);
    \\copy notifications FROM 'output/notifications.csv' WITH (FORMAT csv, HEADER true);
"""

import csv
import io
import os
import random
import string
import time
import uuid
from concurrent.futures import ProcessPoolExecutor
from datetime import datetime, timedelta, timezone

from faker import Faker

# --------------------------------------------------------------------------
# Cấu hình
# --------------------------------------------------------------------------
N_ROWS = int(os.environ.get("N_ROWS", 1_000_000))
OUTPUT_DIR = os.environ.get("OUTPUT_DIR", "output")
SEED = int(os.environ.get("SEED", 42))
WORKERS = int(os.environ.get("WORKERS", os.cpu_count() or 12))
CHUNK_SIZE = int(os.environ.get("CHUNK_SIZE", 50_000))  # số dòng mỗi task con

os.makedirs(OUTPUT_DIR, exist_ok=True)


# ==========================================================================
# Helpers dùng chung (nhẹ, không phụ thuộc Faker để tránh overhead khởi tạo
# lặp lại object Faker() ở mỗi lần gọi hàm nhỏ)
# ==========================================================================
WORDS = [
    "alpha", "beta", "gamma", "delta", "omega", "sigma", "nova", "orbit",
    "pixel", "vertex", "quantum", "cipher", "matrix", "vector", "atlas",
    "falcon", "nimbus", "zenith", "cobalt", "prism", "flux", "ember",
    "cascade", "beacon", "harbor", "meridian", "aurora", "cinder", "drift",
    "echo", "forge", "grove", "haven", "ion", "jolt", "karma", "lumen",
]

FIRST_NAMES = [
    "Nguyen", "Tran", "Le", "Pham", "Hoang", "Huynh", "Phan", "Vu", "Vo",
    "Dang", "Bui", "Do", "Ho", "Ngo", "Duong", "Ly", "James", "Mary",
    "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda", "David",
]
LAST_NAMES = [
    "An", "Binh", "Chi", "Dung", "Giang", "Hoa", "Khanh", "Linh", "Minh",
    "Ngoc", "Phuong", "Quang", "Son", "Thao", "Uyen", "Smith", "Johnson",
    "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez",
]

FILE_EXTS = ["pdf", "png", "jpg", "docx", "xlsx", "zip", "csv", "txt"]


def fast_name(rng):
    return f"{rng.choice(FIRST_NAMES)} {rng.choice(LAST_NAMES)}"


def fast_hex_token(rng, length=64):
    return "".join(rng.choices(string.hexdigits.lower()[:16], k=length))


def fast_sentence(rng, n_words=8):
    return " ".join(rng.choices(WORDS, k=n_words)).capitalize()


def fast_paragraph(rng, n_sentences=3, words_per_sentence=8):
    return ". ".join(
        fast_sentence(rng, words_per_sentence) for _ in range(n_sentences)
    ) + "."


def fast_catch_phrase(rng):
    return f"{rng.choice(WORDS).capitalize()}-{rng.choice(WORDS)} {rng.choice(WORDS)} platform"


def fast_hex_color(rng):
    return "#" + "".join(rng.choices("0123456789abcdef", k=6))


def fast_file_name(rng):
    return f"{rng.choice(WORDS)}_{rng.randint(1000, 9999)}.{rng.choice(FILE_EXTS)}"


def new_uuid(rng):
    # uuid4 dùng os.urandom nội bộ (không phụ thuộc random.seed), vẫn ổn về
    # tốc độ và duy nhất toàn cục kể cả chạy song song nhiều process.
    return str(uuid.uuid4())


def random_dt(rng, start_days_ago=730, end_days_ago=0, now=None):
    now = now or datetime.now(timezone.utc)
    delta_days = rng.uniform(end_days_ago, start_days_ago)
    return now - timedelta(days=delta_days)


def iso(dt):
    return dt.isoformat() if dt else ""


# ==========================================================================
# Sinh cặp duy nhất (project_id, user_id) kiểu O(n), không set, không lặp dò
# ==========================================================================
def unique_pairs_fast(pool_a, pool_b, n, seed):
    """
    Trả về generator n cặp (a, b) duy nhất từ pool_a x pool_b.
    Dùng kỹ thuật: random permutation trên không gian chỉ số [0, len_a*len_b)
    rồi giải mã index -> (i, j) bằng chia/dư. Không cần set kiểm tra trùng,
    độ phức tạp O(n), an toàn khi n gần bằng len_a*len_b.
    """
    len_a, len_b = len(pool_a), len(pool_b)
    total = len_a * len_b
    if n > total:
        raise ValueError("Không đủ tổ hợp để sinh cặp duy nhất")

    rng = random.Random(seed)

    # Nếu n nhỏ hơn nhiều so với total, random.sample trên range là hiệu quả
    # (Python's random.sample dùng thuật toán tối ưu selection sampling).
    if n <= total // 2 or total < 5_000_000:
        indices = rng.sample(range(total), n)
    else:
        # n gần lấp đầy total: lấy phần bù (loại trừ) sẽ nhanh hơn
        exclude_count = total - n
        excluded = set(rng.sample(range(total), exclude_count))
        indices = (idx for idx in range(total) if idx not in excluded)

    for idx in indices:
        i = idx // len_b
        j = idx % len_b
        yield pool_a[i], pool_b[j]


# ==========================================================================
# CSV writer đơn giản dùng buffer StringIO để giảm số lần syscall write()
# ==========================================================================
class BufferedCsvWriter:
    def __init__(self, path, header, mode="w"):
        self.file = open(path, mode, newline="", encoding="utf-8")
        self.writer = csv.writer(self.file)
        if mode == "w" and header:
            self.writer.writerow(header)
        self.buffer = []
        self.batch_size = 20_000

    def write_row(self, row):
        self.buffer.append(row)
        if len(self.buffer) >= self.batch_size:
            self.flush()

    def flush(self):
        if self.buffer:
            self.writer.writerows(self.buffer)
            self.buffer = []

    def close(self):
        self.flush()
        self.file.close()


def chunks(n, size):
    """Chia n thành các đoạn (start, count) kích thước <= size."""
    start = 0
    while start < n:
        count = min(size, n - start)
        yield start, count
        start += count


def merge_part_files(final_path, header, part_paths):
    """Gộp các file part (không có header) thành 1 file cuối có header.
    Dùng copy nhị phân theo khối lớn để tối đa tốc độ I/O thay vì đọc/ghi
    từng dòng."""
    with open(final_path, "wb") as out:
        out.write((",".join(header) + "\r\n").encode("utf-8"))
        for p in part_paths:
            with open(p, "rb") as f:
                while True:
                    block = f.read(4 * 1024 * 1024)
                    if not block:
                        break
                    out.write(block)
            os.remove(p)


# ==========================================================================
# Worker functions cho từng bảng (chạy trong process con)
# Mỗi worker nhận (start_i, count, seed, ...pools cần thiết...) và tự ghi
# ra 1 file part riêng để tránh tranh chấp ghi file giữa các process.
# ==========================================================================
ROLES_USER = ["ADMIN", "USER", "SUPER_ADMIN"]
ROLES_MEMBER = ["OWNER", "ADMIN", "MEMBER"]
STATUS_INVITE = ["PENDING", "ACCEPTED", "REJECTED"]
STATUS_TASK = ["TODO", "IN_PROGRESS", "DONE"]
PRIORITY_TASK = ["LOW", "MEDIUM", "HIGH"]
STATUS_SPRINT = ["PLANNED", "ACTIVE", "COMPLETED"]
FIELDS_HISTORY = ["status", "priority", "assignee_id", "due_date", "title"]
NOTI_TYPES = ["TASK_ASSIGNED", "TASK_COMMENT", "COMMENT",
              "TASK_UPDATED", "PROJECT_INVITATION", "SYSTEM"]


def worker_users(args):
    start_i, count, seed, part_path = args
    rng = random.Random(seed + start_i)
    w = BufferedCsvWriter(part_path, None)
    now = datetime.now(timezone.utc)
    rows = []
    for i in range(start_i, start_i + count):
        uid = str(uuid.uuid4())
        created = random_dt(rng, 730, 30, now)
        updated = created + timedelta(days=rng.randint(0, 29))
        email = f"user_{i}_{uuid.uuid4().hex[:8]}@example.com"
        rows.append((
            uid,
            fast_name(rng),
            email,
            fast_hex_token(rng, 64),
            f"https://picsum.photos/seed/{uid[:8]}/200" if rng.random() < 0.6 else "",
            rng.choice(ROLES_USER),
            iso(created),
            iso(updated),
        ))
    w.writer.writerows(rows)
    w.close()
    return part_path, [r[0] for r in rows]


def worker_projects(args):
    start_i, count, seed, part_path, user_ids = args
    rng = random.Random(seed + start_i + 1)
    w = BufferedCsvWriter(part_path, None)
    now = datetime.now(timezone.utc)
    rows = []
    for i in range(start_i, start_i + count):
        pid = str(uuid.uuid4())
        created = random_dt(rng, 700, 10, now)
        updated = created + timedelta(days=rng.randint(0, 20))
        rows.append((
            pid,
            fast_catch_phrase(rng),
            fast_paragraph(rng, 2, 10),
            rng.choice(user_ids),
            iso(created),
            iso(updated),
        ))
    w.writer.writerows(rows)
    w.close()
    return part_path, [r[0] for r in rows]


def worker_pairs_generic(args):
    """Worker chung cho các bảng dạng cặp (a_id, b_id) không cần thêm field
    ngẫu nhiên phức tạp, ví dụ task_sprints, task_labels."""
    (part_path, pair_slice, row_builder_name, extra_seed) = args
    rng = random.Random(extra_seed)
    w = BufferedCsvWriter(part_path, None)
    rows = [ROW_BUILDERS[row_builder_name](rng, a, b) for a, b in pair_slice]
    w.writer.writerows(rows)
    w.close()
    return part_path


def _build_project_member(rng, project_id, user_id):
    return (str(uuid.uuid4()), project_id, user_id, rng.choice(ROLES_MEMBER),
            iso(random_dt(rng, 600, 0)))


def _build_task_label(rng, task_id, label_id):
    return (str(uuid.uuid4()), task_id, label_id)


def _build_task_sprint(rng, task_id, sprint_id):
    return (task_id, sprint_id)


ROW_BUILDERS = {
    "project_member": _build_project_member,
    "task_label": _build_task_label,
    "task_sprint": _build_task_sprint,
}


def worker_invitations(args):
    start_i, count, seed, part_path, project_ids, user_ids = args
    rng = random.Random(seed + start_i + 2)
    w = BufferedCsvWriter(part_path, None)
    now = datetime.now(timezone.utc)
    rows = []
    for _ in range(count):
        created = random_dt(rng, 365, 0, now)
        status = rng.choice(STATUS_INVITE)
        responded = created + timedelta(days=rng.randint(0, 5)) if status != "PENDING" else None
        rows.append((
            str(uuid.uuid4()),
            rng.choice(project_ids),
            rng.choice(user_ids),
            rng.choice(user_ids),
            status,
            iso(responded),
            iso(created),
            iso(responded or created),
        ))
    w.writer.writerows(rows)
    w.close()
    return part_path


def worker_labels(args):
    start_i, count, seed, part_path, project_ids = args
    rng = random.Random(seed + start_i + 3)
    w = BufferedCsvWriter(part_path, None)
    rows = []
    for _ in range(count):
        lid = str(uuid.uuid4())
        rows.append((
            lid,
            rng.choice(project_ids),
            rng.choice(WORDS).capitalize(),
            fast_hex_color(rng),
            iso(random_dt(rng, 600, 0)),
        ))
    w.writer.writerows(rows)
    w.close()
    return part_path, [r[0] for r in rows]


def worker_tasks(args):
    """
    Mỗi worker sinh 1 chunk task độc lập. parent_task_id chỉ được phép trỏ
    tới task khác đã sinh TRONG CÙNG chunk trước đó (đơn giản hoá so với bản
    gốc để cho phép chạy song song mà không cần đồng bộ toàn cục -- vẫn giữ
    đúng ngữ nghĩa "some tasks have subtasks" mà không vi phạm FK).
    """
    start_i, count, seed, part_path, project_ids, user_ids = args
    rng = random.Random(seed + start_i + 4)
    w = BufferedCsvWriter(part_path, None)
    now = datetime.now(timezone.utc)
    local_ids = []
    rows = []
    for _ in range(count):
        tid = str(uuid.uuid4())
        created = random_dt(rng, 500, 0, now)
        updated = created + timedelta(days=rng.randint(0, 15))
        start = random_dt(rng, 400, 100, now) if rng.random() < 0.7 else None
        due = (start or created) + timedelta(days=rng.randint(1, 30)) if rng.random() < 0.7 else None
        parent = rng.choice(local_ids) if (local_ids and rng.random() < 0.2) else None
        rows.append((
            tid,
            rng.choice(project_ids),
            fast_sentence(rng, 6),
            fast_paragraph(rng, 3, 12),
            rng.choice(STATUS_TASK),
            rng.choice(PRIORITY_TASK),
            rng.choice(user_ids) if rng.random() < 0.85 else "",
            rng.choice(user_ids),
            parent or "",
            iso(due),
            iso(start),
            iso(created),
            iso(updated),
        ))
        local_ids.append(tid)
    w.writer.writerows(rows)
    w.close()
    return part_path, local_ids


def worker_comments(args):
    start_i, count, seed, part_path, task_ids, user_ids = args
    rng = random.Random(seed + start_i + 5)
    w = BufferedCsvWriter(part_path, None)
    rows = []
    for _ in range(count):
        rows.append((
            str(uuid.uuid4()),
            rng.choice(task_ids),
            rng.choice(user_ids),
            fast_paragraph(rng, 3, 10),
            iso(random_dt(rng, 400, 0)),
        ))
    w.writer.writerows(rows)
    w.close()
    return part_path


def worker_attachments(args):
    start_i, count, seed, part_path, task_ids, user_ids = args
    rng = random.Random(seed + start_i + 6)
    w = BufferedCsvWriter(part_path, None)
    rows = []
    for _ in range(count):
        fname = fast_file_name(rng)
        rows.append((
            str(uuid.uuid4()),
            rng.choice(task_ids),
            f"https://cdn.example.com/{uuid.uuid4()}/{fname}",
            fname,
            rng.choice(user_ids),
            iso(random_dt(rng, 400, 0)),
        ))
    w.writer.writerows(rows)
    w.close()
    return part_path


def worker_histories(args):
    start_i, count, seed, part_path, task_ids, user_ids = args
    rng = random.Random(seed + start_i + 7)
    w = BufferedCsvWriter(part_path, None)
    rows = []
    for _ in range(count):
        field = rng.choice(FIELDS_HISTORY)
        if field == "status":
            old, new = rng.sample(STATUS_TASK, 2)
        elif field == "priority":
            old, new = rng.sample(PRIORITY_TASK, 2)
        else:
            old, new = rng.choice(WORDS), rng.choice(WORDS)
        rows.append((
            str(uuid.uuid4()),
            rng.choice(task_ids),
            rng.choice(user_ids),
            field,
            old,
            new,
            iso(random_dt(rng, 400, 0)),
        ))
    w.writer.writerows(rows)
    w.close()
    return part_path


def worker_sprints(args):
    start_i, count, seed, part_path, project_ids = args
    rng = random.Random(seed + start_i + 8)
    w = BufferedCsvWriter(part_path, None)
    rows = []
    for i in range(start_i, start_i + count):
        start = random_dt(rng, 400, 30).date()
        end = start + timedelta(days=rng.randint(7, 21))
        sid = str(uuid.uuid4())
        rows.append((
            sid,
            rng.choice(project_ids),
            f"Sprint {i + 1}",
            rng.choice(STATUS_SPRINT),
            start.isoformat(),
            end.isoformat(),
            iso(random_dt(rng, 400, 30)),
        ))
    w.writer.writerows(rows)
    w.close()
    return part_path, [r[0] for r in rows]


def worker_notifications(args):
    start_i, count, seed, part_path, task_ids, project_ids, sprint_ids, user_ids = args
    rng = random.Random(seed + start_i + 9)
    w = BufferedCsvWriter(part_path, None)
    entity_map = {"TASK": task_ids, "PROJECT": project_ids, "SPRINT": sprint_ids}
    entity_types = list(entity_map.keys())
    rows = []
    for _ in range(count):
        etype = rng.choice(entity_types)
        eid = rng.choice(entity_map[etype])
        rows.append((
            str(uuid.uuid4()),
            rng.choice(user_ids),
            rng.choice(NOTI_TYPES),
            fast_sentence(rng, 10),
            eid,
            etype,
            "true" if rng.random() < 0.4 else "false",
            iso(random_dt(rng, 200, 0)),
        ))
    w.writer.writerows(rows)
    w.close()
    return part_path


# ==========================================================================
# Orchestration
# ==========================================================================
def run_table(executor, table_name, header, worker_fn, arglist, out_path):
    """Chạy song song worker_fn trên arglist, gộp file part -> file cuối."""
    print(f"[+] Sinh bảng '{table_name}' ({N_ROWS:,} dòng, {len(arglist)} tasks song song)...")
    t0 = time.time()
    results = list(executor.map(worker_fn, arglist))
    part_paths = [r[0] if isinstance(r, tuple) else r for r in results]
    merge_part_files(out_path, header, part_paths)
    ids = []
    if results and isinstance(results[0], tuple):
        for r in results:
            ids.extend(r[1])
    print(f"  -> {out_path}  ({time.time() - t0:.1f}s)")
    return ids


def main():
    t_start = time.time()
    print(f"N_ROWS={N_ROWS:,}  WORKERS={WORKERS}  OUTPUT_DIR={OUTPUT_DIR}")

    with ProcessPoolExecutor(max_workers=WORKERS) as executor:

        # 1. users
        arglist = [
            (start, count, SEED, os.path.join(OUTPUT_DIR, f"_users_part_{start}.csv"))
            for start, count in chunks(N_ROWS, CHUNK_SIZE)
        ]
        user_ids = run_table(
            executor, "users",
            ["id", "full_name", "email", "password", "avatar_url", "role", "created_at", "updated_at"],
            worker_users, arglist, os.path.join(OUTPUT_DIR, "users.csv"),
        )

        # 2. projects
        arglist = [
            (start, count, SEED, os.path.join(OUTPUT_DIR, f"_projects_part_{start}.csv"), user_ids)
            for start, count in chunks(N_ROWS, CHUNK_SIZE)
        ]
        project_ids = run_table(
            executor, "projects",
            ["id", "name", "description", "owner_id", "created_at", "updated_at"],
            worker_projects, arglist, os.path.join(OUTPUT_DIR, "projects.csv"),
        )

        # 3. project_members (unique pairs project x user)
        print(f"[+] Sinh bảng 'project_members' ({N_ROWS:,} dòng)...")
        t0 = time.time()
        pairs = list(unique_pairs_fast(project_ids, user_ids, N_ROWS, SEED + 100))
        pair_chunks = list(chunks(len(pairs), CHUNK_SIZE))
        arglist = [
            (os.path.join(OUTPUT_DIR, f"_pm_part_{start}.csv"), pairs[start:start + count], "project_member", SEED + 100 + start)
            for start, count in pair_chunks
        ]
        part_paths = list(executor.map(worker_pairs_generic, arglist))
        merge_part_files(os.path.join(OUTPUT_DIR, "project_members.csv"),
                          ["id", "project_id", "user_id", "role", "joined_at"], part_paths)
        print(f"  -> project_members.csv ({time.time() - t0:.1f}s)")
        del pairs

        # 4. project_invitations
        arglist = [
            (start, count, SEED, os.path.join(OUTPUT_DIR, f"_inv_part_{start}.csv"), project_ids, user_ids)
            for start, count in chunks(N_ROWS, CHUNK_SIZE)
        ]
        run_table(
            executor, "project_invitations",
            ["id", "project_id", "invitee_id", "invited_by_id", "status", "responded_at", "created_at", "updated_at"],
            worker_invitations, arglist, os.path.join(OUTPUT_DIR, "project_invitations.csv"),
        )

        # 5. labels
        arglist = [
            (start, count, SEED, os.path.join(OUTPUT_DIR, f"_labels_part_{start}.csv"), project_ids)
            for start, count in chunks(N_ROWS, CHUNK_SIZE)
        ]
        label_ids = run_table(
            executor, "labels",
            ["id", "project_id", "name", "color", "created_at"],
            worker_labels, arglist, os.path.join(OUTPUT_DIR, "labels.csv"),
        )

        # 6. tasks (parent_task_id chỉ trỏ trong cùng chunk, xem docstring worker_tasks)
        arglist = [
            (start, count, SEED, os.path.join(OUTPUT_DIR, f"_tasks_part_{start}.csv"), project_ids, user_ids)
            for start, count in chunks(N_ROWS, CHUNK_SIZE)
        ]
        task_ids = run_table(
            executor, "tasks",
            ["id", "project_id", "title", "description", "status", "priority",
             "assignee_id", "reporter_id", "parent_task_id", "due_date", "start_date", "created_at", "updated_at"],
            worker_tasks, arglist, os.path.join(OUTPUT_DIR, "tasks.csv"),
        )

        # 7. task_comments
        arglist = [
            (start, count, SEED, os.path.join(OUTPUT_DIR, f"_comments_part_{start}.csv"), task_ids, user_ids)
            for start, count in chunks(N_ROWS, CHUNK_SIZE)
        ]
        run_table(
            executor, "task_comments",
            ["id", "task_id", "user_id", "content", "created_at"],
            worker_comments, arglist, os.path.join(OUTPUT_DIR, "task_comments.csv"),
        )

        # 8. task_attachments
        arglist = [
            (start, count, SEED, os.path.join(OUTPUT_DIR, f"_attach_part_{start}.csv"), task_ids, user_ids)
            for start, count in chunks(N_ROWS, CHUNK_SIZE)
        ]
        run_table(
            executor, "task_attachments",
            ["id", "task_id", "file_url", "file_name", "uploaded_by", "created_at"],
            worker_attachments, arglist, os.path.join(OUTPUT_DIR, "task_attachments.csv"),
        )

        # 9. task_labels (unique pairs task x label)
        print(f"[+] Sinh bảng 'task_labels' ({N_ROWS:,} dòng)...")
        t0 = time.time()
        pairs = list(unique_pairs_fast(task_ids, label_ids, N_ROWS, SEED + 200))
        pair_chunks = list(chunks(len(pairs), CHUNK_SIZE))
        arglist = [
            (os.path.join(OUTPUT_DIR, f"_tl_part_{start}.csv"), pairs[start:start + count], "task_label", SEED + 200 + start)
            for start, count in pair_chunks
        ]
        part_paths = list(executor.map(worker_pairs_generic, arglist))
        merge_part_files(os.path.join(OUTPUT_DIR, "task_labels.csv"),
                          ["id", "task_id", "label_id"], part_paths)
        print(f"  -> task_labels.csv ({time.time() - t0:.1f}s)")
        del pairs

        # 10. task_histories
        arglist = [
            (start, count, SEED, os.path.join(OUTPUT_DIR, f"_hist_part_{start}.csv"), task_ids, user_ids)
            for start, count in chunks(N_ROWS, CHUNK_SIZE)
        ]
        run_table(
            executor, "task_histories",
            ["id", "task_id", "changed_by", "field", "old_value", "new_value", "created_at"],
            worker_histories, arglist, os.path.join(OUTPUT_DIR, "task_histories.csv"),
        )

        # 11. sprints
        arglist = [
            (start, count, SEED, os.path.join(OUTPUT_DIR, f"_sprints_part_{start}.csv"), project_ids)
            for start, count in chunks(N_ROWS, CHUNK_SIZE)
        ]
        sprint_ids = run_table(
            executor, "sprints",
            ["id", "project_id", "name", "status", "start_date", "end_date", "created_at"],
            worker_sprints, arglist, os.path.join(OUTPUT_DIR, "sprints.csv"),
        )

        # 12. task_sprints (unique pairs task x sprint)
        print(f"[+] Sinh bảng 'task_sprints' ({N_ROWS:,} dòng)...")
        t0 = time.time()
        pairs = list(unique_pairs_fast(task_ids, sprint_ids, N_ROWS, SEED + 300))
        pair_chunks = list(chunks(len(pairs), CHUNK_SIZE))
        arglist = [
            (os.path.join(OUTPUT_DIR, f"_ts_part_{start}.csv"), pairs[start:start + count], "task_sprint", SEED + 300 + start)
            for start, count in pair_chunks
        ]
        part_paths = list(executor.map(worker_pairs_generic, arglist))
        merge_part_files(os.path.join(OUTPUT_DIR, "task_sprints.csv"),
                          ["task_id", "sprint_id"], part_paths)
        print(f"  -> task_sprints.csv ({time.time() - t0:.1f}s)")
        del pairs

        # 13. notifications
        arglist = [
            (start, count, SEED, os.path.join(OUTPUT_DIR, f"_noti_part_{start}.csv"), task_ids, project_ids, sprint_ids, user_ids)
            for start, count in chunks(N_ROWS, CHUNK_SIZE)
        ]
        run_table(
            executor, "notifications",
            ["id", "user_id", "type", "content", "entity_id", "entity_type", "is_read", "created_at"],
            worker_notifications, arglist, os.path.join(OUTPUT_DIR, "notifications.csv"),
        )

    print(f"\nHoàn tất! ({time.time() - t_start:.1f}s) Toàn bộ CSV nằm trong thư mục:", os.path.abspath(OUTPUT_DIR))


if __name__ == "__main__":
    main()