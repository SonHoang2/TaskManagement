import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class HttpService {
  private readonly apiUrl = environment.apiGatewayUrl;

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwt_token');
    let headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    return headers;
  }

  get<T>(endpoint: string, params?: HttpParams): Observable<T> {
    return this.http.get<T>(`${this.apiUrl}${endpoint}`, {
      headers: this.getHeaders(),
      params
    });
  }

  post<T>(endpoint: string, body: any, params?: HttpParams): Observable<T> {
    return this.http.post<T>(`${this.apiUrl}${endpoint}`, body, {
      headers: this.getHeaders(),
      params
    });
  }

  put<T>(endpoint: string, body: any, params?: HttpParams): Observable<T> {
    return this.http.put<T>(`${this.apiUrl}${endpoint}`, body, {
      headers: this.getHeaders(),
      params
    });
  }

  patch<T>(endpoint: string, body: any, params?: HttpParams): Observable<T> {
    return this.http.patch<T>(`${this.apiUrl}${endpoint}`, body, {
      headers: this.getHeaders(),
      params
    });
  }

  delete<T>(endpoint: string, params?: HttpParams): Observable<T> {
    return this.http.delete<T>(`${this.apiUrl}${endpoint}`, {
      headers: this.getHeaders(),
      params
    });
  }

  postFormData<T>(endpoint: string, formData: FormData): Observable<T> {
    const headers = new HttpHeaders();
    const token = localStorage.getItem('jwt_token');
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }

    return this.http.post<T>(`${this.apiUrl}${endpoint}`, formData, {
      headers
    });
  }
}
