export class StringUtils {
  static trim(value?: string): string | undefined {
    return value?.trim();
  }

  static isBlank(value?: string): boolean {
    return !value || value.trim() === '';
  }

  static normalizeWhitespace(value: string): string {
    return value.trim().replace(/\s+/g, ' ');
  }

  static toTitleCase(value: string): string {
    return value
      .toLowerCase()
      .split(' ')
      .map(
        (word) =>
          word.charAt(0).toUpperCase() +
          word.slice(1),
      )
      .join(' ');
  }

  static normalizeName(value: string): string {
    return this.toTitleCase(
      this.normalizeWhitespace(value),
    );
  }

 static normalizeSubjectName(value: string) {
    return this.normalizeName(value);
  }
}