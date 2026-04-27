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
            {"main.user", "Потребител"},
            {"error.title", "Грешка"},
            {"error.unknown", "Неизвестна грешка"},
            {"error.edit.header", "Редакция на дракон"},
            {"error.edit.selectDragon", "Изберете дракон за редакция"},
            {"error.command.header", "Грешка при изпълнение на команда"},
            {"error.dialog.header", "Грешка в диалога"},
            {"error.dialog.openFailed", "Диалогът не можа да се отвори"},
            {"error.operation.header", "Операцията е неуспешна"}
        };
    }
}
