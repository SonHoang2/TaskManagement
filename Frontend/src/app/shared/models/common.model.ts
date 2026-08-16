export interface PaginationParams {
  page: number;
  size: number;
  sort?: string;
  direction?: 'ASC' | 'DESC';
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  page: number;
  hasNext: boolean;
  hasPrevious: boolean;
  numberOfElements: number;
}

export interface PageWrapper<T> {
  page: PaginatedResponse<T>;
}

export interface JSendResponse<T> {
  status: 'success' | 'error' | 'fail';
  data?: T;
  message?: string;
}
