import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { ProjectService } from './services/project.service';
import { Project, PaginationParams, SearchParams } from './models/project.model';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    FormsModule,
  ],
  templateUrl: './projects.component.html',
  styleUrl: './projects.component.scss',
})
export class ProjectsComponent implements OnInit {
  private readonly projectService = inject(ProjectService);
  private readonly snackBar = inject(MatSnackBar);

  readonly isLoading = signal(true);
  readonly searchTerm = signal('');
  readonly error = signal<string | null>(null);

  readonly projects = signal<Project[]>([]);
  readonly currentPage = signal(0);
  readonly pageSize = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

  readonly currentResultsCount = computed(() => this.projects().length);
  readonly resultsRange = computed(() => {
    const start = this.currentPage() * this.pageSize() + 1;
    const end = Math.min((this.currentPage() + 1) * this.pageSize(), this.totalElements());
    return { start, end };
  });

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.isLoading.set(true);
    this.error.set(null);

    const pagination: PaginationParams = {
      page: this.currentPage(),
      size: this.pageSize(),
    };

    const search: SearchParams = {
      search: this.searchTerm() || undefined,
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

  onSearchChange(value: string) {
    this.searchTerm.set(value);
    this.currentPage.set(0);
    this.loadProjects();
  }

  confirmDelete(projectId: string): void {
    const snackBarRef = this.snackBar.open(
      'Are you sure you want to delete this project?',
      'Delete',
      {
        duration: 5000,
        panelClass: 'delete-snackbar',
      },
    );

    snackBarRef.onAction().subscribe(() => {
      this.deleteProject(projectId);
    });
  }

  deleteProject(projectId: string) {
    this.isLoading.set(true);
    this.projectService.deleteProject(projectId).subscribe({
      next: (response) => {
        if (response.status === 'success') {
          this.snackBar.open('Project deleted successfully', 'Close', {
            duration: 3000,
          });
          this.loadProjects();
        } else {
          this.error.set(response.message || 'Failed to delete project');
          this.isLoading.set(false);
          this.snackBar.open('Failed to delete project', 'Close', {
            duration: 3000,
          });
        }
      },
      error: (err) => {
        this.error.set('An error occurred while deleting project');
        this.isLoading.set(false);
        this.snackBar.open('An error occurred while deleting project', 'Close', {
          duration: 3000,
        });
      },
    });
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadProjects();
    }
  }

  onPageInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const pageNumber = parseInt(input.value, 10);

    if (!isNaN(pageNumber) && pageNumber >= 1 && pageNumber <= this.totalPages()) {
      this.currentPage.set(pageNumber - 1);
      this.loadProjects();
    } else {
      // Reset to current page if invalid
      input.value = (this.currentPage() + 1).toString();
    }
  }

  onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.loadProjects();
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.currentPage.set(0);
    this.loadProjects();
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString();
  }
}
