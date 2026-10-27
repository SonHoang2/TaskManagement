#!/usr/bin/env python3
"""
Generates fake data for the whole schema (users, projects, tasks, ...), linked together like real data.

Model: users are split into teams of TEAM_SIZE people. Each team has a few projects whose members come from
that same team, so the same group of people works together on several projects. Every other table follows the
project:
  - projects: the owner is a user in the team, the project is created after the owner signed up
  - project_members: owner (OWNER), 1-2 ADMIN, the rest MEMBER; joined_at is after the project was created
  - project_invitations: every member (except the owner) has an ACCEPTED invitation sent by the owner/an admin,
    responded_at = joined_at; plus a few PENDING/REJECTED invitations for team members not in the project
  - labels: each project's own label set (Bug, Feature, Backend, ...)
  - sprints: consecutive 2-week sprints over the last 6 months; past = COMPLETED, running = ACTIVE,
    upcoming = PLANNED
  - tasks: reporter/assignee are members who had joined the project when the task was created; status matches
    the task's sprint (finished sprint -> mostly DONE, upcoming sprint -> TODO); subtasks point to a task in the
    same project
  - task_sprints, task_labels: only link a task to sprints/labels of the same project
  - task_histories: the assignee/status changes that lead to the task's current state
  - task_comments, task_attachments: written/uploaded by project members after the task was created
  - notifications: generated from exactly the events notification-service handles (task assigned, comment,
    invitation)
Every user has role USER and belongs to at least one project.

User i (0 <= i < N_USERS): id = UUID from md5('fake-user-<i>'), email user_<i>@example.com, password
Password123! (bcrypt). load-tests/project-service/list-projects-load-test.js relies on this convention to sign JWTs.

How to run:
    pip install bcrypt
    python3 generate_fake_data.py
    ./output/load_fake_data.sh      # load the CSVs into the 5 databases (see the instructions in the script)

Customize with environment variables:
    N_USERS=50000 TEAM_SIZE=25 PROJECTS_PER_TEAM=8 OUTPUT_DIR=./output WORKERS=12 SEED=42 python3 generate_fake_data.py
The defaults (50,000 users) generate about 16,000 projects and 560,000 tasks.
"""

import csv
import hashlib
import math
import os
import random
import time
import uuid
from concurrent.futures import ProcessPoolExecutor
from datetime import datetime, time as dtime, timedelta, timezone

import bcrypt

# --------------------------------------------------------------------------
# Configuration
# --------------------------------------------------------------------------
N_USERS = int(os.environ.get("N_USERS", 50_000))
TEAM_SIZE = int(os.environ.get("TEAM_SIZE", 25))
PROJECTS_PER_TEAM = int(os.environ.get("PROJECTS_PER_TEAM", 8))
MEMBERS_PER_PROJECT = (5, 15)
TASKS_PER_PROJECT = (10, 60)
OUTPUT_DIR = os.environ.get("OUTPUT_DIR", "output")
SEED = int(os.environ.get("SEED", 42))
WORKERS = int(os.environ.get("WORKERS", os.cpu_count() or 12))
TEAMS_PER_TASK = int(os.environ.get("TEAMS_PER_TASK", 50))  # teams per worker task

FAKE_PASSWORD = "Password123!"
SPRINT_LENGTH = timedelta(days=14)
SPRINT_WINDOW = timedelta(days=182)  # only generate sprints within the last ~6 months

# Column order of each CSV; output/load_fake_data.sh lists the columns in exactly this order
TABLES = {
    "users": ["id", "full_name", "email", "password", "avatar_url", "role", "created_at", "updated_at"],
    "projects": ["id", "name", "description", "owner_id", "created_at", "updated_at"],
    "project_members": ["id", "project_id", "user_id", "role", "joined_at"],
    "project_invitations": ["id", "project_id", "invitee_id", "invited_by_id", "status", "responded_at",
                            "created_at", "updated_at"],
    "labels": ["id", "project_id", "name", "color", "created_at"],
    "tasks": ["id", "project_id", "title", "description", "status", "priority", "assignee_id", "reporter_id",
              "parent_task_id", "due_date", "start_date", "created_at", "updated_at"],
    "task_comments": ["id", "task_id", "user_id", "content", "created_at"],
    "task_attachments": ["id", "task_id", "file_url", "file_name", "uploaded_by", "created_at"],
    "task_labels": ["id", "task_id", "label_id"],
    "task_histories": ["id", "task_id", "changed_by", "field", "old_value", "new_value", "created_at"],
    "sprints": ["id", "project_id", "name", "status", "start_date", "end_date", "created_at"],
    "task_sprints": ["task_id", "sprint_id"],
    "notifications": ["id", "user_id", "type", "content", "entity_id", "entity_type", "is_read", "created_at"],
}

