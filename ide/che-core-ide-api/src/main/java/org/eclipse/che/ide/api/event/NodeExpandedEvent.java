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
package org.eclipse.che.ide.api.event;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Andrienko Alexander
 */
public class NodeExpandedEvent extends GwtEvent<NodeExpandedEventHandler> {

    public static Type<NodeExpandedEventHandler> TYPE = new Type<>();

    @Override
    public Type<NodeExpandedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(NodeExpandedEventHandler handler) {
        handler.onNodeExpanded();
    }
}
