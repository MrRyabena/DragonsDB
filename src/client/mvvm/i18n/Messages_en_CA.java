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
            {"main.user", "User"}
        };
    }
}
