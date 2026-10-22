import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiClient } from '../http/api-client';
import { PlatformDashboard } from '../models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly api = inject(ApiClient);

  /** dashboard-service answers `{ dashboard: … }` without a JSend envelope. */
  platform(): Observable<PlatformDashboard> {
    return this.api
      .getRaw<{ dashboard: PlatformDashboard }>('dashboard-service', '/dashboard')
      .pipe(map((d) => d.dashboard));
  }
}