# ==========================================================================
# Vocabulary
# ==========================================================================
FAMILY_NAMES = ["Nguyen", "Tran", "Le", "Pham", "Hoang", "Huynh", "Phan", "Vu", "Vo", "Dang", "Bui", "Do",
                "Ho", "Ngo", "Duong", "Ly"]
MIDDLE_NAMES = ["Van", "Thi", "Minh", "Duc", "Thanh", "Ngoc", "Quoc", "Hoang", "Gia", "Bao"]
GIVEN_NAMES = ["An", "Binh", "Chi", "Dung", "Giang", "Hoa", "Khanh", "Linh", "Minh", "Ngoc", "Phuong",
               "Quang", "Son", "Thao", "Uyen", "Trang", "Tuan", "Hieu", "Nam", "Lan", "Huy", "Vy", "Long", "Mai"]

PROJECT_AREAS = ["Customer Portal", "Mobile App", "Payment Gateway", "Admin Dashboard", "Data Pipeline",
                 "Marketing Website", "Inventory System", "HR Platform", "Booking Engine", "Notification Service",
                 "Analytics Platform", "Loyalty Program", "Search Service", "Internal Wiki", "CRM Integration",
                 "Checkout Flow", "Reporting Suite", "Onboarding Experience", "Billing System", "Support Center"]
PROJECT_QUALIFIERS = ["", "", "v2", "Redesign", "Migration", "MVP", "Revamp", "2.0"]
PROJECT_GOALS = ["Improve conversion and reduce drop-off for {area}.",
                 "Rebuild {area} on the new platform and retire the legacy stack.",
                 "Deliver the first public release of {area}.",
                 "Stabilize {area} and cut support tickets in half.",
                 "Add self-service features to {area} requested by customers."]

TASK_VERBS = ["Implement", "Fix", "Refactor", "Design", "Write tests for", "Document", "Optimize", "Review",
              "Add validation to", "Migrate", "Set up monitoring for", "Investigate"]
TASK_OBJECTS = ["login flow", "password reset", "user profile page", "search filters", "checkout page",
                "payment webhook", "email templates", "CSV export", "role permissions", "dashboard charts",
                "API pagination", "file upload", "notification settings", "audit log", "onboarding wizard",
                "rate limiting", "cache layer", "database indexes", "error handling", "mobile layout",
                "dark mode", "invoice PDF", "session timeout", "admin user list", "report scheduler"]
TASK_DESCRIPTIONS = ["Acceptance criteria are in the linked design. Coordinate with QA before closing.",
                     "Customers reported this in the last release; reproduce first, then fix.",
                     "Keep the change backward compatible with the current API clients.",
                     "Split into smaller PRs if the change touches more than one service.",
                     "Measure before and after; attach the numbers to this task.",
                     "Follow the existing patterns in the codebase and update the docs."]
COMMENTS = ["I'll pick this up after the standup.", "Pushed a first draft, please take a look.",
            "Blocked on the API change, waiting for backend.", "Can we clarify the acceptance criteria?",
            "Tested on staging, works as expected.", "Found an edge case with empty input, fixing now.",
            "Moved to the next sprint, not enough capacity.", "Design looks good to me.",
            "Added unit tests and updated the README.", "Let's discuss this in the planning meeting.",
            "Merged, closing once QA confirms.", "This also affects the mobile app."]

