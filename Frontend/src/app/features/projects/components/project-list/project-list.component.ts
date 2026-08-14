import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProjectService } from '../../services/project.service';
import { Project, PaginationParams, SearchParams } from '../../models/project.model';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './project-list.component.html',
  styleUrls: ['./project-list.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectListComponent {
  private readonly projectService = inject(ProjectService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly projects = signal<Project[]>([]);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly currentPage = signal(0);
  protected readonly pageSize = signal(10);
  protected readonly isLoading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly searchForm: FormGroup = this.fb.group({
    search: [''],
    status: [''],
  });

  protected readonly sortOptions = [
    { value: 'createdAt', label: 'Created Date' },
    { value: 'name', label: 'Name' },
    { value: 'updatedAt', label: 'Last Updated' },
  ];

  protected readonly currentSort = signal('createdAt');
  protected readonly sortDirection = signal<'ASC' | 'DESC'>('DESC');

  ngOnInit(): void {
    this.loadProjects();
  }

  protected loadProjects(): void {
    this.isLoading.set(true);
    this.error.set(null);

    const pagination: PaginationParams = {
      page: this.currentPage(),
      size: this.pageSize(),
      sort: this.currentSort(),
      direction: this.sortDirection(),
    };

    const search: SearchParams = {
      search: this.searchForm.value.search || undefined,
      status: this.searchForm.value.status || undefined,
    };

    this.projectService.getProjects(pagination, search).subscribe({
      next: (response) => {
        if (response.status === 'success' && response.data) {
          this.projects.set(response.data.content);
          this.totalElements.set(response.data.totalElements);
          this.totalPages.set(response.data.totalPages);
        } else {
          this.error.set(response.message || 'Failed to load projects');
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('An error occurred while loading projects');
        this.isLoading.set(false);
      },
    });
  }

  protected onSearch(): void {
    this.currentPage.set(0);
    this.loadProjects();
  }

  protected onSortChange(sort: string): void {
    this.currentSort.set(sort);
    this.currentPage.set(0);
    this.loadProjects();
  }

  protected onSortDirectionChange(): void {
    this.sortDirection.update((dir) => (dir === 'ASC' ? 'DESC' : 'ASC'));
    this.currentPage.set(0);
    this.loadProjects();
  }

  protected onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadProjects();
  }

  protected onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.loadProjects();
  }

  protected navigateToProject(projectId: string): void {
    this.router.navigate(['/projects', projectId]);
  }

  protected createNewProject(): void {
    this.router.navigate(['/projects', 'new']);
  }

  protected getPaginationArray(): number[] {
    const pages: number[] = [];
    const totalPages = this.totalPages();
    const current = this.currentPage();

    for (let i = 0; i < totalPages; i++) {
      if (i === 0 || i === totalPages - 1 || (i >= current - 1 && i <= current + 1)) {
        pages.push(i);
      }
    }
    return pages;
  }

  protected formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString();
  }

  protected readonly min = (a: number, b: number): number => Math.min(a, b);
}
