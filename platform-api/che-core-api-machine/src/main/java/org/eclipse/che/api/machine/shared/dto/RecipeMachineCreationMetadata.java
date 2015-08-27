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
package org.eclipse.che.api.machine.shared.dto;

import org.eclipse.che.api.machine.shared.dto.recipe.MachineRecipe;
import org.eclipse.che.dto.shared.DTO;

/**
 * Describes information needed for machine creation from recipe
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface RecipeMachineCreationMetadata extends MachineCreationMetadata {
    /**
     * Type of machine implementation
     */
    String getType();

    void setType(String type);

    RecipeMachineCreationMetadata withType(String type);

    /**
     * Recipe of machine instance
     */
    MachineRecipe getRecipe();

    void setRecipe(MachineRecipe recipeDescriptor);

    RecipeMachineCreationMetadata withRecipe(MachineRecipe recipeDescriptor);

    /**
     * Id of a workspace machine should be bound to
     */
    String getWorkspaceId();

    void setWorkspaceId(String workspaceId);

    RecipeMachineCreationMetadata withWorkspaceId(String workspaceId);

    boolean isDev();

    void setDev(boolean bindWorkspace);

    RecipeMachineCreationMetadata withDev(boolean bindWorkspace);

    @Override
    RecipeMachineCreationMetadata withOutputChannel(String outputChannel);

    @Override
    RecipeMachineCreationMetadata withDisplayName(String displayName);

    @Override
    RecipeMachineCreationMetadata withMemorySize(int mem);
}