LABEL_COLORS = {"Bug": "#d73a4a", "Feature": "#a2eeef", "Enhancement": "#84b6eb", "Frontend": "#fbca04",
                "Backend": "#0e8a16", "DevOps": "#5319e7", "Documentation": "#0075ca", "Urgent": "#b60205",
                "Tech Debt": "#c5def5", "UX": "#f9d0c4", "Security": "#e99695", "Performance": "#bfdadc"}
LABEL_NAMES = list(LABEL_COLORS)

ATTACHMENT_NAMES = ["design-mockup.png", "error-screenshot.png", "requirements.pdf", "test-report.xlsx",
                    "api-spec.yaml", "meeting-notes.docx", "flow-diagram.png", "sample-data.csv", "logs.txt"]

PRIORITIES = ["LOW", "MEDIUM", "HIGH"]
PRIORITY_WEIGHTS = [30, 50, 20]


# ==========================================================================
# Helpers
# ==========================================================================
def user_id(i):
    """Deterministic id of user i; the load test recomputes exactly this value."""
    return str(uuid.UUID(hashlib.md5(f"fake-user-{i}".encode()).hexdigest()))


def new_id(rng):
    return str(uuid.UUID(int=rng.getrandbits(128), version=4))


def between(rng, start, end):
    """Random point in time within [start, end]; returns start if the range is empty."""
    if end <= start:
        return start
    return start + (end - start) * rng.random()


def iso(dt):
    return dt.isoformat() if dt else ""


def weighted(rng, options, weights):
    return rng.choices(options, weights=weights, k=1)[0]


def chunks(n, size):
    """Split n into (start, count) chunks of size <= size."""
    start = 0
    while start < n:
        count = min(size, n - start)
        yield start, count
        start += count


def part_path(table, batch):
    return os.path.join(OUTPUT_DIR, f"_{table}_part_{batch}.csv")


def merge_part_files(final_path, header, part_paths):
    """Merge the part files (no header) into one final file with a header, copying in large blocks."""
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
# Data generation for one team
# ==========================================================================
class Member:
    __slots__ = ("user", "role", "joined")

    def __init__(self, user, role, joined):
        self.user, self.role, self.joined = user, role, joined


