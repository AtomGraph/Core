/*
 * Copyright 2015 Martynas Jusevičius <martynas@graphity.org>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.graphity.core.exception;

import javax.ws.rs.core.Response.StatusType;

/**
 * A runtime exception thrown by a client that signals a failure to process the HTTP request or HTTP response.
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class ClientException extends RuntimeException
{
    private final StatusType statusType;
    
    public ClientException(StatusType statusType)
    {
	if (statusType == null) throw new IllegalArgumentException("StatusType must be not null");        
        this.statusType = statusType;
    }
    
    public StatusType getStatusType()
    {
        return statusType;
    }
    
    @Override
    public String toString()
    {
        return getStatusType().toString();
    }
    
}