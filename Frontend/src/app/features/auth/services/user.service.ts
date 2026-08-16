import { Injectable } from '@angular/core';
import { HttpService } from '../../../core/services/http.service';
import { Observable } from 'rxjs';

export interface UserProfile {
  id: string;
  email: string;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateProfileRequest {
  name?: string;
  email?: string;
}

export interface JSendResponse<T> {
  status: 'success' | 'error' | 'fail';
  data?: T;
  message?: string;
}

@Injectable({
  providedIn: 'root',
})
export class UserService {
  constructor(private httpService: HttpService) {}

  getProfile(): Observable<JSendResponse<UserProfile>> {
    return this.httpService.get<JSendResponse<UserProfile>>('/api/users/profile');
  }

  updateProfile(data: UpdateProfileRequest): Observable<JSendResponse<UserProfile>> {
    return this.httpService.put<JSendResponse<UserProfile>>('/api/users/profile', data);
  }

  deleteAccount(): Observable<JSendResponse<{ message: string }>> {
    return this.httpService.delete<JSendResponse<{ message: string }>>('/api/users/profile');
  }
}
