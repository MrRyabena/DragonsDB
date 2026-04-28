package client.mvvm;

import java.util.Locale;

import client.RequestClient;
import client.mvvm.service.ClientGateway;
import client.mvvm.service.LocalizationService;
import client.mvvm.service.RequestClientGateway;
import client.mvvm.state.ClientSessionState;
import client.mvvm.state.DragonStore;
import client.mvvm.vm.AuthViewModel;
import client.mvvm.vm.MainViewModel;

/**
 * Dependency injection container for MVVM layer classes.
 *
 * <p>Creates and provides centralized access to:
 * <ul>
 *   <li>State stores (ClientSessionState, DragonStore)
 *   <li>Services (LocalizationService, ClientGateway, ClientGateway)
 *   <li>ViewModels (AuthViewModel, MainViewModel with child VMs)
 * </ul>
 *
 * <p>All instances are singletons created once per client session.
 */
public class ClientMvvmContext {
    private final LocalizationService localizationService;
    private final ClientSessionState sessionState;
    private final DragonStore dragonStore;
    private final ClientGateway gateway;

    private final AuthViewModel authViewModel;
    private final MainViewModel mainViewModel;

    public ClientMvvmContext(RequestClient requestClient) {
        this.localizationService = new LocalizationService();
        this.sessionState = new ClientSessionState();
        this.dragonStore = new DragonStore();
        this.gateway = new RequestClientGateway(requestClient);

        this.authViewModel = new AuthViewModel(gateway, sessionState);
        this.mainViewModel = new MainViewModel(gateway, sessionState, dragonStore);

        // Default locale from assignment list.
        localizationService.setCurrentLocale(Locale.forLanguageTag("en-CA"));
    }

    public LocalizationService getLocalizationService() {
        return localizationService;
    }

    public AuthViewModel getAuthViewModel() {
        return authViewModel;
    }

    public MainViewModel getMainViewModel() {
        return mainViewModel;
    }
}
