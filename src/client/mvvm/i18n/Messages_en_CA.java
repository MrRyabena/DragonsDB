package client.mvvm.i18n;

import java.util.ListResourceBundle;

public class Messages_en_CA extends ListResourceBundle {
    @Override
    protected Object[][] getContents() {
        return new Object[][] {
            {"app.title", "DragonsDB"},
            {"auth.login", "Sign In"},
            {"auth.register", "Sign Up"},
            {"auth.user", "User"},
            {"common.error", "Error"},
            {"main.refresh", "Refresh"},
            {"main.add", "Add"},
            {"main.edit", "Edit"},
            {"main.delete", "Delete"},
            {"main.logout", "Sign Out"},
            {"main.filter", "Filter"},
            {"main.command", "Command"},
            {"main.user", "User"},
            {"error.title", "Error"},
            {"error.unknown", "Unknown error"},
            {"error.edit.header", "Edit dragon"},
            {"error.edit.selectDragon", "Select a dragon to edit"},
            {"error.command.header", "Command failed"},
            {"error.dialog.header", "Dialog error"},
            {"error.dialog.openFailed", "Failed to open dialog"},
            {"error.operation.header", "Operation failed"}
        };
    }
}
