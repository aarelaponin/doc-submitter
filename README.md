# Document Submitter Plugin for Joget DX8

A Joget DX8 plugin for GovStack Registration Building Block that extracts form data, transforms it to GovStack JSON format, and sends it to the Processing API for registration.

## Overview

This plugin is the **sender component** in a two-part architecture:
- **DocSubmitter** (this plugin) - Extracts Joget form data, transforms to GovStack JSON, sends to Processing API
- **ProcessingAPI** (receiver) - Receives GovStack JSON, validates, maps to Joget forms, saves to database

Together they enable bidirectional data exchange between Joget instances using GovStack Registration Building Block standards.

## Features

- **YAML-Driven Configuration** - All field mappings defined in `services.yml` (no code changes needed)
- **Automatic Field Transformation** - Handles dates, numbers, checkboxes, master data lookups
- **Grid/Subform Support** - Processes parent-child relationships (farmer → crops, livestock)
- **Field Normalization** - Converts yes/no ↔ 1/2 formats automatically
- **Validation Rules** - Enforces conditional requirements via `validation-rules.yaml`
- **Configuration Generators** - Auto-generate configuration from minimal hints (92% time savings)
- **Multi-Service Support** - Deploy multiple services to same Joget instance
- **Hot-Deployable** - OSGi bundle architecture, no server restart required

## Requirements

- Joget DX8 Platform
- Java 8 or higher
- Maven 3.6+

## Quick Start

### 1. Build the Plugin

```bash
mvn clean package
```

The compiled plugin will be available at `target/doc-submitter-8.1-SNAPSHOT.jar`

### 2. Install to Joget

1. Upload the JAR file through Joget's Manage Plugins interface (Settings → Manage Plugins)
2. The plugin will be hot-deployed without server restart

### 3. Configure in Workflow

1. Add "GovStack Document Submitter" as a Tool in your workflow process
2. Configure the plugin properties:
   - **Processing API URL**: `http://receiver:8080/jw/api/govstack/v2/{serviceId}/applications`
   - **Service ID**: `farmers_registry` (or your service ID from services.yml)
   - **Form Structure Path**: `src/main/resources/docs-metadata/form_structure.yaml`
   - **Services Config Path**: `src/main/resources/docs-metadata/services.yml`
   - **Validation Rules Path**: `src/main/resources/docs-metadata/validation-rules.yaml`

## Configuration

### Services Configuration (`services.yml`)

Single configuration file shared by both sender and receiver:

```yaml
service:
  id: farmers_registry
  name: Farmers Registry Service

metadata:
  masterDataFields: [district, cropType, livestockType]
  fieldNormalization:
    yesNo: [canReadWrite, cropProduction]
    oneTwo: [gender, chronicallyIll]

formMappings:
  farmerBasicInfo:
    formId: farmerBasicInfo
    tableName: app_fd_farmer_basic_data
    fields:
      - joget: national_id
        govstack: identifiers[0].value
        required: true
      - joget: first_name
        govstack: name.given[0]
        required: true
```

### Configuration Generators ⚡

**Generate configuration in 2 seconds instead of 3 hours:**

```bash
# Quick start for farmer registry
./generate-farmer-config.sh

# For new services
./generate-config.sh student-mapping-hints.yaml student-business-rules.yaml
```

**Benefits:**
- 92% time savings (15 minutes vs 3 hours)
- Auto-detects 21 master data fields + 18 normalization fields
- Zero typos, guaranteed consistency
- 84-100% field coverage

See [README-GENERATORS.md](README-GENERATORS.md) for details.

## Project Structure

```
doc-submitter/
├── src/main/java/global/govstack/farmreg/registration/
│   ├── lib/DocSubmitter.java                    # Main plugin - extracts & sends data
│   ├── model/
│   │   ├── MappingHints.java                    # Configuration model for generators
│   │   └── BusinessRules.java                   # Validation rules model
│   ├── util/
│   │   ├── ServicesYamlGenerator.java           # Auto-generates services.yml
│   │   └── ValidationRulesGenerator.java        # Auto-generates validation-rules.yaml
│   ├── service/                                  # Business logic services
│   └── exception/                                # Custom exceptions
├── src/main/resources/
│   ├── properties/DocSubmitter.json              # Plugin UI configuration
│   └── docs-metadata/
│       ├── form_structure.yaml                   # Form metadata (extracted from Joget)
│       ├── services.yml                          # Field mappings configuration
│       └── validation-rules.yaml                 # Business validation rules
├── templates/
│   ├── mapping-hints-template.yaml               # Template for new services
│   └── business-rules-template.yaml              # Template for validation rules
├── generate-config.sh                            # Main configuration generator
├── generate-farmer-config.sh                     # Quick farmer config generator
├── farmer-mapping-hints.yaml                     # Example: farmer registry hints
└── farmer-business-rules.yaml                    # Example: farmer validation rules
```

