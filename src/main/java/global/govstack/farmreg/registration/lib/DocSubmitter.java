package global.govstack.farmreg.registration.lib;

import global.govstack.farmreg.registration.model.PluginResponse;
import global.govstack.farmreg.registration.service.GovStackApiClient;
import global.govstack.farmreg.registration.service.metadata.GenericFormDataExtractor;
import global.govstack.farmreg.registration.service.metadata.GovStackJsonEncoder;
import global.govstack.farmreg.registration.service.metadata.YamlMetadataService;
import org.joget.apps.app.service.AppUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.WorkflowVariable;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.commons.util.LogUtil;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * GovStack Registration Building Block Plugin for sending Document
 *
 * This plugin extracts farmer registration data from Joget forms,
 * converts it to GovStack JSON format, and sends it to the Processing Server API
 *
 * @author GovStack Registration BB Team
 * @version 2.0
 */
public class DocSubmitter extends DefaultApplicationPlugin {

    private static final String PLUGIN_NAME = "GovStack Farmer Data Submitter";
    private static final String PLUGIN_VERSION = "2.0.0";

    private YamlMetadataService metadataService;
    private GenericFormDataExtractor dataExtractor;
    private GovStackJsonEncoder jsonEncoder;

    /**
     * Plugin execution entry point
     * Called when the plugin is executed as a process tool activity
     */
    @Override
    public Object execute(Map properties) {
        LogUtil.info(getClassName(), "Executing GovStack Farmer Data Submitter Plugin");

        try {
            // Initialize metadata-driven services
            initializeServices();
            // Debug: Log all properties received
            LogUtil.info(getClassName(), "Received properties count: " + properties.size());
            for (Object key : properties.keySet()) {
                Object value = properties.get(key);
                if (value != null) {
                    String valueStr = value.toString();
                    // Limit log length for large values
                    if (valueStr.length() > 100) {
                        valueStr = valueStr.substring(0, 100) + "...";
                    }
                    LogUtil.info(getClassName(), "Property [" + key + "] = " + valueStr);
                }
            }

            // Get configuration from properties
            String apiEndpoint = getPropertyString("apiEndpoint", properties);
            String apiId = getPropertyString("apiId", properties);
            String apiKey = getPropertyString("apiKey", properties);
            String extractionMode = getPropertyString("extractionMode", properties);
            boolean validateBeforeSending = "true".equals(getPropertyString("validateBeforeSending", properties));
            boolean updateWorkflowStatus = "true".equals(getPropertyString("updateWorkflowStatus", properties));
            boolean logJsonPayload = "true".equals(getPropertyString("logJsonPayload", properties));

            // Get farmer ID using standard Joget pattern
            String farmerId = null;
            if ("specific".equals(extractionMode)) {
                farmerId = getPropertyString("specificFarmerId", properties);
                LogUtil.info(getClassName(), "Using specific farmer ID: " + farmerId);
            } else {
                // Get the workflow assignment
                WorkflowAssignment assignment = (WorkflowAssignment) properties.get("workflowAssignment");

                if (assignment != null) {
                    String processId = assignment.getProcessId();
                    LogUtil.info(getClassName(), "Process ID: " + processId);

                    // Get workflow manager to retrieve process variables
                    WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext()
                        .getBean("workflowManager");

                    // Try to get the record ID from workflow variables in priority order
                    String[] variableNames = {"recordId", "formRecordId", "farmerId", "primaryKey", "id"};

                    for (String varName : variableNames) {
                        farmerId = workflowManager.getProcessVariable(processId, varName);
                        if (farmerId != null && !farmerId.trim().isEmpty()) {
                            LogUtil.info(getClassName(), "Found farmer ID in workflow variable '" + varName + "': " + farmerId);
                            break;
                        }
                    }

                    // Log all workflow variables for debugging
                    LogUtil.info(getClassName(), "Checking all process variables for debugging:");
                    Collection<WorkflowVariable> variables = workflowManager.getProcessVariableList(processId);
                    if (variables != null) {
                        for (WorkflowVariable var : variables) {
                            LogUtil.info(getClassName(), "  Variable [" + var.getId() + "] = " + var.getVal());
                        }
                    }

                    if (farmerId == null || farmerId.trim().isEmpty()) {
                        LogUtil.error(getClassName(), null, "No farmer ID found in any workflow variable");
                    } else {
                        LogUtil.info(getClassName(), "Successfully retrieved Farmer ID: " + farmerId);
                    }
                } else {
                    LogUtil.warn(getClassName(), "WorkflowAssignment is null");
                }
            }

            if (farmerId == null || farmerId.trim().isEmpty()) {
                LogUtil.error(getClassName(), null, "Farmer ID not found in workflow");
                return PluginResponse.error("Farmer ID not found");
            }

            LogUtil.info(getClassName(), "Processing registration data for ID: " + farmerId);

            // Extract form data using metadata
            Map<String, Object> formData = dataExtractor.extractAllFormData(farmerId);

            if (formData == null || formData.isEmpty()) {
                LogUtil.error(getClassName(), null, "No data found for record: " + farmerId);
                return PluginResponse.error("No data found for record: " + farmerId);
            }

            // Convert to GovStack JSON using metadata-driven encoder
            String govStackJson = jsonEncoder.encodeToGovStackJson(formData);

            if (govStackJson == null) {
                LogUtil.error(getClassName(), null, "Failed to build GovStack JSON");
                return PluginResponse.error("Failed to build GovStack JSON");
            }

            if (logJsonPayload) {
                LogUtil.info(getClassName(), "GovStack JSON payload:\n" + govStackJson);
            }

            // Validate if required
            if (validateBeforeSending) {
                if (!validateData(formData)) {
                    LogUtil.error(getClassName(), null, "Data validation failed");
                    return PluginResponse.error("Data validation failed - missing required fields");
                }
            }

            // Wrap in test data format if needed (for compatibility with ProcessingAPI)
            boolean useTestDataFormat = "true".equals(getPropertyString("useTestDataFormat", properties));
            if (useTestDataFormat) {
                govStackJson = jsonEncoder.wrapInTestDataFormat(govStackJson);
                LogUtil.info(getClassName(), "Wrapped JSON in test data format");
            }

            // Send to API
            GovStackApiClient apiClient = new GovStackApiClient(apiEndpoint, apiId, apiKey);

            // Configure timeouts
            String connectionTimeoutStr = getPropertyString("connectionTimeout", properties, "30");
            String readTimeoutStr = getPropertyString("readTimeout", properties, "60");

            int connectionTimeout = 30; // default
            int readTimeout = 60; // default

            try {
                if (connectionTimeoutStr != null && !connectionTimeoutStr.trim().isEmpty()) {
                    connectionTimeout = Integer.parseInt(connectionTimeoutStr);
                }
            } catch (NumberFormatException e) {
                LogUtil.warn(getClassName(), "Invalid connectionTimeout value, using default: 30");
            }

            // Configure API client timeouts
            try {
                if (connectionTimeoutStr != null && !connectionTimeoutStr.trim().isEmpty()) {
                    connectionTimeout = Integer.parseInt(connectionTimeoutStr);
                    apiClient.setConnectionTimeout(connectionTimeout * 1000);
                }
                if (readTimeoutStr != null && !readTimeoutStr.trim().isEmpty()) {
                    readTimeout = Integer.parseInt(readTimeoutStr);
                    apiClient.setReadTimeout(readTimeout * 1000);
                }
            } catch (NumberFormatException e) {
                LogUtil.warn(getClassName(), "Invalid timeout value, using defaults");
            }

            // Send to API
            GovStackApiClient.ApiResponse apiResponse = apiClient.sendToGovStack(govStackJson);

            if (apiResponse.isSuccess()) {
                LogUtil.info(getClassName(), "Successfully sent data to GovStack API");

                // Update workflow variables if configured
                if (updateWorkflowStatus) {
                    WorkflowAssignment assignment = (WorkflowAssignment) properties.get("workflowAssignment");
                    if (assignment != null) {
                        updateWorkflowVariables(assignment, apiResponse);
                    }
                }

                return PluginResponse.success("Successfully processed and sent registration data with ID: " + farmerId);
            } else {
                LogUtil.error(getClassName(), null, "API call failed: " + apiResponse.getMessage());
                return PluginResponse.error("Failed to send data: " + apiResponse.getMessage());
            }

        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Error in GovStack Farmer Data Submitter Plugin");
            return PluginResponse.error("Error: " + e.getMessage());
        }
    }

