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
package org.eclipse.che.git.impl.nativegit.ssh;

import com.google.common.io.Files;

import org.apache.commons.io.FileUtils;
import org.eclipse.che.api.git.GitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static java.nio.file.attribute.AclEntryPermission.APPEND_DATA;
import static java.nio.file.attribute.AclEntryPermission.DELETE;
import static java.nio.file.attribute.AclEntryPermission.READ_ACL;
import static java.nio.file.attribute.AclEntryPermission.READ_ATTRIBUTES;
import static java.nio.file.attribute.AclEntryPermission.READ_DATA;
import static java.nio.file.attribute.AclEntryPermission.READ_NAMED_ATTRS;
import static java.nio.file.attribute.AclEntryPermission.SYNCHRONIZE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.eclipse.che.api.core.util.SystemInfo.isUnix;
import static org.eclipse.che.api.core.util.SystemInfo.isWindows;

/**
 * Implementation of script that provide ssh connection
 *
 * @author Anton Korneta
 */
public class GitSshScript {

    private static final Logger LOG                 = LoggerFactory.getLogger(GitSshScriptProvider.class);
    private static final String SSH_SCRIPT_TEMPLATE = "exec ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -i $ssh_key $@";//todo check maybe call but ...
    private static final String SSH_SCRIPT          = "ssh_script";
    private static final String DEFAULT_KEY_NAME    = "identity";
    private static final String OWNER_NAME_PROPERTY = "user.name";

    private byte[] sshKey;
    private String host;
    private File   rootFolder;
    private File   sshScriptFile;

    public GitSshScript(String host, byte[] sshKey) throws GitException {
        File file = Files.createTempDir();
        this.rootFolder = new File(file.getAbsolutePath() + File.separator + "test     test");//todo it's for test space in the path
        try {
            java.nio.file.Files.createDirectory(rootFolder.toPath());
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
        this.host = host;
        this.sshKey = sshKey;
        this.sshScriptFile = storeSshScript(writePrivateKeyFile().getPath());
    }

    /**
     * Writes private SSH key into file.
     *
     * @return file that contains SSH key
     * @throws GitException
     *         if other error occurs
     */
    private File writePrivateKeyFile() throws GitException {
        final File keyDirectory = new File(rootFolder, host);
        if (!keyDirectory.exists()) {
            keyDirectory.mkdirs();
        }

        final File keyFile = new File(rootFolder, host + File.separator + DEFAULT_KEY_NAME);
        try (FileOutputStream fos = new FileOutputStream(keyFile)) {
            fos.write(sshKey);
        } catch (IOException e) {
            LOG.error("Cant store key", e);
            throw new GitException("Cant store ssh key. ");
        }

        try {
            if (isUnix()) {
                //set perm to -rw-------
                Set<PosixFilePermission> permissions = EnumSet.of(OWNER_READ, OWNER_WRITE);
                java.nio.file.Files.setPosixFilePermissions(keyFile.toPath(), permissions);
            } else if (isWindows()) {
                AclFileAttributeView attributes = java.nio.file.Files.getFileAttributeView(keyFile.toPath(), AclFileAttributeView.class);

                AclEntry.Builder builder = AclEntry.newBuilder();
                builder.setType(AclEntryType.ALLOW);

                String ownerName = System.getProperty(OWNER_NAME_PROPERTY);
                UserPrincipal userPrincipal = FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByName(ownerName);

                builder.setPrincipal(userPrincipal);
                builder.setPermissions(READ_DATA,
                                       //WRITE_DATA,
                                       APPEND_DATA,
                                       READ_NAMED_ATTRS,
                                       //WRITE_NAMED_ATTRS,
                                       READ_ATTRIBUTES,
                                       //WRITE_ATTRIBUTES,
                                       DELETE,
                                       READ_ACL,
                                       //WRITE_ACL,
                                       SYNCHRONIZE);

                AclEntry entry = builder.build();
                List<AclEntry> aclEntryList = new ArrayList<>();
                aclEntryList.add(entry);
                attributes.setAcl(aclEntryList);
            }
        } catch (IOException e) {
            throw new GitException("Failed to set file permissions");
        }

        this.sshScriptFile = keyFile;
        return keyFile;
    }

    /**
     * Stores ssh script that will be executed with all commands that need ssh.
     *
     * @param keyPath
     *         path to ssh key
     * @return file that contains script for ssh commands
     * @throws GitException
     *         when any error with ssh script storing occurs
     */
    private File storeSshScript(String keyPath) throws GitException {
        File sshScriptFile = new File(rootFolder, SSH_SCRIPT);
        try (FileOutputStream fos = new FileOutputStream(sshScriptFile)) {
            fos.write(SSH_SCRIPT_TEMPLATE.replace("$ssh_key", "\"" + keyPath + "\"").getBytes());
        } catch (IOException e) {
            LOG.error("It is not possible to store " + keyPath + " ssh key");
            throw new GitException("Can't store SSH key");
        }
        if (!sshScriptFile.setExecutable(true)) {
            LOG.error("Can't make " + sshScriptFile + " executable");
            throw new GitException("Can't set permissions to SSH key");
        }
        return sshScriptFile;
    }

    public File getSshScriptFile() {
        return sshScriptFile;
    }

    /**
     * Remove script folder with sshScript and sshKey
     *
     * @throws GitException
     *         when any error with ssh script deleting occurs
     */
    public void delete() throws GitException {
        try {
            FileUtils.deleteDirectory(rootFolder);
        } catch (IOException ioEx) {
            throw new GitException("Can't remove SSH script directory", ioEx);
        }
    }
}
