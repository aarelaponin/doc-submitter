package global.govstack.farmreg.registration.lib;

import global.govstack.farmreg.registration.model.PluginResponse;
import org.joget.apps.app.service.AppUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.commons.util.LogUtil;
import org.springframework.context.ApplicationContext;

import java.util.Map;

/**
 * GovStack Registration Building Block Plugin for sending Document
 *
 * This plugin from specific document form creates generalised json
 * document, which can be sent to the Digital Registry or to Registration
 *
 * @author GovStack Registration BB Team
 * @version 1.0
 */
public class DocSubmitter extends DefaultApplicationPlugin {

    private static final String PLUGIN_NAME = "Registration Building Block Document Sender";
    private static final String PLUGIN_VERSION = "1.0.0";

    /**
     * Plugin execution entry point
     * Called when the plugin is executed as a process tool activity
     */
    @Override
    public Object execute(Map properties) {
        LogUtil.info(getClassName(), "Executing Registration Building Block Document Sender Plugin");

        try {
            // Get workflow assignment and context
            WorkflowAssignment workflowAssignment = (WorkflowAssignment) properties.get("workflowAssignment");
            if (workflowAssignment == null) {
                LogUtil.warn(getClassName(), "WorkflowAssignment is null");
                return null;
            }

            // Extract taxpayer information from workflow variables
            String taxpayerId = getProcessIdFromWorkflow(workflowAssignment);
            if (taxpayerId == null || taxpayerId.trim().isEmpty()) {
                LogUtil.warn(getClassName(), "Document ID not found in workflow variables");
                return null;
            }

            // Perform risk assessment
            PluginResponse workResult = performWork(taxpayerId, properties);

            if (workResult != null) {
                // Update workflow variables with assessment results
                LogUtil.info(getClassName(), "Document sending is completed: " + taxpayerId);
            } else {
                LogUtil.warn(getClassName(), "Document sending failed for document: " + taxpayerId);
            }

            return workResult;

        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Error in Registration Building Block Document Sender Plugin");
            return null;
        }
    }

    /**
     * Perform the core risk assessment using the 6-Factor Probability Model
     */
    private PluginResponse performWork(String taxpayerId, Map properties) {
        try {
            LogUtil.info(getClassName(), "Starting document processing for document: " + taxpayerId);

            // Create risk assessment request

            LogUtil.info(getClassName(), "Work completed");

            return PluginResponse.success("Success");

        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Error performing risk assessment for taxpayer: " + taxpayerId);
            return PluginResponse.error("Internal error during risk assessment: " + e.getMessage());
        }
    }

    /**
     * Extract taxpayer ID from workflow variables
     */
    private String getProcessIdFromWorkflow(WorkflowAssignment assignment) {
        try {
            // Try to get from workflow variable
            String processId = assignment.getProcessId();

            // Alternative: Get from form data
            if (processId == null) {
                ApplicationContext appContext = AppUtil.getApplicationContext();
                WorkflowManager workflowManager = (WorkflowManager) appContext.getBean("workflowManager");
            }

            return processId;

        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Error extracting process ID from workflow");
            return null;
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
        return "Registration Building Block Document Sender for generalising specific document into json stream.";
    }

    @Override
    public String getLabel() {
        return "GovStack Document sender";
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
        return "fa fa-calculator";
    }

    /**
     * Get plugin category
     */
    public String getCategory() {
        return "GovStack Registration Building Blocks";
    }

}