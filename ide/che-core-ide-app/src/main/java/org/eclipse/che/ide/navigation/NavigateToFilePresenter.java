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
package org.eclipse.che.ide.navigation;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.websocket.Message;
import org.eclipse.che.ide.websocket.MessageBuilder;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.RequestCallback;
import org.eclipse.che.ide.websocket.rest.Unmarshallable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.gwt.http.client.RequestBuilder.GET;
import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;

/**
 * Presenter for file navigation (find file by name and open it).
 *
 * @author Ann Shumilova
 * @author Artem Zatsarynnyy
 */
@Singleton
public class NavigateToFilePresenter implements NavigateToFileView.ActionDelegate {

    private final String                      SEARCH_URL;
    private       MessageBus                  wsMessageBus;
    private       DtoUnmarshallerFactory      dtoUnmarshallerFactory;
    private       DialogFactory               dialogFactory;
    private       CoreLocalizationConstant    localizationConstant;
    private final NewProjectExplorerPresenter projectExplorer;
    private       NavigateToFileView          view;
    private       AppContext                  appContext;
    private       EventBus                    eventBus;
    private       Map<String, ItemReference>  resultMap;

    @Inject
    public NavigateToFilePresenter(NavigateToFileView view,
                                   AppContext appContext,
                                   EventBus eventBus,
                                   MessageBus wsMessageBus,
                                   @Named("workspaceId") String workspaceId,
                                   DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                   DialogFactory dialogFactory,
                                   CoreLocalizationConstant localizationConstant,
                                   NewProjectExplorerPresenter projectExplorer) {
        this.view = view;
        this.appContext = appContext;
        this.eventBus = eventBus;
        this.wsMessageBus = wsMessageBus;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.dialogFactory = dialogFactory;
        this.localizationConstant = localizationConstant;
        this.projectExplorer = projectExplorer;

        resultMap = new HashMap<>();

        SEARCH_URL = "/project/" + workspaceId + "/search";
        view.setDelegate(this);
    }

    /** Show dialog with view for navigation. */
    public void showDialog() {
        view.showDialog();
        view.clearInput();
    }

    /** {@inheritDoc} */
    @Override
    public void onRequestSuggestions(String query, final AsyncCallback<List<ItemReference>> callback) {
        resultMap = new HashMap<>();

        // add '*' to allow search files by first letters
        search(query + "*", new AsyncCallback<List<ItemReference>>() {
            @Override
            public void onSuccess(List<ItemReference> result) {
                for (ItemReference item : result) {
                    final String path = item.getPath();
                    // skip hidden items
                    if (!isItemHidden(path)) {
                        resultMap.put(path, item);
                    }
                }
                List itemReference = new ArrayList<>(resultMap.values());
                callback.onSuccess(itemReference);
            }

            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onFileSelected() {
        view.close();
        final ItemReference selectedItem = resultMap.get(view.getItemPath());

        HasStorablePath selectedPath = new HasStorablePath() {
            @Nonnull
            @Override
            public String getStorablePath() {
                return selectedItem.getPath();
            }
        };

        projectExplorer.navigate(selectedPath, true, true);
    }

    private void search(String fileName, final AsyncCallback<List<ItemReference>> callback) {
        final String projectPath = appContext.getCurrentProject().getRootProject().getPath();
        final String url = SEARCH_URL + projectPath + "/?name=" + URL.encodePathSegment(fileName);
        Message message = new MessageBuilder(GET, url).header(ACCEPT, APPLICATION_JSON).build();
        Unmarshallable<List<ItemReference>> unmarshaller = dtoUnmarshallerFactory.newWSListUnmarshaller(ItemReference.class);
        try {
            wsMessageBus.send(message, new RequestCallback<List<ItemReference>>(unmarshaller) {
                @Override
                protected void onSuccess(List<ItemReference> result) {
                    callback.onSuccess(result);
                }

                @Override
                protected void onFailure(Throwable exception) {
                    callback.onFailure(exception);
                }
            });
        } catch (WebSocketException e) {
            callback.onFailure(e);
        }
    }

    private boolean isItemHidden(String path) {
        return path.contains("/.");
    }
}
