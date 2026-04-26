package client.mvvm;

import java.util.Scanner;

import client.RequestClient;
import client.mvvm.service.GatewayResult;

/**
 * Small CLI demo for the new MVVM stack. It allows validation of ViewModel wiring
 * before JavaFX views are introduced.
 */
public class MvvmClientDemo {
    public static void main(String[] args) {
        String host = System.getenv("SERVER_HOST");
        if (host == null || host.isBlank()) {
            host = core.Defaults.SERVER_HOST;
        }

        int port = core.Defaults.SERVER_PORT;
        String portEnv = System.getenv("SERVER_PORT");
        if (portEnv != null) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException ignored) {
            }
        }

        try (RequestClient requestClient = new RequestClient(host, port);
                Scanner scanner = new Scanner(System.in)) {
            ClientMvvmContext context = new ClientMvvmContext(requestClient);

            System.out.print("login: ");
            String login = scanner.nextLine();
            System.out.print("password: ");
            String password = scanner.nextLine();

            GatewayResult auth = context.getAuthViewModel().login(login, password);
            if (!auth.isSuccess()) {
                System.err.println("auth failed: " + auth.message);
                return;
            }

            GatewayResult refreshed = context.getMainViewModel().refresh();
            if (!refreshed.isSuccess()) {
                System.err.println("refresh failed: " + refreshed.message);
                return;
            }

            System.out.println("current user: " + context.getMainViewModel().getCurrentUser());
            System.out.println("rows: " + context.getMainViewModel().getTableViewModel().getVisibleRows().size());
        } catch (Exception e) {
            System.err.println("MVVM demo error: " + e.getMessage());
        }
    }
}
