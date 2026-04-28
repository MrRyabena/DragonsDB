package client.gui;

import client.RequestClient;
import client.mvvm.ClientMvvmContext;
import client.mvvm.service.LocalizationService;
import client.mvvm.vm.AuthViewModel;
import client.mvvm.vm.MainViewModel;

/**
 * Dependency injection container for the GUI layer.
 *
 * <p>Creates and provides access to:
 * <ul>
 *   <li>RequestClient for server communication
 *   <li>All ViewModels (Auth, Main with sub-VMs)
 *   <li>All shared services (Localization, Format, Query)
 *   <li>State stores (DragonStore, ClientSessionState)
 * </ul>
 *
 * <p>Implements AutoCloseable to ensure RequestClient socket is properly closed.
 */
public class GuiClientContext implements AutoCloseable {
    private final RequestClient requestClient;
    private final ClientMvvmContext mvvmContext;

    public GuiClientContext(String host, int port) {
        this.requestClient = new RequestClient(host, port);
        this.mvvmContext = new ClientMvvmContext(requestClient);
    }

    public AuthViewModel getAuthViewModel() {
        return mvvmContext.getAuthViewModel();
    }

    public MainViewModel getMainViewModel() {
        return mvvmContext.getMainViewModel();
    }

    public LocalizationService getLocalizationService() {
        return mvvmContext.getLocalizationService();
    }

    @Override
    public void close() {
        mvvmContext.getMainViewModel().dispose();
        requestClient.close();
    }
}
