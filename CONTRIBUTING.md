# Contributing to JairoSVG

Thank you for your interest in contributing!

## Development Setup

### Prerequisites
- Java 25+
- Maven 3.9+ (or use the included `./mvnw` wrapper)

### Building
```bash
./mvnw clean verify
```

### Running Tests
```bash
./mvnw test
```

### Code Formatting
This project uses [Spotless](https://github.com/diffplug/spotless) for code formatting. Before submitting a PR:
```bash
./mvnw spotless:apply
```

## Submitting Changes

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make your changes
4. Run tests: `./mvnw clean verify`
5. Apply formatting: `./mvnw spotless:apply`
6. Commit with a descriptive message
7. Push and open a Pull Request

## Reporting Issues

- Use the [bug report template](.github/ISSUE_TEMPLATE/bug_report.md) for bugs
- Include: SVG input, expected output, actual output, Java version

## Code of Conduct

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md).
