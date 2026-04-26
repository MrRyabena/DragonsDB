package client.mvvm.vm;

import client.mvvm.model.Credentials;
import client.mvvm.service.ClientGateway;
import client.mvvm.service.DragonsQueryService;
import client.mvvm.service.GatewayResult;
import client.mvvm.service.ParameterValueProvider;
import client.mvvm.state.ClientSessionState;
import client.mvvm.state.DragonStore;

/** Main screen orchestrator that coordinates table, visualization and command execution. */
public class MainViewModel extends BaseViewModel {
    private final ClientGateway gateway;
    private final ClientSessionState sessionState;
    private final DragonStore dragonStore;

    private final DragonsTableViewModel tableViewModel;
    private final VisualizationViewModel visualizationViewModel;

    public MainViewModel(
            ClientGateway gateway, ClientSessionState sessionState, DragonStore dragonStore) {
        this.gateway = gateway;
        this.sessionState = sessionState;
        this.dragonStore = dragonStore;

        this.tableViewModel = new DragonsTableViewModel(dragonStore, new DragonsQueryService());
        this.visualizationViewModel = new VisualizationViewModel(dragonStore, sessionState);
        this.tableViewModel.bind();
        this.visualizationViewModel.bind();
    }

    public DragonsTableViewModel getTableViewModel() {
        return tableViewModel;
    }

    public VisualizationViewModel getVisualizationViewModel() {
        return visualizationViewModel;
    }

    public String getCurrentUser() {
        return sessionState.getCurrentUser();
    }

    public GatewayResult refresh() {
        return executeCommand("show", null);
    }

    public GatewayResult executeCommand(String command, ParameterValueProvider parameterProvider) {
        clearError();
        setBusy(true);
        try {
            Credentials credentials = sessionState.getCredentials();
            GatewayResult result = gateway.sendCommand(command, credentials, parameterProvider);
            if (result.isSuccess()) {
                if (result.dragons != null) {
                    dragonStore.replaceAll(result.dragons);
                }
            } else {
                setErrorMessage(result.message);
            }
            return result;
        } finally {
            setBusy(false);
        }
    }

    public void dispose() {
        tableViewModel.unbind();
        visualizationViewModel.unbind();
    }
}
