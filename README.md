# Bab

![Build](https://github.com/bab-sh/bab-intellij-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/29264.svg)](https://plugins.jetbrains.com/plugin/29264)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/29264.svg)](https://plugins.jetbrains.com/plugin/29264)

<!-- Plugin description -->
IntelliJ plugin for [bab](https://bab.sh) - a simple, cross-platform task runner.

## Features

### Editor Support
- Syntax highlighting for `babfile.yml` and `babfile.yaml`
- JSON Schema validation with inline errors and warnings
- Autocompletion for all babfile properties

### Task Navigation
- Ctrl+Click navigation from dependency references to task definitions
- Task name autocompletion in `deps` and `run.task` fields
- Support for included babfiles with prefixed task references

### Code Inspection
- Unresolved task reference detection
- Self-dependency detection
- Real-time error highlighting

### Run Configuration
- Run tasks directly from the editor gutter
- Configurable run configurations with working directory and arguments
- Dry-run mode support

### Tool Window
- Hierarchical task browser in the right panel
- Double-click to navigate to task definition
- Search and filter tasks
<!-- Plugin description end -->

## Installation

**From JetBrains Marketplace:**

<kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > Search for "Bab" > <kbd>Install</kbd>

**Manual Installation:**

Download the [latest release](https://github.com/bab-sh/bab-intellij-plugin/releases/latest) and install via <kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install Plugin from Disk...</kbd>

## Requirements

- IntelliJ IDEA 2025.2 or later
- [bab](https://bab.sh) installed for running tasks
