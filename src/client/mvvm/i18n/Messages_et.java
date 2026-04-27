package client.mvvm.i18n;

import java.util.ListResourceBundle;

public class Messages_et extends ListResourceBundle {
    @Override
    protected Object[][] getContents() {
        return new Object[][] {
            {"app.title", "DragonsDB"},
            {"auth.login", "Sisselogimine"},
            {"auth.register", "Registreerimine"},
            {"auth.user", "Kasutaja"},
            {"common.error", "Viga"},
            {"main.refresh", "Värskenda"},
            {"main.add", "Lisa"},
            {"main.edit", "Muuda"},
            {"main.delete", "Kustuta"},
            {"main.logout", "Logi välja"},
            {"main.filter", "Filter"},
            {"main.command", "Käsk"},
            {"main.user", "Kasutaja"},
            {"error.title", "Viga"},
            {"error.unknown", "Tundmatu viga"},
            {"error.edit.header", "Draakoni muutmine"},
            {"error.edit.selectDragon", "Vali muutmiseks draakon"},
            {"error.command.header", "Käsu täitmine nurjus"},
            {"error.dialog.header", "Dialoogi viga"},
            {"error.dialog.openFailed", "Dialoogi avamine ebaõnnestus"},
            {"error.operation.header", "Toiming ebaõnnestus"}
        };
    }
}
