/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/

package org.eclipse.che.ide.bootstrap;

import com.google.gwt.core.client.Callback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.workspace.gwt.client.WorkspaceServiceClient;
import org.eclipse.che.api.workspace.shared.dto.CommandDto;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.MachineConfigDto;
import org.eclipse.che.api.workspace.shared.dto.MachineSourceDto;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.loader.LoaderPresenter;
import org.eclipse.che.ide.loader.OperationInfo;
import org.eclipse.che.ide.loader.OperationInfo.Status;
import org.eclipse.che.ide.util.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class WorkspaceComponent implements Component {

    private static final String RECIPE_URL =
            "https://gist.githubusercontent.com/vparfonov/5c633534bfb0c127854f/raw/f176ee3428c2d39d08c7b4762aee6855dc5c8f75/jdk8_maven3_tomcat8";

    private final WorkspaceServiceClient   workspaceServiceClient;
    private final CoreLocalizationConstant localizedConstants;
    private final LoaderPresenter          loader;
    private final AppContext               appContext;
    private final DtoFactory               dtoFactory;

    @Inject
    public WorkspaceComponent(WorkspaceServiceClient workspaceServiceClient,
                              CoreLocalizationConstant localizedConstants,
                              LoaderPresenter loader,
                              AppContext appContext,
                              DtoFactory dtoFactory) {
        this.workspaceServiceClient = workspaceServiceClient;
        this.localizedConstants = localizedConstants;
        this.loader = loader;
        this.appContext = appContext;
        this.dtoFactory = dtoFactory;
    }

    @Override
    public void start(final Callback<Component, Exception> callback) {
        final OperationInfo getWsOperation = new OperationInfo(localizedConstants.gettingWorkspace(), Status.IN_PROGRESS, loader);
        loader.show(getWsOperation);
        workspaceServiceClient.getWorkspaces(0, 1).then(new Operation<List<UsersWorkspaceDto>>() {
            @Override
            public void apply(List<UsersWorkspaceDto> arg) throws OperationException {
                if (!arg.isEmpty()) {
                    getWsOperation.setStatus(Status.FINISHED);
                    Config.setCurrentWorkspace(arg.get(0));
                    appContext.setWorkspace(arg.get(0));
                    callback.onSuccess(WorkspaceComponent.this);
                } else {
                    createWorkspace(callback, getWsOperation);
                }
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                getWsOperation.setStatus(Status.ERROR);
                callback.onFailure(new Exception(arg.getCause()));
            }
        });
    }

    private void createWorkspace(final Callback<Component, Exception> callback, final OperationInfo getWsOperation) {
        WorkspaceConfigDto workspaceConfig = getWorkspaceConfig();
        UsersWorkspaceDto usersWorkspaceDto = dtoFactory.createDto(UsersWorkspaceDto.class)
                                                        .withName(workspaceConfig.getName())
                                                        .withAttributes(workspaceConfig.getAttributes())
                                                        .withCommands(workspaceConfig.getCommands())
                                                        .withEnvironments(workspaceConfig.getEnvironments())
                                                        .withDefaultEnvName(workspaceConfig.getDefaultEnvName())
                                                        .withTemporary(true);
        final OperationInfo createWsOperation = new OperationInfo(localizedConstants.creatingWorkspace(), Status.IN_PROGRESS, loader);
        loader.print(createWsOperation);
        workspaceServiceClient.create(usersWorkspaceDto, null).then(new Operation<UsersWorkspaceDto>() {
            @Override
            public void apply(UsersWorkspaceDto arg) throws OperationException {
                getWsOperation.setStatus(Status.FINISHED);
                createWsOperation.setStatus(Status.FINISHED);
                startWorkspace(arg.getId(), arg.getDefaultEnvName(), callback);
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                getWsOperation.setStatus(Status.ERROR);
                createWsOperation.setStatus(Status.ERROR);
                callback.onFailure(new Exception(arg.getCause()));
            }
        });
    }

    private void startWorkspace(String id, String envName, final Callback<Component, Exception> callback) {
        final OperationInfo startWsOperation =
                new OperationInfo(localizedConstants.startingOperation("workspace"), Status.IN_PROGRESS, loader);
        loader.print(startWsOperation);
        workspaceServiceClient.startById(id, envName).then(new Operation<UsersWorkspaceDto>() {
            @Override
            public void apply(UsersWorkspaceDto arg) throws OperationException {
                startWsOperation.setStatus(Status.FINISHED);
                Config.setCurrentWorkspace(arg);
                appContext.setWorkspace(arg);
                callback.onSuccess(WorkspaceComponent.this);
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                startWsOperation.setStatus(Status.ERROR);
                callback.onFailure(new Exception(arg.getCause()));
            }
        });
    }

    private WorkspaceConfigDto getWorkspaceConfig() {
        List<MachineConfigDto> machineConfigs = new ArrayList<>();
        machineConfigs.add(dtoFactory.createDto(MachineConfigDto.class)
                                     .withName("dev-machine")
                                     .withType("docker")
                                     .withSource(dtoFactory.createDto(MachineSourceDto.class)
                                                           .withType("recipe")
                                                           .withLocation(RECIPE_URL))
                                     .withDev(true)
                                     .withMemorySize(512));

        Map<String, EnvironmentDto> environments = new HashMap<>();
        environments.put("dev-env", dtoFactory.createDto(EnvironmentDto.class)
                                              .withName("dev-env")
                                              .withMachineConfigs(machineConfigs));

        List<CommandDto> commands = new ArrayList<>();
        commands.add(dtoFactory.createDto(CommandDto.class)
                               .withName("MCI")
                               .withCommandLine("mvn clean install"));

        Map<String, String> attrs = new HashMap<>();
        attrs.put("fake_attr", "attr_value");

        return dtoFactory.createDto(WorkspaceConfigDto.class)
                         .withName("dev-cfg")
                         .withDefaultEnvName("dev-env")
                         .withEnvironments(environments)
                         .withCommands(commands)
                         .withAttributes(attrs);
    }
}
