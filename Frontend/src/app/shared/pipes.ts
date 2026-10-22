import { Pipe, PipeTransform } from '@angular/core';
import { relativeTime, shortDate } from '../core/utils/format';

/** Impure so "5m ago" keeps moving while the view is open. */
@Pipe({ name: 'ago', pure: false })
export class AgoPipe implements PipeTransform {
  transform(iso: string | null | undefined): string {
    return relativeTime(iso);
  }
}

@Pipe({ name: 'shortDate' })
export class ShortDatePipe implements PipeTransform {
  transform(iso: string | null | undefined): string {
    return shortDate(iso);
  }
}