class TeamGenerator:
    def __init__(self, team, ctx, out):
        self.rng = random.Random(ctx["seed"] * 1_000_003 + team)
        self.now = ctx["now"]
        self.password_hash = ctx["password_hash"]
        self.out = out
        first = team * TEAM_SIZE
        self.user_range = range(first, min(N_USERS, first + TEAM_SIZE))

    def notify(self, user, type_, content, entity_id, entity_type, at):
        # Old notifications have mostly been read
        read_chance = 0.9 if self.now - at > timedelta(days=7) else 0.3
        self.out["notifications"].writerow((new_id(self.rng), user, type_, content, entity_id, entity_type,
                                            "true" if self.rng.random() < read_chance else "false", iso(at)))

    def run(self):
        users = self.gen_users()
        projects = [self.plan_project(users) for _ in range(self.rng.randint(max(1, PROJECTS_PER_TEAM // 2),
                                                                             PROJECTS_PER_TEAM * 3 // 2))]
        self.cover_everyone(users, projects)
        for project in projects:
            self.gen_project(project, users)

    def gen_users(self):
        users = {}
        for i in self.user_range:
            created = self.now - timedelta(days=self.rng.uniform(30, 900))
            uid = user_id(i)
            name = f"{self.rng.choice(FAMILY_NAMES)} {self.rng.choice(MIDDLE_NAMES)} {self.rng.choice(GIVEN_NAMES)}"
            avatar = f"https://i.pravatar.cc/200?u={uid}" if self.rng.random() < 0.6 else ""
            updated = between(self.rng, created, min(self.now, created + timedelta(days=60)))
            self.out["users"].writerow((uid, name, f"user_{i}@example.com", self.password_hash, avatar, "USER",
                                        iso(created), iso(updated)))
            users[uid] = created
        return users

    def plan_project(self, users):
        rng = self.rng
        owner = rng.choice(list(users))
        created = between(rng, users[owner] + timedelta(days=1), self.now - timedelta(days=7))
        area = rng.choice(PROJECT_AREAS)
        name = f"{area} {rng.choice(PROJECT_QUALIFIERS)}".strip()
        members = [Member(owner, "OWNER", created)]

        others = [u for u in users if u != owner]
        size = min(len(others), rng.randint(*MEMBERS_PER_PROJECT) - 1)
        for u in rng.sample(others, size):
            members.append(Member(u, "MEMBER", self.join_time(created, users[u])))
        members.sort(key=lambda m: m.joined)
        for m in rng.sample(members[1:], min(len(members) - 1, rng.randint(1, 2))):
            m.role = "ADMIN"

        return {"id": new_id(rng), "name": name, "area": area, "owner": owner, "created": created,
                "members": members}

    def join_time(self, project_created, user_created):
        base = max(project_created, user_created)
        return between(self.rng, base, min(self.now - timedelta(hours=1), base + timedelta(days=30)))

    def cover_everyone(self, users, projects):
        """Anyone not in any project is added as a MEMBER of one of the team's projects."""
        covered = {m.user for p in projects for m in p["members"]}
        for u in users:
            if u not in covered:
                project = self.rng.choice(projects)
                project["members"].append(Member(u, "MEMBER", self.join_time(project["created"], users[u])))
                project["members"].sort(key=lambda m: m.joined)

    def gen_project(self, project, users):
        rng = self.rng
        pid, created, members = project["id"], project["created"], project["members"]

        # A project was last updated when its newest member joined
        self.out["projects"].writerow((pid, project["name"], rng.choice(PROJECT_GOALS).format(area=project["area"]),
                                       project["owner"], iso(created), iso(members[-1].joined)))
        for m in members:
            self.out["project_members"].writerow((new_id(rng), pid, m.user, m.role, iso(m.joined)))

        self.gen_invitations(project, users)
        labels = self.gen_labels(pid, created)
        sprints = self.gen_sprints(pid, created)

        tasks = []
        for _ in range(rng.randint(*TASKS_PER_PROJECT)):
            tasks.append(self.gen_task(project, labels, sprints, tasks))

    def gen_invitations(self, project, users):
        rng, now = self.rng, self.now
        pid, members = project["id"], project["members"]
        invite_text = f"You have been invited to join project: {project['name']}"

        for idx, m in enumerate(members):
            if m.role == "OWNER":
                continue
            # The inviter is the owner or an admin who joined earlier
            inviters = [x.user for x in members[:idx] if x.role in ("OWNER", "ADMIN")] or [project["owner"]]
            sent = between(rng, max(project["created"], m.joined - timedelta(days=3)), m.joined)
            self.out["project_invitations"].writerow((new_id(rng), pid, m.user, rng.choice(inviters), "ACCEPTED",
                                                      iso(m.joined), iso(sent), iso(m.joined)))
            self.notify(m.user, "PROJECT_INVITATION", invite_text, pid, "PROJECT", sent)

        member_ids = {m.user for m in members}
        outsiders = [u for u in users if u not in member_ids]
        managers = [m for m in members if m.role in ("OWNER", "ADMIN")]
        for u in rng.sample(outsiders, min(len(outsiders), rng.randint(0, 2))):
            inviter = rng.choice(managers)
            sent = between(rng, max(inviter.joined, users[u]), now - timedelta(hours=1))
            if rng.random() < 0.5:
                status, responded = "PENDING", None
            else:
                status, responded = "REJECTED", between(rng, sent, min(now, sent + timedelta(days=3)))
            self.out["project_invitations"].writerow((new_id(rng), pid, u, inviter.user, status, iso(responded),
                                                      iso(sent), iso(responded or sent)))
            self.notify(u, "PROJECT_INVITATION", invite_text, pid, "PROJECT", sent)

    def gen_labels(self, pid, project_created):
        labels = []
        for name in self.rng.sample(LABEL_NAMES, self.rng.randint(4, 7)):
            lid = new_id(self.rng)
            at = between(self.rng, project_created, min(self.now, project_created + timedelta(days=2)))
            self.out["labels"].writerow((lid, pid, name, LABEL_COLORS[name], iso(at)))
            labels.append(lid)
        return labels

    def gen_sprints(self, pid, project_created):
        """Consecutive 2-week sprints; always ends with exactly one PLANNED sprint starting after today."""
        now = self.now
        day = max(project_created + timedelta(days=3), now - SPRINT_WINDOW).date()
        sprints = []
        while True:
            start = datetime.combine(day, dtime(9), tzinfo=timezone.utc)
            end = datetime.combine(day + SPRINT_LENGTH - timedelta(days=1), dtime(18), tzinfo=timezone.utc)
            status = "COMPLETED" if end < now else "ACTIVE" if start <= now else "PLANNED"
            sid = new_id(self.rng)
            planned_at = between(self.rng, max(project_created, start - timedelta(days=3)), min(now, start))
            self.out["sprints"].writerow((sid, pid, f"Sprint {len(sprints) + 1}", status, start.date().isoformat(),
                                          end.date().isoformat(), iso(planned_at)))
            sprints.append({"id": sid, "status": status, "start": start, "end": end})
            if status == "PLANNED":
                return sprints
            day = end.date() + timedelta(days=1)

    def pick_status(self, sprint, created):
        rng = self.rng
        statuses = ["TODO", "IN_PROGRESS", "DONE"]
        if sprint is None:
            weights = [35, 15, 50] if self.now - created > timedelta(days=30) else [60, 25, 15]
        else:
            weights = {"COMPLETED": [5, 10, 85], "ACTIVE": [30, 40, 30], "PLANNED": [100, 0, 0]}[sprint["status"]]
        return weighted(rng, statuses, weights)

    def gen_task(self, project, labels, sprints, earlier_tasks):
        rng, now = self.rng, self.now
        out = self.out
        pid = project["id"]
        tid = new_id(rng)

        created = between(rng, project["created"], now - timedelta(hours=1))
        present = [m.user for m in project["members"] if m.joined <= created]
        reporter = rng.choice(present)

        # Most tasks go into the nearest sprint not yet finished when the task is created; the rest stay in the backlog.
        # The sprint must have at least 1 day left, since the task is assigned within 1 day and must start before the sprint ends.
        open_sprints = [s for s in sprints if s["end"] - created >= timedelta(days=1)]
        sprint = None
        if open_sprints and rng.random() < 0.75:
            sprint = open_sprints[0] if rng.random() < 0.6 else rng.choice(open_sprints)
        status = self.pick_status(sprint, created)

        assignee = rng.choice(present) if status != "TODO" or rng.random() < 0.7 else None
        title = f"{rng.choice(TASK_VERBS)} {rng.choice(TASK_OBJECTS)}"

        assigned_at = between(rng, created, min(now, created + timedelta(days=1))) if assignee else None
        started = done = None
        if status != "TODO":
            window_start = max(assigned_at, sprint["start"]) if sprint else assigned_at
            window_end = min(now, sprint["end"]) if sprint else now
            started = between(rng, window_start, window_end)
            if status == "DONE":
                done = between(rng, started, window_end)

        start_date = started or (sprint["start"] if sprint else None)
        if sprint:
            due_date = sprint["end"]
        elif rng.random() < 0.6:
            due_date = (start_date or created) + timedelta(days=rng.randint(3, 21))
        else:
            due_date = None

        # Change history leading to the current state
        history = []
        if assignee:
            history.append((reporter, "assignee_id", "", assignee, assigned_at))
        if started:
            history.append((assignee, "status", "TODO", "IN_PROGRESS", started))
        if done:
            history.append((assignee, "status", "IN_PROGRESS", "DONE", done))
        priority = weighted(rng, PRIORITIES, PRIORITY_WEIGHTS)
        if rng.random() < 0.15:
            old = rng.choice([p for p in PRIORITIES if p != priority])
            history.append((reporter, "priority", old, priority, between(rng, created, now)))
        updated = max([created] + [h[4] for h in history])

        # Subtasks only point to existing tasks (not subtasks) in the same project
        parents = [t for t, is_sub in earlier_tasks if not is_sub]
        parent = rng.choice(parents) if parents and rng.random() < 0.15 else None

        out["tasks"].writerow((tid, pid, title, rng.choice(TASK_DESCRIPTIONS), status, priority, assignee or "",
                               reporter, parent or "", iso(due_date), iso(start_date), iso(created), iso(updated)))
        for changed_by, field, old, new, at in history:
            out["task_histories"].writerow((new_id(rng), tid, changed_by, field, old, new, iso(at)))
        if sprint:
            out["task_sprints"].writerow((tid, sprint["id"]))
        for lid in rng.sample(labels, weighted(rng, [0, 1, 2, 3], [25, 40, 25, 10])):
            out["task_labels"].writerow((new_id(rng), tid, lid))

        if assignee and assignee != reporter:
            self.notify(assignee, "TASK_ASSIGNED", f"You have been assigned to task: {title}", tid, "TASK",
                        assigned_at)

        # Comments come from people involved in the task, or another project member
        people = [p for p in (assignee, reporter) if p]
        comment_until = done or now
        for _ in range(weighted(rng, [0, 1, 2, 3, 4], [35, 25, 20, 12, 8])):
            author = rng.choice(people) if rng.random() < 0.7 else rng.choice(present)
            at = between(rng, created, comment_until)
            out["task_comments"].writerow((new_id(rng), tid, author, rng.choice(COMMENTS), iso(at)))
            # Same as EventConsumer: notify the assignee and reporter, except the comment author
            for recipient in {assignee, reporter} - {None, author}:
                self.notify(recipient, "COMMENT", f"New comment on task: {title}", tid, "TASK", at)

        if rng.random() < 0.25:
            for name in rng.sample(ATTACHMENT_NAMES, rng.randint(1, 2)):
                file_id = new_id(rng)
                out["task_attachments"].writerow((new_id(rng), tid,
                                                  f"https://cdn.example.com/attachments/{file_id}/{name}", name,
                                                  rng.choice(people), iso(between(rng, created, comment_until))))

        return tid, parent is not None


def worker_teams(args):
    """Generate all data for a range of teams and write it to this batch's own part files."""
    batch, first_team, team_count, ctx = args
    files = {t: open(part_path(t, batch), "w", newline="", encoding="utf-8", buffering=1 << 20) for t in TABLES}
    try:
        writers = {t: csv.writer(f) for t, f in files.items()}
        for team in range(first_team, first_team + team_count):
            TeamGenerator(team, ctx, writers).run()
    finally:
        for f in files.values():
            f.close()


# ==========================================================================
# Orchestration
# ==========================================================================
def main():
    t_start = time.time()
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    n_teams = math.ceil(N_USERS / TEAM_SIZE)
    print(f"N_USERS={N_USERS:,}  TEAM_SIZE={TEAM_SIZE}  teams={n_teams:,}  WORKERS={WORKERS}  OUTPUT_DIR={OUTPUT_DIR}")

    # One hash for all users: bcrypt is deliberately slow, hashing per user would take hours
    ctx = {
        "seed": SEED,
        "now": datetime.now(timezone.utc),
        "password_hash": bcrypt.hashpw(FAKE_PASSWORD.encode(), bcrypt.gensalt(10)).decode(),
    }
    batches = list(chunks(n_teams, TEAMS_PER_TASK))
    arglist = [(batch, start, count, ctx) for batch, (start, count) in enumerate(batches)]

    print(f"[+] Sinh dữ liệu ({len(batches)} tasks song song)...")
    with ProcessPoolExecutor(max_workers=WORKERS) as executor:
        list(executor.map(worker_teams, arglist))

    for table, header in TABLES.items():
        final_path = os.path.join(OUTPUT_DIR, f"{table}.csv")
        merge_part_files(final_path, header, [part_path(table, b) for b in range(len(batches))])
        with open(final_path, "rb") as f:
            rows = sum(1 for _ in f) - 1
        print(f"  -> {final_path:<40} {rows:>10,} dòng")

    print(f"\nHoàn tất! ({time.time() - t_start:.1f}s) Toàn bộ CSV nằm trong thư mục:", os.path.abspath(OUTPUT_DIR))


if __name__ == "__main__":
    main()