    /**
     * Get property string value with default
     */
    private String getPropertyString(String property, Map properties) {
        return getPropertyString(property, properties, "");
    }

    private String getPropertyString(String property, Map properties, String defaultValue) {
        Object value = properties.get(property);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Initialize metadata-driven services
     */
    private void initializeServices() {
        if (metadataService == null) {
            metadataService = new YamlMetadataService();
        }
        if (dataExtractor == null) {
            dataExtractor = new GenericFormDataExtractor(metadataService);
        }
        if (jsonEncoder == null) {
            jsonEncoder = new GovStackJsonEncoder(metadataService);
        }
        LogUtil.info(getClassName(), "Initialized metadata-driven services");
    }

    /**
     * Validate form data completeness
     */
    private boolean validateData(Map<String, Object> formData) {
        // Basic validation - check if we have any data
        if (formData == null || formData.isEmpty()) {
            LogUtil.warn(getClassName(), "Form data is empty");
            return false;
        }

        // Check for at least one section with data
        boolean hasData = false;
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            if (entry.getValue() != null &&
                ((entry.getValue() instanceof Map && !((Map) entry.getValue()).isEmpty()) ||
                 (entry.getValue() instanceof List && !((List) entry.getValue()).isEmpty()))) {
                hasData = true;
                break;
            }
        }

        if (!hasData) {
            LogUtil.warn(getClassName(), "No actual data found in any form section");
            return false;
        }

        // TODO: Implement field-level validation using metadata required flags

        return true;
    }

    /**
     * Update workflow variables with API response
     */
    private void updateWorkflowVariables(WorkflowAssignment assignment, GovStackApiClient.ApiResponse response) {
        try {
            ApplicationContext appContext = AppUtil.getApplicationContext();
            WorkflowManager workflowManager = (WorkflowManager) appContext.getBean("workflowManager");

            if (workflowManager != null) {
                String processId = assignment.getProcessId();

                // Set workflow variables
                workflowManager.processVariable(processId, "submissionStatus", response.isSuccess() ? "success" : "failed");
                workflowManager.processVariable(processId, "applicationId", response.getApplicationId());
                workflowManager.processVariable(processId, "submissionMessage", response.getMessage());

                LogUtil.info(getClassName(), "Updated workflow variables for process: " + processId);
            }
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Error updating workflow variables");
        }
    }


    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public String getDescription() {
        return "Extracts farmer registration data from Joget forms, converts to GovStack JSON format, and sends to Processing Server API.";
    }

    @Override
    public String getLabel() {
        return "GovStack Farmer Data Submitter";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/DocSubmitter.json", null, true, null);
    }

    /**
     * Get plugin icon
     */
    public String getIcon() {
        return "fa fa-upload";
    }

    /**
     * Get plugin category
     */
    public String getCategory() {
        return "GovStack Registration Building Blocks";
    }

}