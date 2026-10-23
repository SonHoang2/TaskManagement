import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { JSend, PageQuery } from '../models';

/** Microservices routed by the API gateway as `/<service>/**`. */
export type ServiceName =
  | 'user-service'
  | 'project-service'
  | 'task-service'
  | 'sprint-service'
  | 'notification-service'
  | 'dashboard-service';

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly fieldErrors: Record<string, string> = {},
  ) {
    super(message);
  }
}

type Params = Record<string, string | number | boolean | null | undefined>;

/**
 * Thin HttpClient wrapper: builds gateway URLs (`/<service>/api/v1/<path>`),
 * unwraps JSend envelopes and normalises errors into {@link ApiError}.
 */
@Injectable({ providedIn: 'root' })
export class ApiClient {
  private readonly http = inject(HttpClient);

  url(service: ServiceName, path: string): string {
    return `${environment.apiGatewayUrl}/${service}/api/v1${path}`;
  }

  /** WebSocket endpoints are mounted outside `/api/v1`, e.g. `/notification-service/ws`. */
  wsUrl(service: ServiceName, path: string): string {
    return `${environment.apiGatewayUrl.replace(/^http/, 'ws')}/${service}${path}`;
  }

  get<T>(service: ServiceName, path: string, params?: Params): Observable<T> {
    return this.unwrap(
      this.http.get<JSend<T>>(this.url(service, path), { params: toParams(params) }),
    );
  }

  /** For endpoints that do not use the JSend envelope (dashboard-service). */
  getRaw<T>(service: ServiceName, path: string): Observable<T> {
    return this.http.get<T>(this.url(service, path)).pipe(catchError(toApiError));
  }

  post<T>(service: ServiceName, path: string, body: unknown): Observable<T> {
    return this.unwrap(this.http.post<JSend<T>>(this.url(service, path), body));
  }

  patch<T>(service: ServiceName, path: string, body: unknown): Observable<T> {
    return this.unwrap(this.http.patch<JSend<T>>(this.url(service, path), body));
  }

  delete(service: ServiceName, path: string): Observable<void> {
    return this.unwrap(this.http.delete<JSend<void>>(this.url(service, path)));
  }

  private unwrap<T>(source: Observable<JSend<T>>): Observable<T> {
    return source.pipe(
      map((res) => res?.data as T),
      catchError(toApiError),
    );
  }
}

export function pageParams(q: PageQuery = {}, extra: Params = {}): Params {
  return { page: q.page ?? 0, size: q.size ?? 20, sort: q.sort, ...extra };
}

function toParams(params?: Params): HttpParams | undefined {
  if (!params) return undefined;
  let p = new HttpParams();
  for (const [k, v] of Object.entries(params)) {
    if (v !== null && v !== undefined && v !== '') p = p.set(k, String(v));
  }
  return p;
}

function toApiError(err: unknown): Observable<never> {
  if (!(err instanceof HttpErrorResponse)) {
    return throwError(() => new ApiError(String(err), 0));
  }
  const body = err.error as Partial<JSend<{ validationErrors?: Record<string, string> }>> | null;
  const fieldErrors = body?.data?.validationErrors ?? {};
  let message = body?.message;

  if (!message) {
    switch (err.status) {
      case 0:
        message = 'Cannot reach the server. Check that the API gateway is running.';
        break;
      case 401:
        message = 'Your session has expired. Please sign in again.';
        break;
      case 403:
        message = "You don't have permission to do that.";
        break;
      case 404:
        message = 'Not found.';
        break;
      case 429:
        message = 'Too many requests — please slow down and try again shortly.';
        break;
      default:
        message = err.status >= 500 ? 'Something went wrong on the server.' : 'Request failed.';
    }
  } else if (message === 'Validation failed' && Object.keys(fieldErrors).length) {
    message = Object.values(fieldErrors).join(', ');
  }
  return throwError(() => new ApiError(message!, err.status, fieldErrors));
}

export function errorMessage(err: unknown): string {
  if (err instanceof ApiError) return err.message;
  if (err instanceof Error) return err.message;
  return 'Something went wrong.';
}
