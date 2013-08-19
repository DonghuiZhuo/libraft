/*
 * Copyright (c) 2013, Allen A. George <allen dot george at gmail dot com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of libraft nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.libraft.kayvee.resources;

import com.google.common.util.concurrent.ListenableFuture;
import io.libraft.NotLeaderException;
import io.libraft.kayvee.api.KeyValue;
import io.libraft.kayvee.configuration.ClusterMember;
import io.libraft.kayvee.store.CannotSubmitCommandException;
import io.libraft.kayvee.store.DistributedStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * JAX-RS-annotated class that represents a collection of
 * key-value pairs and operations on this collection.It acts
 * as a <em>root resource</em> for {@link KeyResource}.
 * <p/>
 * Its path is {@code http://base.url/keys}
 */
@Path("/keys")
public final class KeysResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeysResource.class);
    private static final String KEY_PATTERN = "[a-zA-Z0-9][a-zA-Z_0-9]*";

    private final Set<ClusterMember> members;
    private final DistributedStore distributedStore;

    public KeysResource(Set<ClusterMember> members, DistributedStore distributedStore) {
        this.members = members;
        this.distributedStore = distributedStore;
    }

    @Path("/{key:" + KEY_PATTERN + "}")
    public KeyResource forKey(@PathParam("key") String key) {
        LOGGER.info("locate sub-resource:{}", key);
        return new KeyResource(key, members, distributedStore);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<KeyValue> getAll() throws Exception {
        LOGGER.info("get all keys");

        try {
            ListenableFuture<Collection<KeyValue>> getAllFuture = distributedStore.getAll();
            return getAllFuture.get(ResourceConstants.COMMAND_TIMEOUT, ResourceConstants.COMMAND_TIMEOUT_TIME_UNIT);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof NotLeaderException) {
                throw new CannotSubmitCommandException((NotLeaderException) cause, members);
            } else {
                throw e;
            }
        }
    }
}