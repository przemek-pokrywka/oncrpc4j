/*
 * Copyright (c) 2009 - 2012 Deutsches Elektronen-Synchroton,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program (see the file COPYING.LIB for more
 * details); if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.dcache.xdr;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

public class RpcProtocolFilter extends BaseFilter {

    private final static Logger _log = LoggerFactory.getLogger(RpcProtocolFilter.class);
    private final ReplyQueue<Integer, RpcReply> _replyQueue;

    public RpcProtocolFilter() {
        this(null);
    }

    public RpcProtocolFilter(ReplyQueue<Integer, RpcReply> replyQueue) {
        _replyQueue = replyQueue;
    }

    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {

        Xdr xdr = ctx.getMessage();
        if (xdr == null) {
            _log.error("Parser returns bad XDR");
            return ctx.getStopAction();
        }

        xdr.beginDecoding();

        RpcMessage message = new RpcMessage(xdr);
        XdrTransport transport = new GrizzlyXdrTransport(ctx, _replyQueue);

        switch (message.type()) {
            case RpcMessageType.CALL:
                RpcCall call = new RpcCall(message.xid(), xdr, transport);
                try {
                    call.accept();
                    ctx.setMessage(call);

                } catch (RpcException e) {
                    call.reject(e.getStatus(), e.getRpcReply());
                    _log.info("RPC request rejected: {}", e.getMessage());
                    return ctx.getStopAction();
                } catch (OncRpcException e) {
                    _log.info("failed to process RPC request: {}", e.getMessage());
                    return ctx.getStopAction();
                }
                return ctx.getInvokeAction();
            case RpcMessageType.REPLY:
                try {
                    RpcReply reply = new RpcReply(message.xid(), xdr, transport);
                    if (_replyQueue != null) {
                        _replyQueue.put(message.xid(), reply);
                    }
                } catch (OncRpcException e) {
                    _log.warn("failed to decode reply:", e);
                }
                return ctx.getStopAction();
            default:
                // bad XDR
                return ctx.getStopAction();
        }
    }
}
