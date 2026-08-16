import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';

export interface ApiError {
  message: string;
  code?: string;
  details?: any;
}

@Injectable({
  providedIn: 'root',
})
export class ErrorHandlingService {
  constructor() {}

  handleError(error: HttpErrorResponse): Observable<never> {
    let apiError: ApiError = {
      message: 'An unexpected error occurred',
    };

    if (error.error instanceof ErrorEvent) {
      apiError.message = `Client error: ${error.error.message}`;
    } else {
      if (error.status === 0) {
        apiError.message = 'Network error. Please check your connection.';
      } else if (error.status === 400) {
        apiError = this.parseJSendError(error.error);
        apiError.message = apiError.message || 'Bad request';
      } else if (error.status === 401) {
        apiError.message = 'Unauthorized. Please login again.';
      } else if (error.status === 403) {
        apiError.message = 'Access denied. You do not have permission.';
      } else if (error.status === 404) {
        apiError.message = 'Resource not found.';
      } else if (error.status === 500) {
        apiError.message = 'Server error. Please try again later.';
      } else {
        apiError = this.parseJSendError(error.error);
        apiError.message = apiError.message || `Error ${error.status}`;
      }
    }

    console.error('API Error:', apiError);
    return throwError(() => apiError);
  }

  private parseJSendError(error: any): ApiError {
    if (error && error.data) {
      return {
        message: error.data.message || error.message || 'An error occurred',
        code: error.data.code,
        details: error.data,
      };
    }
    return {
      message: error?.message || 'An error occurred',
      details: error,
    };
  }

  getErrorMessage(error: any): string {
    if (typeof error === 'string') {
      return error;
    }
    if (error?.message) {
      return error.message;
    }
    return 'An unexpected error occurred';
  }

  showError(error: any): void {
    const message = this.getErrorMessage(error);
    console.error('Error:', message);
  }
}
