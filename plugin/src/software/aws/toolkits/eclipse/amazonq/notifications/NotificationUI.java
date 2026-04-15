// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

/**
 * Renders notification dialogs on the SWT UI thread.
 *
 * <p>Maps notification severity to SWT dialog icons and renders the {@code en-US}
 * locale title and description. Notification actions are rendered as dialog buttons;
 * {@code openUrl} actions open the specified URL in the system default browser via
 * {@link PluginUtils#openWebpage(String)}.</p>
 */
public final class NotificationUI {

    private NotificationUI() {
        // Utility class — prevent instantiation
    }

    /**
     * Displays a notification to the user as a {@link MessageDialog}.
     *
     * <p>The dialog is shown asynchronously on the SWT UI thread via
     * {@link Display#asyncExec(Runnable)}. If the notification content or
     * {@code en-US} locale is missing, the notification is silently skipped.</p>
     *
     * @param notification the notification to display
     */
    public static void showNotification(final NotificationData notification) {
        showNotification(notification, null);
    }

    /**
     * Displays a notification to the user as a {@link MessageDialog}, invoking
     * the given callback when the notification is dismissed.
     *
     * <p>The dismissal callback is invoked for all dialog close scenarios:
     * clicking the "Dismiss" button, clicking the window close button (X),
     * or clicking any action button. The callback receives the notification ID.</p>
     *
     * @param notification      the notification to display
     * @param onDismissCallback optional callback invoked with the notification ID
     *                          when the dialog is closed; may be {@code null}
     */
    public static void showNotification(final NotificationData notification,
                                        final Consumer<String> onDismissCallback) {
        if (notification == null) {
            return;
        }

        // Extract en-US content; skip if missing
        NotificationContentDescription content = resolveContent(notification);
        if (content == null) {
            return;
        }

        String title = content.getTitle() != null ? content.getTitle() : "Amazon Q Notification";
        String description = content.getDescription() != null ? content.getDescription() : "";

        // Resolve actions and their labels
        List<NotificationFollowupActions> validActions = resolveActions(notification);
        String[] buttonLabels = buildButtonLabels(validActions);

        int dialogImageType = mapSeverityToIcon(notification.getSeverity());

        Display.getDefault().asyncExec(() -> {
            try {
                Shell shell = Display.getDefault().getActiveShell();
                if (shell == null) {
                    shell = new Shell(Display.getDefault());
                }

                MessageDialog dialog = new MessageDialog(
                        shell,
                        title,
                        null, // default title image
                        description,
                        dialogImageType,
                        buttonLabels,
                        buttonLabels.length - 1 // default button is the last one ("Dismiss")
                );

                int result = dialog.open();
                handleActionResult(result, validActions);

                // Invoke dismissal callback for all close scenarios
                if (onDismissCallback != null && notification.getId() != null) {
                    onDismissCallback.accept(notification.getId());
                }
            } catch (Exception e) {
                Activator.getLogger().warn("Failed to show notification dialog", e);
            }
        });
    }

    /**
     * Extracts the {@code en-US} content from a notification.
     *
     * @return the content description, or {@code null} if not available
     */
    private static NotificationContentDescription resolveContent(final NotificationData notification) {
        if (notification.getContent() == null) {
            return null;
        }
        return notification.getContent().getLocale();
    }

    /**
     * Collects actions that have valid {@code en-US} locale content with a title.
     */
    private static List<NotificationFollowupActions> resolveActions(final NotificationData notification) {
        List<NotificationFollowupActions> validActions = new ArrayList<>();
        if (notification.getActions() == null) {
            return validActions;
        }
        for (NotificationFollowupActions action : notification.getActions()) {
            NotificationActionDescription actionDesc = resolveActionDescription(action);
            if (actionDesc != null && actionDesc.getTitle() != null && !actionDesc.getTitle().isEmpty()) {
                validActions.add(action);
            }
        }
        return validActions;
    }

    /**
     * Extracts the {@code en-US} action description from a followup action.
     */
    private static NotificationActionDescription resolveActionDescription(final NotificationFollowupActions action) {
        if (action == null || action.getContent() == null) {
            return null;
        }
        return action.getContent().getLocale();
    }

    /**
     * Builds the button label array for the dialog. Action titles come first,
     * followed by a "Dismiss" button.
     */
    private static String[] buildButtonLabels(final List<NotificationFollowupActions> validActions) {
        String[] labels = new String[validActions.size() + 1];
        for (int i = 0; i < validActions.size(); i++) {
            labels[i] = resolveActionDescription(validActions.get(i)).getTitle();
        }
        labels[validActions.size()] = "Dismiss";
        return labels;
    }

    /**
     * Handles the user's button click. If the clicked button corresponds to an
     * {@code openUrl} action, opens the URL in the system browser.
     */
    private static void handleActionResult(final int result, final List<NotificationFollowupActions> validActions) {
        if (result < 0 || result >= validActions.size()) {
            // User clicked "Dismiss" or closed the dialog
            return;
        }

        NotificationFollowupActions clickedAction = validActions.get(result);
        if ("openUrl".equals(clickedAction.getType())) {
            NotificationActionDescription actionDesc = resolveActionDescription(clickedAction);
            if (actionDesc != null && actionDesc.getUrl() != null && !actionDesc.getUrl().isEmpty()) {
                PluginUtils.openWebpage(actionDesc.getUrl());
            }
        }
    }

    /**
     * Maps a notification severity string to the corresponding SWT dialog image type.
     *
     * @param severity the severity string ({@code "Critical"}, {@code "Warning"}, or {@code "Info"})
     * @return the SWT icon constant
     */
    private static int mapSeverityToIcon(final String severity) {
        if (severity == null) {
            return MessageDialog.INFORMATION;
        }
        switch (severity) {
            case "Critical":
                return MessageDialog.ERROR;
            case "Warning":
                return MessageDialog.WARNING;
            case "Info":
            default:
                return MessageDialog.INFORMATION;
        }
    }
}
