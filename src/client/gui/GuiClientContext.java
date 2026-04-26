package client.gui;

import client.RequestClient;
import client.mvvm.ClientMvvmContext;
import client.mvvm.service.LocalizationService;
import client.mvvm.vm.AuthViewModel;
import client.mvvm.vm.MainViewModel;

/** Composition root for the JavaFX client layer. */
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
