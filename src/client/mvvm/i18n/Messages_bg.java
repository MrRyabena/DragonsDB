package client.mvvm.i18n;

import java.util.ListResourceBundle;

public class Messages_bg extends ListResourceBundle {
    @Override
    protected Object[][] getContents() {
        return new Object[][] {
            {"app.title", "DragonsDB"},
            {"auth.login", "Вход"},
            {"auth.register", "Регистрация"},
            {"auth.user", "Потребител"},
            {"common.error", "Грешка"},
            {"main.refresh", "Обновяване"},
            {"main.add", "Добавяне"},
            {"main.edit", "Редакция"},
            {"main.delete", "Изтриване"},
            {"main.logout", "Изход"},
            {"main.filter", "Филтър"},
            {"main.command", "Команда"},
            {"main.user", "Потребител"}
        };
    }
}
