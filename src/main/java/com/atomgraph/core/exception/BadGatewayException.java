/*
 * Copyright 2021 Martynas Jusevičius <martynas@atomgraph.com>.
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
package com.atomgraph.core.exception;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class BadGatewayException extends InternalServerErrorException
{

    public BadGatewayException()
    {
    }

    public BadGatewayException(String message)
    {
        super(message);
    }

    public BadGatewayException(Response response)
    {
        super(response);
    }

    public BadGatewayException(String message, Response response)
    {
        super(message, response);
    }

    public BadGatewayException(Throwable cause)
    {
        super(cause);
    }

    public BadGatewayException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public BadGatewayException(Response response, Throwable cause)
    {
        super(response, cause);
    }

    public BadGatewayException(String message, Response response, Throwable cause)
    {
        super(message, response, cause);
    }

}
