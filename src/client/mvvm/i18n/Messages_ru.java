package client.mvvm.i18n;

import java.util.ListResourceBundle;

public class Messages_ru extends ListResourceBundle {
    @Override
    protected Object[][] getContents() {
        return new Object[][] {
            {"app.title", "DragonsDB"},
            {"auth.login", "Вход"},
            {"auth.register", "Регистрация"},
            {"auth.user", "Пользователь"},
            {"common.error", "Ошибка"},
            {"main.refresh", "Обновить"},
            {"main.add", "Добавить"},
            {"main.edit", "Изменить"},
            {"main.delete", "Удалить"},
            {"main.logout", "Выйти"},
            {"main.filter", "Фильтр"},
            {"main.command", "Команда"},
            {"main.user", "Пользователь"}
        };
    }
}
