# Document Submitter Plugin for Joget DX8

A Joget DX8 plugin for GovStack Registration Building Block that processes and submits documents within workflow processes.

## Overview

This plugin provides document submission capabilities for the GovStack Registration Building Block, enabling seamless integration with Joget workflow processes to handle document processing and transmission.

## Features

- Process documents within Joget workflows
- Configurable processing modes and parameters
- Integration with GovStack Registration Building Block
- Hot-deployable OSGi bundle architecture

## Requirements

- Joget DX8 Platform
- Java 11 or higher
- Maven 3.6+

## Building

```bash
# Build the plugin
mvn clean package
```

The compiled plugin will be available at `target/doc-submitter-8.1-SNAPSHOT.jar`

## Installation

1. Build the plugin using Maven
2. Upload the JAR file through Joget's Manage Plugins interface
3. The plugin will be hot-deployed without server restart

## Configuration

The plugin can be configured through Joget's UI with the following settings:
- Processing Mode (Automatic/Manual)
- Amount Tolerance for matching
- Settlement days configuration
- Batch processing parameters

## Project Structure

```
├── src/main/java/
│   └── global/govstack/farmreg/
│       ├── Activator.java              # OSGi bundle activator
│       └── registration/
│           ├── lib/DocSubmitter.java    # Main plugin implementation
│           ├── model/                   # Data models
│           └── constants/               # Constants and configurations
├── src/main/resources/
│   └── properties/DocSubmitter.json     # Plugin configuration schema
├── docs/                                # Joget plugin development documentation
└── pom.xml                             # Maven build configuration
```

## Documentation

Comprehensive Joget plugin development guides are available in the `docs/` directory:
- Plugin basics and architecture
- Development best practices
- API reference
- Plugin types and examples

## License

Part of the GovStack Registration Building Block initiative.

## Support

For issues and questions, please use the GitHub issue tracker.