## Documentation

### Quick References
- **[README-GENERATORS.md](README-GENERATORS.md)** - Configuration generators quick start
- **[SERVICES_YML_GUIDE.md](docs/SERVICES_YML_GUIDE.md)** - services.yml format reference

### Comprehensive Guides
- **[END_TO_END_SERVICE_CONFIGURATION.md](../END_TO_END_SERVICE_CONFIGURATION.md)** - Complete 9-phase walkthrough
- **[GENERATOR_USAGE.md](GENERATOR_USAGE.md)** - Detailed generator usage
- **[CONFIGURATION_GUIDE.md](../CONFIGURATION_GUIDE.md)** - Overall architecture
- **[GENERATOR_SUMMARY.md](../GENERATOR_SUMMARY.md)** - Benefits & test results

### Joget Plugin Development
- **[docs/](docs/)** - Plugin basics, API reference, best practices

## Configuring a New Service

### Option 1: Quick Start with Generators (15 minutes)

```bash
# 1. Copy templates
cp templates/mapping-hints-template.yaml student-hints.yaml
cp templates/business-rules-template.yaml student-rules.yaml

# 2. Edit hints (10 minutes)
vim student-hints.yaml  # Set service ID, map key fields

# 3. Edit rules (5 minutes)
vim student-rules.yaml  # Define validation logic

# 4. Generate (2 seconds)
./generate-config.sh student-hints.yaml student-rules.yaml

# 5. Deploy
cp services.yml src/main/resources/docs-metadata/
mvn clean package
```

### Option 2: Manual Configuration (3 hours)

See [END_TO_END_SERVICE_CONFIGURATION.md](../END_TO_END_SERVICE_CONFIGURATION.md) for complete manual process.

## Examples

### Working Examples Included
- **Farmer Registry** - 11 forms, 104 fields, grid relationships, conditional validation
  - Configuration: `farmer-mapping-hints.yaml` (120 lines) + `farmer-business-rules.yaml` (17 lines)
  - Generated output: `services-farmer.yml` (378 lines) + `validation-rules-farmer.yaml` (24 lines)

### Additional Examples in Documentation
- **Student Enrollment** - Person entity with enrollment forms
- **Patient Registration** - Healthcare registration with medical history
- **Product Catalog** - Multi-category product management

## Architecture

```
Sender (port 8080-2, DB 3307)          Receiver (port 8080-1, DB 3306)
┌─────────────────────────┐           ┌─────────────────────────┐
│  Joget Form Submission  │           │    Processing API       │
│         ↓               │           │         ↓               │
│    DocSubmitter         │           │   Validates JSON        │
│    (this plugin)        │  HTTP     │         ↓               │
│         ↓               │  POST     │   Maps to Joget         │
│  Read services.yml      │  ────→    │         ↓               │
│         ↓               │  JSON     │  Read services.yml      │
│  Extract form data      │           │         ↓               │
│         ↓               │           │  Save to database       │
│  Transform to GovStack  │           │                         │
│         ↓               │           │                         │
│  Validate rules         │           │                         │
│         ↓               │           │                         │
│  Send HTTP POST         │           │                         │
└─────────────────────────┘           └─────────────────────────┘

      Both use SAME services.yml (single source of truth)
```

## Multi-Service Deployment

Deploy multiple services to the same Joget instance by creating separate plugin projects:

```
gs-plugins/
├── doc-submitter-farmers/
│   ├── pom.xml (artifactId: doc-submitter-farmers)
│   └── src/main/resources/docs-metadata/services.yml (farmers_registry)
├── doc-submitter-students/
│   ├── pom.xml (artifactId: doc-submitter-students)
│   └── src/main/resources/docs-metadata/services.yml (student_enrollment)
```

Different artifactIds = different OSGi bundles = no conflicts.

## Testing

```bash
# Compile generators
mvn compile

# Test farmer config generation
./generate-farmer-config.sh

# Compare with existing
diff services-farmer.yml src/main/resources/docs-metadata/services.yml
diff validation-rules-farmer.yaml src/main/resources/docs-metadata/validation-rules.yaml

# Run unit tests
mvn test
```

## Troubleshooting

### "ClassNotFoundException: ServicesYamlGenerator"
**Solution:** Compile first: `mvn compile`

### "Failed to load services.yml"
**Solution:** Check paths in plugin configuration match actual file locations

### "Field not mapping correctly"
**Solution:** Check field name in services.yml matches Joget form field ID exactly (case-sensitive)

### "Validation rule not triggering"
**Solution:** Check condition syntax in validation-rules.yaml matches field values exactly

## License

Part of the GovStack Registration Building Block initiative.

## Support

For issues and questions, please use the GitHub issue tracker.