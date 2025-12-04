# bab Changelog

## [Unreleased]

## [0.0.2]

### Added
- Add Icons and plugin logo

## [0.0.1]

### Added
- File type recognition for `babfile.yml` and `babfile.yaml` (case-insensitive)
- JSON Schema validation with inline errors and warnings
- Autocompletion for babfile structure (tasks, deps, run, cmd, platforms, includes)
- Task dependency validation (errors for undefined task references)
- Self-dependency detection (errors when a task depends on itself)
- Task name autocompletion in `deps` field (excludes current task)
- Ctrl+Click navigation from dependency to task definition
- YAML syntax highlighting via bundled YAML plugin
