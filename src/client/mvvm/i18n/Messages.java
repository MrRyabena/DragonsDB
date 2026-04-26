package client.mvvm.i18n;

import java.util.ListResourceBundle;

public class Messages extends ListResourceBundle {
    @Override
    protected Object[][] getContents() {
        return new Object[][] {
            {"app.title", "DragonsDB"},
            {"auth.login", "Login"},
            {"auth.register", "Register"},
            {"auth.user", "User"},
            {"common.error", "Error"},
            {"main.refresh", "Refresh"},
            {"main.add", "Add"},
            {"main.edit", "Edit"},
            {"main.delete", "Delete"},
            {"main.logout", "Logout"},
            {"main.filter", "Filter"},
            {"main.command", "Command"},
            {"main.user", "User"}
        };
    }
}
