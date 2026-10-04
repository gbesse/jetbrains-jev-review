package org.jev.review;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.ui.Messages;
import java.util.concurrent.CompletableFuture;

public final class ReviewSelectionAction extends AnAction {
    private static final CredentialAttributes KEY = new CredentialAttributes("jev.jetbrains.review", "typesafe-api-key");

    @Override public void update(AnActionEvent event) {
        var editor = event.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);
        event.getPresentation().setEnabledAndVisible(editor != null && editor.getSelectionModel().hasSelection());
    }

    @Override public void actionPerformed(AnActionEvent event) {
        var editor = event.getRequiredData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);
        var project = event.getProject(); var selection = editor.getSelectionModel();
        var document = editor.getDocument(); long stamp = document.getModificationStamp();
        var lines = ReviewCore.selectedLines(selection.getSelectedText(), document.getLineNumber(selection.getSelectionStart()), selection.getSelectionStart());
        var credentials = PasswordSafe.getInstance().get(KEY);
        String apiKey = credentials == null ? null : credentials.getPasswordAsString();
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = Messages.showPasswordDialog(project, "TypeSafe API key (stored in JetBrains PasswordSafe)", "Jev Review", null);
            if (apiKey == null || apiKey.isBlank()) return;
            PasswordSafe.getInstance().set(KEY, new Credentials("typesafe", apiKey));
        }
        String finalApiKey = apiKey;
        CompletableFuture.supplyAsync(() -> { try { return ReviewCore.findings(TypeSafeClient.decide(ReviewCore.request(lines), finalApiKey), lines, .75); } catch (Exception error) { throw new RuntimeException(error); } })
            .whenComplete((findings, error) -> ApplicationManager.getApplication().invokeLater(() -> {
                if (error != null) { notify(project, error.getCause() == null ? error.getMessage() : error.getCause().getMessage(), NotificationType.ERROR); return; }
                if (document.getModificationStamp() != stamp) { notify(project, "Selection changed while review was running; stale findings were discarded.", NotificationType.WARNING); return; }
                var highlighters = new java.util.ArrayList<com.intellij.openapi.editor.markup.RangeHighlighter>();
                for (var finding : findings) HighlightManager.getInstance(project).addRangeHighlight(editor, finding.line().startOffset(), finding.line().endOffset(), EditorColors.WARNING_ATTRIBUTES, false, highlighters);
                notify(project, findings.isEmpty() ? "No declared issue cleared the threshold." : findings.size() + " exact line(s) highlighted: " + findings.stream().map(ReviewCore.Finding::label).toList(), findings.isEmpty() ? NotificationType.INFORMATION : NotificationType.WARNING);
            }));
    }

    private static void notify(com.intellij.openapi.project.Project project, String message, NotificationType type) {
        NotificationGroupManager.getInstance().getNotificationGroup("Jev Review").createNotification(message, type).notify(project);
    }
}
