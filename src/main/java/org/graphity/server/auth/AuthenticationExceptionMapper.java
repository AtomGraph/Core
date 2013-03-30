/**
 *  Copyright 2012 Martynas Jusevičius <martynas@graphity.org>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.graphity.server.auth;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Provider
public class AuthenticationExceptionMapper implements ExceptionMapper<AuthenticationException>
{

    @Override
    public Response toResponse(AuthenticationException ae)
    {
        if (ae.getRealm() != null)
            return Response.
                    status(Status.UNAUTHORIZED).
                    header("WWW-Authenticate", "Basic realm=\"" + ae.getRealm() + "\"").
                    type(MediaType.TEXT_PLAIN).
                    entity(ae.getMessage()).
                    build();
	else return Response.
                    status(Status.UNAUTHORIZED).
                    type(MediaType.TEXT_PLAIN).
                    entity(ae.getMessage()).
                    build();
    }
    
}
