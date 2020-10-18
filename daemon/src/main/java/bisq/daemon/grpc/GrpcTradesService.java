/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.daemon.grpc;

import bisq.core.api.CoreApi;
import bisq.core.api.model.TradeInfo;
import bisq.core.trade.Trade;

import bisq.proto.grpc.GetTradeReply;
import bisq.proto.grpc.GetTradeRequest;
import bisq.proto.grpc.TakeOfferReply;
import bisq.proto.grpc.TakeOfferRequest;
import bisq.proto.grpc.TradesGrpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import static bisq.core.api.model.TradeInfo.toTradeInfo;

@Slf4j
class GrpcTradesService extends TradesGrpc.TradesImplBase {

    private final CoreApi coreApi;

    @Inject
    public GrpcTradesService(CoreApi coreApi) {
        this.coreApi = coreApi;
    }

    @Override
    public void getTrade(GetTradeRequest req,
                         StreamObserver<GetTradeReply> responseObserver) {
        try {
            Trade trade = coreApi.getTrade(req.getTradeId());
            var reply = GetTradeReply.newBuilder()
                    .setTrade(toTradeInfo(trade).toProtoMessage())
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (IllegalStateException | IllegalArgumentException cause) {
            var ex = new StatusRuntimeException(Status.UNKNOWN.withDescription(cause.getMessage()));
            responseObserver.onError(ex);
            throw ex;
        }
    }

    @Override
    public void takeOffer(TakeOfferRequest req,
                          StreamObserver<TakeOfferReply> responseObserver) {
        try {
            coreApi.takeOffer(req.getOfferId(),
                    req.getPaymentAccountId(),
                    trade -> {
                        TradeInfo tradeInfo = toTradeInfo(trade);
                        var reply = TakeOfferReply.newBuilder()
                                .setTrade(tradeInfo.toProtoMessage())
                                .build();
                        responseObserver.onNext(reply);
                        responseObserver.onCompleted();
                    });
        } catch (IllegalStateException | IllegalArgumentException cause) {
            var ex = new StatusRuntimeException(Status.UNKNOWN.withDescription(cause.getMessage()));
            responseObserver.onError(ex);
            throw ex;
        }
    }
}
